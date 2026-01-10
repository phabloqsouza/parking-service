package com.estapar.parking.api.controller;

import com.estapar.parking.service.GarageInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InitializationController {
    
    private static final Logger logger = LoggerFactory.getLogger(InitializationController.class);
    
    private final GarageInitializationService initializationService;
    
    public InitializationController(GarageInitializationService initializationService) {
        this.initializationService = initializationService;
    }
    
    @PostMapping("/initialize")
    public ResponseEntity<String> initialize() {
        logger.info("Received initialization request");
        try {
            initializationService.initializeFromSimulator();
            return ResponseEntity.ok("Garage initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize garage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to initialize garage: " + e.getMessage());
        }
    }
}
