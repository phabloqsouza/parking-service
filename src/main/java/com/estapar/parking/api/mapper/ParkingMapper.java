package com.estapar.parking.api.mapper;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ParkingMapper {
    
    @Mapping(target = "currency", constant = "BRL")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    RevenueResponseDto toRevenueResponseDto(BigDecimal amount);
    
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "spotId", ignore = true)
    @Mapping(target = "exitTime", ignore = true)
    @Mapping(target = "finalPrice", ignore = true)
    @Mapping(target = "version", ignore = true)
    ParkingSession toParkingSession(String vehicleLicensePlate, Instant entryTime, UUID garageId, UUID sectorId, BigDecimal basePrice);
}
