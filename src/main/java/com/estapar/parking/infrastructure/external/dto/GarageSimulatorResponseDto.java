package com.estapar.parking.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record GarageSimulatorResponseDto(
    @JsonProperty("garage")
    List<SectorConfigDto> garage,
    
    @JsonProperty("spots")
    List<SpotConfigDto> spots
) {
    public record SectorConfigDto(
        String sector,
        @JsonProperty("basePrice")
        BigDecimal basePrice,
        @JsonProperty("max_capacity")
        Integer maxCapacity
    ) {}
    
    public record SpotConfigDto(
        Integer id,
        String sector,
        BigDecimal lat,
        BigDecimal lng
    ) {}
}
