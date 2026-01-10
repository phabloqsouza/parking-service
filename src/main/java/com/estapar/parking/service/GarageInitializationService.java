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
            
            // Initialize sectors
            initializeSectors(garage.getId(), config.garage());
            
            // Initialize spots
            initializeSpots(garage.getId(), config.spots());
            
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
    
    private void initializeSectors(UUID garageId, List<GarageSimulatorResponseDto.SectorConfigDto> sectorConfigs) {
        logger.info("Initializing {} sectors for garage {}", sectorConfigs.size(), garageId);
        
        for (GarageSimulatorResponseDto.SectorConfigDto config : sectorConfigs) {
            Optional<Sector> existingSector = sectorRepository.findByGarageIdAndSectorCode(
                garageId, config.sector());
            
            if (existingSector.isPresent()) {
                // Update existing sector
                Sector sector = existingSector.get();
                sector.setBasePrice(config.basePrice());
                sector.setMaxCapacity(config.maxCapacity() != null ? config.maxCapacity() : 100);
                sectorRepository.save(sector);
                logger.debug("Updated sector: {} for garage {}", config.sector(), garageId);
            } else {
                // Create new sector
                Sector sector = new Sector();
                sector.setId(UUID.randomUUID());
                sector.setGarageId(garageId);
                sector.setSectorCode(config.sector());
                sector.setBasePrice(config.basePrice());
                sector.setMaxCapacity(config.maxCapacity() != null ? config.maxCapacity() : 100);
                sector.setOccupiedCount(0);
                sector.setVersion(0);
                // created_at is handled by database DEFAULT CURRENT_TIMESTAMP
                sectorRepository.save(sector);
                logger.debug("Created sector: {} for garage {}", config.sector(), garageId);
            }
        }
    }
    
    private void initializeSpots(UUID garageId, List<GarageSimulatorResponseDto.SpotConfigDto> spotConfigs) {
        if (spotConfigs == null || spotConfigs.isEmpty()) {
            logger.warn("No spots provided in simulator configuration");
            return;
        }
        
        logger.info("Initializing {} spots for garage {}", spotConfigs.size(), garageId);
        
        // Get all sectors for this garage to map sector codes to IDs
        List<Sector> sectors = sectorRepository.findByGarageId(garageId);
        
        for (GarageSimulatorResponseDto.SpotConfigDto config : spotConfigs) {
            // Find sector by code
            Sector sector = sectors.stream()
                .filter(s -> s.getSectorCode().equals(config.sector()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Sector not found for spot: " + config.sector()));
            
            // Check if spot already exists (by coordinates within sector)
            BigDecimal tolerance = BigDecimal.valueOf(0.000001);
            List<ParkingSpot> existingSpots = spotRepository
                .findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                    sector.getId(),
                    config.lat().subtract(tolerance),
                    config.lat().add(tolerance),
                    config.lng().subtract(tolerance),
                    config.lng().add(tolerance)
                );
            
            if (existingSpots.isEmpty()) {
                // Create new spot
                ParkingSpot spot = new ParkingSpot();
                spot.setId(UUID.randomUUID());
                spot.setSectorId(sector.getId());
                spot.setLatitude(config.lat());
                spot.setLongitude(config.lng());
                spot.setIsOccupied(false);
                spot.setVersion(0);
                // created_at is handled by database DEFAULT CURRENT_TIMESTAMP
                spotRepository.save(spot);
                logger.debug("Created spot: lat={}, lng={} in sector {}", 
                           config.lat(), config.lng(), config.sector());
            } else {
                // Spot already exists, update coordinates if needed
                ParkingSpot spot = existingSpots.get(0);
                spot.setLatitude(config.lat());
                spot.setLongitude(config.lng());
                spotRepository.save(spot);
                logger.debug("Updated spot: lat={}, lng={} in sector {}", 
                           config.lat(), config.lng(), config.sector());
            }
        }
    }
}
