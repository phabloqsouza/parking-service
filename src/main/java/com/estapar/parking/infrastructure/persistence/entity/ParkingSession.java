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
           @Index(name = "idx_spot_exit", columnList = "spot_id,exit_time"),
           @Index(name = "idx_spot_vehicle_exit", columnList = "spot_id,vehicle_license_plate,exit_time"),
           @Index(name = "idx_spot_entry_time", columnList = "spot_id,entry_time"),
           @Index(name = "idx_garage_vehicle_exit", columnList = "garage_id,vehicle_license_plate,exit_time")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private ParkingSpot spot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;
    
    @Column(nullable = false, length = 20)
    private String vehicleLicensePlate;
    
    @Column(nullable = false)
    private Instant entryTime;
    
    @Column
    private Instant exitTime;
    
    @Column(nullable = true, precision = 19, scale = 2)
    private BigDecimal basePrice;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal finalPrice;
    
    @Column
    private Long availableCapacityAtEntry;
    
    @Version
    @Column(nullable = false)
    private Integer version;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public boolean isActive() {
        return exitTime == null;
    }
    
    public boolean isParked() {
        return spot != null;
    }
}
