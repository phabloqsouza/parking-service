package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class PricingService {
    
    private final ParkingFeeCalculator feeCalculator;
    private final PricingStrategyResolver strategyResolver;
    
    public PricingService(ParkingFeeCalculator feeCalculator, PricingStrategyResolver strategyResolver) {
        this.feeCalculator = feeCalculator;
        this.strategyResolver = strategyResolver;
    }
    
    public BigDecimal calculateOccupancy(Integer occupiedCount, Integer maxCapacity) {
        if (occupiedCount == null || maxCapacity == null || maxCapacity == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal occupied = BigDecimal.valueOf(occupiedCount);
        BigDecimal max = BigDecimal.valueOf(maxCapacity);
        BigDecimal percentage = occupied.divide(max, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return percentage.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calculateBasePriceWithDynamicPricing(BigDecimal basePrice, BigDecimal occupancyPercentage) {
        if (basePrice == null || occupancyPercentage == null) {
            throw new IllegalArgumentException("Base price and occupancy percentage must not be null");
        }
        
        PricingStrategy strategy = strategyResolver.findStrategyByOccupancy(occupancyPercentage);
        BigDecimal multiplier = strategy.getMultiplier();
        
        BigDecimal priceWithDynamicPricing = basePrice.multiply(multiplier);
        return priceWithDynamicPricing.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calculateFinalPrice(Instant entryTime, Instant exitTime, BigDecimal basePriceWithDynamicPricing) {
        return feeCalculator.calculateFee(entryTime, exitTime, basePriceWithDynamicPricing);
    }
}
