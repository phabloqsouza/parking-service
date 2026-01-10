package com.estapar.parking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public class ParkingFeeCalculator {
    
    private static final int FREE_MINUTES = 30;
    private static final int MINUTES_PER_HOUR = 60;
    
    public BigDecimal calculateFee(Instant entryTime, Instant exitTime, BigDecimal basePrice) {
        if (entryTime == null || exitTime == null || basePrice == null) {
            throw new IllegalArgumentException("Entry time, exit time, and base price must not be null");
        }
        
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time must be after entry time");
        }
        
        Duration duration = Duration.between(entryTime, exitTime);
        long totalMinutes = duration.toMinutes();
        
        // First 30 minutes are free
        long chargeableMinutes = Math.max(0, totalMinutes - FREE_MINUTES);
        
        if (chargeableMinutes == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        
        // Calculate hours (round up)
        BigDecimal chargeableHours = BigDecimal.valueOf(chargeableMinutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 2, RoundingMode.CEILING);
        
        // Calculate final price: hours * base price
        BigDecimal finalPrice = chargeableHours.multiply(basePrice);
        
        return finalPrice.setScale(2, RoundingMode.HALF_UP);
    }
}
