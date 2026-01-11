package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.event.EventHandler;
import com.estapar.parking.service.event.EventHandlerFactory;
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
    
    private final EventHandlerFactory eventHandlerFactory;
    
    public WebhookController(EventHandlerFactory eventHandlerFactory) {
        this.eventHandlerFactory = eventHandlerFactory;
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
                   correlationId, eventDto.getEventType(), eventDto.getLicensePlate(), garageId);

        try {
            EventHandler handler = eventHandlerFactory.getHandler(eventDto);
            handler.handle(garageId, eventDto);
            
            logger.info("Event processed successfully: correlationId={}, eventType={}, licensePlate={}", 
                       correlationId, eventDto.getEventType(), eventDto.getLicensePlate());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing webhook event: correlationId={}, eventType={}, error={}", 
                        correlationId, eventDto.getEventType(), e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
}
