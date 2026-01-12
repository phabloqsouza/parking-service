package com.estapar.parking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sector", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"garage_id", "sector_code"}),
       indexes = @Index(name = "idx_garage_id", columnList = "garage_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sector {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;
    
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ParkingSpot> spots;
    
    @Column(nullable = false, length = 10, unique = true)
    private String sectorCode;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;
    
    @Column(nullable = false)
    private Integer maxCapacity;
    
    @Column(nullable = false)
    private Integer occupiedCount;
    
    @Version
    @Column(nullable = false)
    private Integer version;
    
    public boolean isFull() {
        return occupiedCount >= maxCapacity;
    }

}
