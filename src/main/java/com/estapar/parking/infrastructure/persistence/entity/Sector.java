package com.estapar.parking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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
    
    @Column(nullable = false)
    private UUID garageId;
    
    @Column(nullable = false, length = 10)
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
        return occupiedCount != null && maxCapacity != null && occupiedCount >= maxCapacity;
    }
    
    public void incrementOccupiedCount() {
        if (this.occupiedCount == null) {
            this.occupiedCount = 0;
        }
        this.occupiedCount++;
    }
    
    public void decrementOccupiedCount() {
        if (this.occupiedCount != null && this.occupiedCount > 0) {
            this.occupiedCount--;
        }
    }
}
