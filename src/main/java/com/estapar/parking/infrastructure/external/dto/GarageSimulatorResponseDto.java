package com.estapar.parking.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record GarageSimulatorResponseDto(
    @NotNull(message = "Garage configuration is required")
    @NotEmpty(message = "Garage must have at least one sector")
    @Valid
    List<SectorConfigDto> garage,
    
    @NotNull(message = "Spots configuration is required")
    @NotEmpty(message = "Garage must have at least one spot")
    @Size(min = 1, message = "Garage must have at least one spot")
    @Valid
    @JsonProperty("spots")
    List<SpotConfigDto> spots
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SectorConfigDto(
        @NotNull(message = "Sector code is required")
        @NotEmpty(message = "Sector code cannot be empty")
        String sector,
        
        @NotNull(message = "Base price is required")
        @Positive(message = "Base price must be positive")
        @JsonProperty("base_price")
        BigDecimal basePrice,
        
        @NotNull(message = "Max capacity is required")
        @Positive(message = "Max capacity must be positive")
        @JsonProperty("max_capacity")
        Integer maxCapacity
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotConfigDto(
        @NotNull(message = "Spot ID is required")
        @Positive(message = "Spot ID must be positive")
        Integer id,
        
        @NotNull(message = "Sector is required")
        @NotEmpty(message = "Sector cannot be empty")
        String sector,
        
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        BigDecimal lat,
        
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        BigDecimal lng
    ) {}
}
