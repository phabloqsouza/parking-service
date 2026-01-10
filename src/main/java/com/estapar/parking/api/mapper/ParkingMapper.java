package com.estapar.parking.api.mapper;

import com.estapar.parking.api.dto.RevenueResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ParkingMapper {
    
    @Mapping(target = "currency", constant = "BRL")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    RevenueResponseDto toRevenueResponseDto(BigDecimal amount);
}
