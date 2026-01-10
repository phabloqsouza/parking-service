package com.estapar.parking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parking_session",
       indexes = {
           @Index(name = "idx_garage_vehicle_exit", columnList = "garage_id,vehicle_license_plate,exit_time"),
           @Index(name = "idx_garage_sector_entry", columnList = "garage_id,sector_id,entry_time"),
           @Index(name = "idx_sector_exit", columnList = "sector_id,exit_time"),
           @Index(name = "idx_spot_exit", columnList = "spot_id,exit_time"),
           @Index(name = "idx_garage_entry_time", columnList = "garage_id,entry_time")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID garageId;
    
    @Column(nullable = false, length = 20)
    private String vehicleLicensePlate;
    
    @Column
    private UUID spotId;
    
    @Column(nullable = false)
    private UUID sectorId;
    
    @Column(nullable = false)
    private Instant entryTime;
    
    @Column
    private Instant exitTime;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal finalPrice;
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public boolean isActive() {
        return exitTime == null;
    }
    
    public boolean isParked() {
        return spotId != null;
    }
}
