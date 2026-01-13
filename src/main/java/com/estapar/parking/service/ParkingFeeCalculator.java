package com.estapar.parking.service;

import com.estapar.parking.util.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ParkingFeeCalculator {
    
    private static final int MINUTES_PER_HOUR = 60;
    
    @Value("${parking.fee.free-minutes:30}")
    private int freeMinutes;
    
    private final BigDecimalUtils bigDecimalUtils;
    
    public BigDecimal calculateFee(Instant entryTime, Instant exitTime, BigDecimal basePrice) {
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time must be after entry time");
        }

        Duration duration = Duration.between(entryTime, exitTime);
        long totalMinutes = duration.toMinutes();

        // First N minutes are free (configurable)
        // If duration is under free time, return free regardless of basePrice
        // This handles the case where user entered but didn't park (basePrice is null)
        if (totalMinutes <= freeMinutes) {
            return bigDecimalUtils.zeroWithCurrencyScale();
        }

        // If basePrice is null, user entered but didn't park - should not charge
        if (basePrice == null) {
            return bigDecimalUtils.zeroWithCurrencyScale();
        }

        // Round up total time to nearest hour (do NOT subtract free minutes)
        BigDecimal chargeableHours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 0, RoundingMode.CEILING);
        
        // Calculate final price: hours * base price
        return bigDecimalUtils.multiplyAndSetCurrencyScale(chargeableHours, basePrice);
    }
}
