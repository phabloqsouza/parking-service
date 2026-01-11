package com.estapar.parking.service;

import com.estapar.parking.infrastructure.external.GarageSimulatorFeignClient;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.*;
import com.estapar.parking.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GarageInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GarageInitializationService.class);
    
    private final GarageSimulatorFeignClient simulatorClient;
    private final GarageRepository garageRepository;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    
    public GarageInitializationService(
            GarageSimulatorFeignClient simulatorClient,
            GarageRepository garageRepository,
            SectorRepository sectorRepository,
            ParkingSpotRepository spotRepository) {
        this.simulatorClient = simulatorClient;
        this.garageRepository = garageRepository;
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
    }
    
    @Transactional
    public void initializeFromSimulator() {
        // Retry logic is handled by Feign client configuration (FeignConfig)
        logger.info("Starting garage initialization from simulator...");
        
        try {
            GarageSimulatorResponseDto config = simulatorClient.getGarageConfiguration();
            
            if (config == null || config.garage() == null || config.garage().isEmpty()) {
                throw new IllegalStateException("Simulator returned empty garage configuration");
            }
            
            // Get or create default garage
            Garage garage = getOrCreateDefaultGarage();
            
            // Build complete garage structure with sectors
            List<Sector> sectors = buildSectors(garage.getId(), config.garage());
            
            // Validate garage structure: at least one sector, each sector has at least one spot
            validateGarageStructure(sectors, config.spots());
            
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
                    updateSpotsForSector(existing.getId(), sector.getSectorCode(), config.spots());
                } else {
                    // Save new sector (spots will be saved separately as there's no cascade)
                    sectorRepository.save(sector);
                    logger.debug("Created sector: {} for garage {}", sector.getSectorCode(), garage.getId());
                    
                    // Save spots for new sector
                    saveSpotsForSector(sector.getId(), sector.getSectorCode(), config.spots());
                }
            }
            
            logger.info("Garage initialization completed successfully. Garage ID: {}", garage.getId());
            
        } catch (Exception e) {
            logger.error("Failed to initialize garage from simulator", e);
            throw new RuntimeException("Garage initialization failed", e);
        }
    }
    
    private Garage getOrCreateDefaultGarage() {
        // Check if default garage exists
        Optional<Garage> existingDefault = garageRepository.findByIsDefaultTrue();
        
        if (existingDefault.isPresent()) {
            logger.info("Default garage already exists: {}", existingDefault.get().getId());
            return existingDefault.get();
        }
        
        // Create new garage as default
        Garage garage = new Garage();
        garage.setId(UUID.randomUUID());
        garage.setName("Default Garage");
        garage.setIsDefault(true);
        garage.setCreatedAt(Instant.now());
        
        garage = garageRepository.save(garage);
        logger.info("Created new default garage: {}", garage.getId());
        
        return garage;
    }
    
    private List<Sector> buildSectors(UUID garageId, 
                                      List<GarageSimulatorResponseDto.SectorConfigDto> sectorConfigs) {
        logger.info("Building {} sectors for garage {}", sectorConfigs.size(), garageId);
        
        return sectorConfigs.stream()
            .map(config -> {
                Sector sector = new Sector();
                sector.setId(UUID.randomUUID());
                sector.setGarageId(garageId);
                sector.setSectorCode(config.sector());
                sector.setBasePrice(config.basePrice());
                sector.setMaxCapacity(config.maxCapacity() != null ? config.maxCapacity() : 100);
                sector.setOccupiedCount(0);
                return sector;
            })
            .toList();
    }
    
    private void validateGarageStructure(List<Sector> sectors, List<GarageSimulatorResponseDto.SpotConfigDto> spotConfigs) {
        if (sectors == null || sectors.isEmpty()) {
            throw new IllegalStateException("Garage must have at least one sector");
        }
        
        if (spotConfigs == null || spotConfigs.isEmpty()) {
            throw new IllegalStateException("Garage must have at least one spot");
        }
        
        // Validate each sector has at least one spot
        sectors.stream()
            .filter(sector -> spotConfigs.stream()
                .noneMatch(spot -> spot.sector().equals(sector.getSectorCode())))
            .findFirst()
            .ifPresent(sector -> {
                throw new IllegalStateException(
                    String.format("Sector %s must have at least one spot", sector.getSectorCode()));
            });
    }
    
    private void saveSpotsForSector(UUID sectorId, String sectorCode, List<GarageSimulatorResponseDto.SpotConfigDto> spotConfigs) {
        // Filter spots for this sector
        List<GarageSimulatorResponseDto.SpotConfigDto> sectorSpots = spotConfigs.stream()
            .filter(spot -> spot.sector().equals(sectorCode))
            .toList();
        
        // Validation already done in validateGarageStructure, but double-check for safety
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
                // Create new spot
                ParkingSpot spot = new ParkingSpot();
                spot.setId(UUID.randomUUID());
                spot.setSectorId(sectorId);
                spot.setLatitude(config.lat());
                spot.setLongitude(config.lng());
                spot.setIsOccupied(false);
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
    
    private void updateSpotsForSector(UUID sectorId, String sectorCode, List<GarageSimulatorResponseDto.SpotConfigDto> spotConfigs) {
        // Same logic as saveSpotsForSector but for existing sectors
        saveSpotsForSector(sectorId, sectorCode, spotConfigs);
    }
}
