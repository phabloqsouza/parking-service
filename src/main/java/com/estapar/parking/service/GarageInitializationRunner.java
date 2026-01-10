package com.estapar.parking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "parking.initialization.enabled",
    havingValue = "true",
    matchIfMissing = true  // Enabled by default for development
)
public class GarageInitializationRunner implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(GarageInitializationRunner.class);
    
    private final GarageInitializationService initializationService;
    
    public GarageInitializationRunner(GarageInitializationService initializationService) {
        this.initializationService = initializationService;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("ApplicationRunner: Initializing garage from simulator...");
        try {
            initializationService.initializeFromSimulator();
            logger.info("ApplicationRunner: Garage initialization completed successfully");
        } catch (Exception e) {
            logger.error("ApplicationRunner: Failed to initialize garage", e);
            // Don't fail startup - initialization can be retried via endpoint
        }
    }
}
