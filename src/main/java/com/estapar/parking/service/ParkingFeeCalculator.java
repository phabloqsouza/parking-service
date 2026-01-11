package com.estapar.parking.service;

import com.estapar.parking.config.DecimalConfig;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public class ParkingFeeCalculator {
    
    private static final int FREE_MINUTES = 30;
    private static final int MINUTES_PER_HOUR = 60;
    
    private final DecimalConfig decimalConfig;
    
    public ParkingFeeCalculator(DecimalConfig decimalConfig) {
        this.decimalConfig = decimalConfig;
    }
    
    public BigDecimal calculateFee(@NotNull(message = "Entry time must not be null") Instant entryTime,
                                   @NotNull(message = "Exit time must not be null") Instant exitTime,
                                   @NotNull(message = "Base price must not be null") BigDecimal basePrice) {
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time must be after entry time");
        }
        
        Duration duration = Duration.between(entryTime, exitTime);
        long totalMinutes = duration.toMinutes();
        
        // First 30 minutes are free
        long chargeableMinutes = Math.max(0, totalMinutes - FREE_MINUTES);
        
        if (chargeableMinutes == 0) {
            return BigDecimal.ZERO.setScale(decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
        }
        
        // Calculate hours (round up)
        BigDecimal chargeableHours = BigDecimal.valueOf(chargeableMinutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), decimalConfig.getCurrencyScale(), 
                        java.math.RoundingMode.CEILING);
        
        // Calculate final price: hours * base price
        BigDecimal finalPrice = chargeableHours.multiply(basePrice);
        
        return finalPrice.setScale(decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
    }
}
