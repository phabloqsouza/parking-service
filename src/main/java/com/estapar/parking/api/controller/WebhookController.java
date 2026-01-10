package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.ParkingEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook", description = "API for receiving parking events (ENTRY, PARKED, EXIT)")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    private final ParkingEventService parkingEventService;
    
    public WebhookController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }
    
    @PostMapping
    @Operation(
        summary = "Handle parking event",
        description = "Accepts vehicle events: ENTRY (requires entry_time), PARKED (requires lat, lng), EXIT (requires exit_time)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Conflict (e.g., sector full, spot occupied)", content = @Content)
    })
    public ResponseEntity<Void> handleWebhookEvent(
            @Parameter(description = "Optional garage ID. If not provided, uses default garage")
            @RequestHeader(value = "X-Garage-Id", required = false) UUID garageId,
            @Valid @RequestBody WebhookEventDto eventDto) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("Received webhook event: correlationId={}, type={}, licensePlate={}, garageId={}", 
                   correlationId, eventDto.getEvent(), eventDto.getLicensePlate(), garageId);
        
        // Resolve garageId - if not provided, will use default garage
        UUID resolvedGarageId = garageId; // Will be resolved to default in service if null
        
        try {
            switch (eventDto.getEvent().toUpperCase()) {
                case "ENTRY" -> {
                    if (eventDto.getEntryTime() == null) {
                        logger.warn("ENTRY event missing entry_time: correlationId={}", correlationId);
                        return ResponseEntity.badRequest().build();
                    }
                    parkingEventService.handleEntryEvent(
                            resolvedGarageId,
                            eventDto.getLicensePlate(),
                            eventDto.getEntryTime(),
                            eventDto.getSector() != null ? eventDto.getSector() : "A"); // Default sector "A"
                    logger.info("ENTRY event processed successfully: correlationId={}, licensePlate={}", 
                               correlationId, eventDto.getLicensePlate());
                    return ResponseEntity.ok().build();
                }
                case "PARKED" -> {
                    if (eventDto.getLat() == null || eventDto.getLng() == null) {
                        logger.warn("PARKED event missing coordinates: correlationId={}", correlationId);
                        return ResponseEntity.badRequest().build();
                    }
                    parkingEventService.handleParkedEvent(
                            resolvedGarageId,
                            eventDto.getLicensePlate(),
                            eventDto.getLat(),
                            eventDto.getLng());
                    logger.info("PARKED event processed successfully: correlationId={}, licensePlate={}", 
                               correlationId, eventDto.getLicensePlate());
                    return ResponseEntity.ok().build();
                }
                case "EXIT" -> {
                    if (eventDto.getExitTime() == null) {
                        logger.warn("EXIT event missing exit_time: correlationId={}", correlationId);
                        return ResponseEntity.badRequest().build();
                    }
                    parkingEventService.handleExitEvent(
                            resolvedGarageId,
                            eventDto.getLicensePlate(),
                            eventDto.getExitTime());
                    logger.info("EXIT event processed successfully: correlationId={}, licensePlate={}", 
                               correlationId, eventDto.getLicensePlate());
                    return ResponseEntity.ok().build();
                }
                default -> {
                    logger.warn("Unknown event type: correlationId={}, eventType={}", correlationId, eventDto.getEvent());
                    return ResponseEntity.badRequest().build();
                }
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event: correlationId={}, eventType={}, error={}", 
                        correlationId, eventDto.getEvent(), e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
}
