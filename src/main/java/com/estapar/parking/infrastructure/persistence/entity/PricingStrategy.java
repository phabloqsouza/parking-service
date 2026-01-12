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
@Table(name = "pricing_strategy",
       uniqueConstraints = @UniqueConstraint(columnNames = {"garage_id", "occupancy_min_percentage", "occupancy_max_percentage"}),
       indexes = @Index(name = "idx_active_occupancy", columnList = "garage_id,is_active,occupancy_min_percentage,occupancy_max_percentage"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingStrategy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal occupancyMinPercentage;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal occupancyMaxPercentage;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant updatedAt;

}
