package com.estapar.parking.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class WebhookEventDto {
    
    @NotNull(message = "Event type is required")
    @Pattern(regexp = "ENTRY|PARKED|EXIT", message = "Event type must be ENTRY, PARKED, or EXIT")
    private String event;
    
    @Pattern(regexp = "[A-Z]{3}[0-9][A-Z][0-9]{2}|[A-Z]{3}-?[0-9]{4}", 
             message = "License plate format is invalid")
    private String licensePlate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant entryTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant exitTime;
    
    private BigDecimal lat;
    
    private BigDecimal lng;
    
    private String sector;
}
