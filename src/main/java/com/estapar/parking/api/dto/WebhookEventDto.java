package com.estapar.parking.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventDto {
    
    @NotNull(message = "Event type is required")
    @Pattern(regexp = "ENTRY|PARKED|EXIT", message = "Event type must be ENTRY, PARKED, or EXIT")
    @JsonProperty("event_type")
    private String event;
    
    @NotNull(message = "License plate is required")
    @Pattern(regexp = "[A-Z]{3}[0-9][A-Z][0-9]{2}|[A-Z]{3}-?[0-9]{4}", 
             message = "License plate format is invalid (expected format: ABC1234 or ABC-1234)")
    @JsonProperty("license_plate")
    private String licensePlate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @JsonProperty("entry_time")
    private Instant entryTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @JsonProperty("exit_time")
    private Instant exitTime;
    
    // Latitude is required only for PARKED events (validated in controller)
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90", inclusive = true)
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90", inclusive = true)
    private BigDecimal lat;
    
    // Longitude is required only for PARKED events (validated in controller)
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180", inclusive = true)
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180", inclusive = true)
    private BigDecimal lng;
    
    @Pattern(regexp = "^[A-Z]$", message = "Sector must be a single uppercase letter (A-Z)")
    private String sector;
}
