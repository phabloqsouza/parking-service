package com.estapar.parking.service;

import com.estapar.parking.util.BigDecimalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingFeeCalculatorTest {

    @Mock
    private BigDecimalUtils bigDecimalUtils;

    @InjectMocks
    private ParkingFeeCalculator parkingFeeCalculator;

    private Instant entryTime;
    private BigDecimal basePrice;

    @BeforeEach
    void setUp() {
        entryTime = Instant.parse("2025-01-01T10:00:00.000Z");
        basePrice = new BigDecimal("10.00");
        ReflectionTestUtils.setField(parkingFeeCalculator, "freeMinutes", 30);
        
        when(bigDecimalUtils.zeroWithCurrencyScale()).thenReturn(BigDecimal.ZERO.setScale(2));
        when(bigDecimalUtils.divideWithCurrencyScale(any(BigDecimal.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    BigDecimal dividend = invocation.getArgument(0);
                    BigDecimal divisor = invocation.getArgument(1);
                    return dividend.divide(divisor, 2, java.math.RoundingMode.CEILING);
                });
        when(bigDecimalUtils.multiplyAndSetCurrencyScale(any(BigDecimal.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    BigDecimal value1 = invocation.getArgument(0);
                    BigDecimal value2 = invocation.getArgument(1);
                    return value1.multiply(value2).setScale(2, java.math.RoundingMode.HALF_UP);
                });
    }

    @Test
    void calculateFee_FreePeriod_ShouldReturnZero() {
        Instant exitTime = entryTime.plusSeconds(29 * 60); // 29 minutes

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(bigDecimalUtils).zeroWithCurrencyScale();
    }

    @Test
    void calculateFee_Exactly30Minutes_ShouldReturnZero() {
        Instant exitTime = entryTime.plusSeconds(30 * 60); // exactly 30 minutes

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(bigDecimalUtils).zeroWithCurrencyScale();
    }

    @Test
    void calculateFee_31Minutes_ShouldCharge1Hour() {
        Instant exitTime = entryTime.plusSeconds(31 * 60); // 31 minutes (1 minute after free period)

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        verify(bigDecimalUtils).divideWithCurrencyScale(eq(BigDecimal.ONE), eq(BigDecimal.valueOf(60)));
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(any(BigDecimal.class), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_91Minutes_ShouldCharge2Hours() {
        Instant exitTime = entryTime.plusSeconds(91 * 60); // 91 minutes (61 chargeable minutes = 2 hours rounded up)

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        verify(bigDecimalUtils).divideWithCurrencyScale(eq(BigDecimal.valueOf(61)), eq(BigDecimal.valueOf(60)));
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(any(BigDecimal.class), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_InvalidTimeRange_ShouldThrowException() {
        Instant exitTime = entryTime.minusSeconds(60); // exit before entry

        assertThatThrownBy(() -> parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exit time must be after entry time");
    }

    @Test
    void calculateFee_MultipleHours_ShouldCalculateCorrectly() {
        Instant exitTime = entryTime.plusSeconds(3 * 60 * 60 + 1); // 3 hours 1 second

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        verify(bigDecimalUtils).divideWithCurrencyScale(any(BigDecimal.class), eq(BigDecimal.valueOf(60)));
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(any(BigDecimal.class), eq(basePrice));
        assertThat(result).isNotNull();
    }
}
