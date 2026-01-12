package com.estapar.parking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "parking_spot",
       indexes = {
           @Index(name = "idx_sector_occupied", columnList = "sector_id,is_occupied"),
           @Index(name = "idx_sector_coordinates", columnList = "sector_id,latitude,longitude"),
           @Index(name = "idx_coordinates", columnList = "latitude,longitude")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;
    
    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(nullable = false)
    private Boolean isOccupied;
    
    @Version
    @Column(nullable = false)
    private Integer version;
}
