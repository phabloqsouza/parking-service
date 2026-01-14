package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.WebhookEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook", description = "API for receiving parking events (ENTRY, PARKED, EXIT)")
@RequiredArgsConstructor
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    private final WebhookEventService webhookEventService;
    
    @PostMapping
    @Operation(
        summary = "Handle parking event",
        description = "Accepts vehicle parking events from the simulator. " +
                      "ENTRY event: requires entry_time (sector is NOT specified - determined when vehicle parks). " +
                      "PARKED event: requires lat and lng coordinates for exact spot matching. " +
                      "EXIT event: requires exit_time. " +
                      "All events require license_plate and event_type. " +
                      "Sector is determined automatically on PARKED event when spot is matched."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found (e.g., garage not found)", content = @Content),
        @ApiResponse(responseCode = "409", description = "Conflict (e.g., garage full, spot occupied, vehicle already has active session)", content = @Content)
    })
    public ResponseEntity<Void> handleWebhookEvent(
            @Parameter(description = "Optional garage ID. If not provided, uses default garage")
            @RequestHeader(value = "X-Garage-Id", required = false) UUID garageId,
            @Valid @RequestBody WebhookEventDto eventDto) {
        
        logger.info("Received webhook event: type={}, licensePlate={}, garageId={}", 
                   eventDto.getEventType(), eventDto.getLicensePlate(), garageId);

        webhookEventService.processEvent(garageId, eventDto);

        logger.info("Event processed successfully: eventType={}, licensePlate={}", 
                   eventDto.getEventType(), eventDto.getLicensePlate());
        return ResponseEntity.ok().build();
    }
}
