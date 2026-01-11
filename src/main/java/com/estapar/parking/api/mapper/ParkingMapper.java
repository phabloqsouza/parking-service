package com.estapar.parking.api.mapper;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
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
    
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Garage toGarage(String name, Boolean isDefault);
    
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "occupiedCount", constant = "0")
    @Mapping(target = "version", ignore = true)
    Sector toSector(UUID garageId, String sectorCode, BigDecimal basePrice, Integer maxCapacity);
    
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "isOccupied", constant = "false")
    @Mapping(target = "version", ignore = true)
    ParkingSpot toParkingSpot(UUID sectorId, BigDecimal latitude, BigDecimal longitude);
    
    default Sector toSector(UUID garageId, GarageSimulatorResponseDto.SectorConfigDto config) {
        return toSector(garageId, config.sector(), config.basePrice(), 
                       config.maxCapacity() != null ? config.maxCapacity() : 100);
    }
    
    default ParkingSpot toParkingSpot(UUID sectorId, GarageSimulatorResponseDto.SpotConfigDto config) {
        return toParkingSpot(sectorId, config.lat(), config.lng());
    }
}
