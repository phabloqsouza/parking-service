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

        if (totalMinutes <= freeMinutes || basePrice == null) {
            return bigDecimalUtils.zeroWithCurrencyScale();
        }

        BigDecimal chargeableHours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 0, RoundingMode.CEILING);
        
        return bigDecimalUtils.multiplyAndSetCurrencyScale(chargeableHours, basePrice);
    }
}
