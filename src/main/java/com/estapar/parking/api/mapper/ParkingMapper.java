package com.estapar.parking.api.mapper;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ParkingMapper {
    
    @Mapping(target = "currency", constant = "BRL")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    RevenueResponseDto toRevenueResponseDto(BigDecimal amount);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "spot", ignore = true)
    @Mapping(target = "sector", ignore = true)
    @Mapping(target = "exitTime", ignore = true)
    @Mapping(target = "finalPrice", ignore = true)
    @Mapping(target = "version", ignore = true)
    ParkingSession toParkingSession(String vehicleLicensePlate, Instant entryTime, Garage garage);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "isDefault", constant = "true")
    @Mapping(target = "sectors", ignore = true)
    @Mapping(target = "maxCapacity", ignore = true)
    Garage toGarage(GarageSimulatorResponseDto config);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sectorCode", source = "sector")
    @Mapping(target = "basePrice", source = "basePrice")
    @Mapping(target = "maxCapacity", source = "maxCapacity")
    @Mapping(target = "occupiedCount", constant = "0")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "spots", ignore = true)
    @Mapping(target = "garage", ignore = true)
    Sector toSector(GarageSimulatorResponseDto.SectorConfigDto config);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "lng")
    @Mapping(target = "isOccupied", constant = "false")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sector", ignore = true)
    ParkingSpot toParkingSpot(GarageSimulatorResponseDto.SpotConfigDto config);
    
    @AfterMapping
    default void configureGarageRelationships(@MappingTarget Garage garage, GarageSimulatorResponseDto config) {
        List<Sector> sectors = config.garage().stream()
            .map(this::toSector)
            .peek(sector -> sector.setGarage(garage))
            .peek(sector -> {
                List<ParkingSpot> spots = config.spots().stream()
                    .filter(spotConfig -> spotConfig.sector().equals(sector.getSectorCode()))
                    .map(this::toParkingSpot)
                    .peek(spot -> spot.setSector(sector))
                    .toList();
                
                if (spots.isEmpty()) {
                    throw new IllegalStateException("Sector " + sector.getSectorCode() + " must have at least one spot");
                }
                
                sector.setSpots(spots);
            })
            .toList();
        
        garage.setSectors(sectors);
        
        int totalMaxCapacity = sectors.stream()
            .mapToInt(Sector::getMaxCapacity)
            .sum();
        garage.setMaxCapacity(totalMaxCapacity);
    }
}
