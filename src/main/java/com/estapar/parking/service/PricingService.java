package com.estapar.parking.service;

import com.estapar.parking.config.DecimalConfig;
import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class PricingService {
    
    private final ParkingFeeCalculator feeCalculator;
    private final PricingStrategyResolver strategyResolver;
    private final DecimalConfig decimalConfig;
    
    public PricingService(ParkingFeeCalculator feeCalculator, 
                         PricingStrategyResolver strategyResolver,
                         DecimalConfig decimalConfig) {
        this.feeCalculator = feeCalculator;
        this.strategyResolver = strategyResolver;
        this.decimalConfig = decimalConfig;
    }
    
    public BigDecimal calculateOccupancy(Integer occupiedCount, Integer maxCapacity) {
        if (occupiedCount == null || maxCapacity == null || maxCapacity == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal occupied = BigDecimal.valueOf(occupiedCount);
        BigDecimal max = BigDecimal.valueOf(maxCapacity);
        // Use 4 decimal places for intermediate calculation, then scale to percentage scale
        BigDecimal percentage = occupied.divide(max, 4, decimalConfig.getRoundingMode())
                .multiply(BigDecimal.valueOf(100));
        
        return percentage.setScale(decimalConfig.getPercentageScale(), decimalConfig.getRoundingMode());
    }
    
    public BigDecimal applyPricing(@NotNull(message = "Base price must not be null") BigDecimal basePrice,
                                    @NotNull(message = "Occupancy percentage must not be null") BigDecimal occupancyPercentage) {
        PricingStrategy strategy = strategyResolver.findStrategy(occupancyPercentage);
        BigDecimal multiplier = strategy.getMultiplier();
        
        BigDecimal priceWithDynamicPricing = basePrice.multiply(multiplier);
        return priceWithDynamicPricing.setScale(decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
    }
    
    public BigDecimal calculateFee(Instant entryTime, Instant exitTime, BigDecimal basePriceWithDynamicPricing) {
        return feeCalculator.calculateFee(entryTime, exitTime, basePriceWithDynamicPricing);
    }
}
