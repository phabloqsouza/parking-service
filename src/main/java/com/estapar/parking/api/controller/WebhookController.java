package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.ParkingEventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    private final ParkingEventService parkingEventService;
    
    public WebhookController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }
    
    @PostMapping
    public ResponseEntity<Void> handleWebhookEvent(
            @RequestHeader(value = "X-Garage-Id", required = false) UUID garageId,
            @Valid @RequestBody WebhookEventDto eventDto) {
        
        logger.info("Received webhook event: type={}, licensePlate={}, garageId={}", 
                   eventDto.getEvent(), eventDto.getLicensePlate(), garageId);
        
        // Resolve garageId - if not provided, will use default garage
        UUID resolvedGarageId = garageId; // Will be resolved to default in service if null
        
        switch (eventDto.getEvent().toUpperCase()) {
            case "ENTRY" -> {
                if (eventDto.getEntryTime() == null) {
                    return ResponseEntity.badRequest().build();
                }
                parkingEventService.handleEntryEvent(
                        resolvedGarageId,
                        eventDto.getLicensePlate(),
                        eventDto.getEntryTime(),
                        eventDto.getSector() != null ? eventDto.getSector() : "A"); // Default sector "A"
                return ResponseEntity.ok().build();
            }
            case "PARKED" -> {
                if (eventDto.getLat() == null || eventDto.getLng() == null) {
                    return ResponseEntity.badRequest().build();
                }
                parkingEventService.handleParkedEvent(
                        resolvedGarageId,
                        eventDto.getLicensePlate(),
                        eventDto.getLat(),
                        eventDto.getLng());
                return ResponseEntity.ok().build();
            }
            case "EXIT" -> {
                if (eventDto.getExitTime() == null) {
                    return ResponseEntity.badRequest().build();
                }
                parkingEventService.handleExitEvent(
                        resolvedGarageId,
                        eventDto.getLicensePlate(),
                        eventDto.getExitTime());
                return ResponseEntity.ok().build();
            }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }
    }
}
