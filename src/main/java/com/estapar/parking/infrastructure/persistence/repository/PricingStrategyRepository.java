package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PricingStrategyRepository extends JpaRepository<PricingStrategy, UUID> {
    
    @Query("SELECT ps FROM PricingStrategy ps WHERE ps.isActive = true " +
           "AND ps.occupancyMinPercentage <= :occupancyPercentage " +
           "AND ps.occupancyMaxPercentage > :occupancyPercentage")
    Optional<PricingStrategy> findActiveStrategyByOccupancyRange(
            @Param("occupancyPercentage") BigDecimal occupancyPercentage);
    
    @Query("SELECT ps FROM PricingStrategy ps WHERE ps.garage.id = :garageId " +
           "AND ps.isActive = true " +
           "AND ps.occupancyMinPercentage <= :occupancyPercentage " +
           "AND ps.occupancyMaxPercentage > :occupancyPercentage")
    Optional<PricingStrategy> findActiveStrategyByGarageAndOccupancyRange(
            @Param("garageId") UUID garageId,
            @Param("occupancyPercentage") BigDecimal occupancyPercentage);
}
