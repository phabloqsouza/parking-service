package com.estapar.parking.service;

import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.external.GarageSimulatorFeignClient;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.*;
import com.estapar.parking.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GarageInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GarageInitializationService.class);
    
    private final GarageSimulatorFeignClient simulatorClient;
    private final GarageRepository garageRepository;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingMapper parkingMapper;
    
    @Transactional
    public void initializeFromSimulator() {
        // Retry logic is handled by Feign client configuration (FeignConfig)
        logger.info("Starting garage initialization from simulator...");
        
        try {
            // Check if garage is already initialized (has sectors)
            Optional<Garage> existingDefault = garageRepository.findByIsDefaultTrue();
            if (existingDefault.isPresent()) {
                Garage garage = existingDefault.get();
                List<Sector> existingSectors = sectorRepository.findByGarageId(garage.getId());
                
                // If garage already has sectors, consider it initialized - skip initialization
                if (!existingSectors.isEmpty()) {
                    logger.info("Garage already initialized with {} sectors. Skipping initialization. Garage ID: {}", 
                               existingSectors.size(), garage.getId());
                    return;
                }
                
                // Garage exists but has no sectors - proceed with initialization
                logger.info("Default garage exists but has no sectors. Proceeding with initialization. Garage ID: {}", 
                           garage.getId());
            }
            
            GarageSimulatorResponseDto config = simulatorClient.getGarageConfiguration();
            
            if (config == null) {
                throw new IllegalStateException("Simulator returned null garage configuration");
            }
            
            // Get or create default garage
            Garage garage = getDefaultGarage();
            
            // Build complete garage structure with sectors
            List<Sector> sectors = createSectors(garage.getId(), config.garage());
            
            // Validate each sector has at least one spot (structure validation)
            sectors.stream()
                .filter(sector -> config.spots().stream()
                    .noneMatch(spot -> spot.sector().equals(sector.getSectorCode())))
                .findFirst()
                .ifPresent(sector -> {
                    throw new IllegalStateException(
                        String.format("Sector %s must have at least one spot", sector.getSectorCode()));
                });
            
            // Save all sectors and their spots in one transaction
            for (Sector sector : sectors) {
                // Check if sector already exists
                Optional<Sector> existingSector = sectorRepository.findByGarageIdAndSectorCode(
                    garage.getId(), sector.getSectorCode());
                
                if (existingSector.isPresent()) {
                    // Update existing sector
                    Sector existing = existingSector.get();
                    existing.setBasePrice(sector.getBasePrice());
                    existing.setMaxCapacity(sector.getMaxCapacity());
                    sectorRepository.save(existing);
                    logger.debug("Updated sector: {} for garage {}", sector.getSectorCode(), garage.getId());
                    
                    // Update spots for existing sector
                    saveSpots(existing.getId(), sector.getSectorCode(), config.spots());
                } else {
                    // Save new sector (spots will be saved separately as there's no cascade)
                    sectorRepository.save(sector);
                    logger.debug("Created sector: {} for garage {}", sector.getSectorCode(), garage.getId());
                    
                    // Save spots for new sector
                    saveSpots(sector.getId(), sector.getSectorCode(), config.spots());
                }
            }
            
            logger.info("Garage initialization completed successfully. Garage ID: {}", garage.getId());
            
        } catch (Exception e) {
            logger.error("Failed to initialize garage from simulator", e);
            throw new RuntimeException("Garage initialization failed", e);
        }
    }
    
    private Garage getDefaultGarage() {
        // Check if default garage exists
        Optional<Garage> existingDefault = garageRepository.findByIsDefaultTrue();
        
        if (existingDefault.isPresent()) {
            Garage garage = existingDefault.get();
            logger.info("Default garage already exists: {}", garage.getId());
            return garage;
        }
        
        // Check if any garage exists (in case isDefault flag was lost)
        List<Garage> allGarages = garageRepository.findAll();
        if (!allGarages.isEmpty()) {
            // Use the first garage and mark it as default
            Garage garage = allGarages.get(0);
            if (!Boolean.TRUE.equals(garage.getIsDefault())) {
                garage.setIsDefault(true);
                garage = garageRepository.save(garage);
                logger.info("Marked existing garage as default: {}", garage.getId());
            }
            return garage;
        }
        
        // No garage exists - create new garage as default
        Garage garage = parkingMapper.toGarage("Default Garage", true);
        
        garage = garageRepository.save(garage);
        logger.info("Created new default garage: {}", garage.getId());
        
        return garage;
    }
    
    private List<Sector> createSectors(UUID garageId, 
                                      List<GarageSimulatorResponseDto.SectorConfigDto> sectorConfigs) {
        logger.info("Creating {} sectors for garage {}", sectorConfigs.size(), garageId);
        
        return sectorConfigs.stream()
            .map(config -> parkingMapper.toSector(garageId, config))
            .toList();
    }
    
    private void saveSpots(UUID sectorId, String sectorCode, List<GarageSimulatorResponseDto.SpotConfigDto> spotConfigs) {
        // Filter spots for this sector
        List<GarageSimulatorResponseDto.SpotConfigDto> sectorSpots = spotConfigs.stream()
            .filter(spot -> spot.sector().equals(sectorCode))
            .toList();
        
        if (sectorSpots.isEmpty()) {
            throw new IllegalStateException("Sector " + sectorCode + " must have at least one spot");
        }
        
        logger.info("Saving {} spots for sector {}", sectorSpots.size(), sectorCode);
        
        BigDecimal tolerance = BigDecimal.valueOf(0.000001);
        
        sectorSpots.forEach(config -> {
            // Check if spot already exists (by coordinates within sector)
            List<ParkingSpot> existingSpots = spotRepository
                .findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                    sectorId,
                    config.lat().subtract(tolerance),
                    config.lat().add(tolerance),
                    config.lng().subtract(tolerance),
                    config.lng().add(tolerance)
                );
            
            if (existingSpots.isEmpty()) {
                // Create new spot using mapper
                ParkingSpot spot = parkingMapper.toParkingSpot(sectorId, config);
                spotRepository.save(spot);
                logger.debug("Created spot: lat={}, lng={} in sector {}", 
                           config.lat(), config.lng(), sectorCode);
            } else {
                // Spot already exists, update coordinates if needed
                ParkingSpot spot = existingSpots.get(0);
                spot.setLatitude(config.lat());
                spot.setLongitude(config.lng());
                spotRepository.save(spot);
                logger.debug("Updated spot: lat={}, lng={} in sector {}", 
                           config.lat(), config.lng(), sectorCode);
            }
        });
    }
    
}
