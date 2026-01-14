package com.estapar.parking.service;

import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.external.GarageSimulatorFeignClient;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GarageInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GarageInitializationService.class);
    
    private final GarageSimulatorFeignClient simulatorClient;
    private final GarageRepository garageRepository;
    private final ParkingMapper parkingMapper;
    
    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 60)
    public void initializeFromSimulator() {
        logger.info("Starting garage initialization from simulator...");
        
        if (garageRepository.existsByIsDefaultTrue()) {
            logger.warn("Default garage already exists. Skipping initialization.");
            return;
        }
        
        GarageSimulatorResponseDto config = simulatorClient.getGarageConfiguration();
        
        if (config == null) {
            throw new IllegalStateException("Simulator returned null garage configuration");
        }
        
        Garage garage = parkingMapper.toGarage(config);
        
        garage = garageRepository.save(garage);
        logger.info("Garage initialization completed successfully. Garage ID: {}, Sectors: {}",
                   garage.getId(), 
                   garage.getSectors() != null ? garage.getSectors().size() : 0);
    }
    
}
