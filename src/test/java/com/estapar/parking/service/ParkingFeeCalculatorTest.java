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
        Instant exitTime = entryTime.plusSeconds(91 * 60); // 91 minutes → ceil(91/60) = 2 hours

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 91 minutes → ceil(91/60) = 2 hours → 2 * 10.00 = 20.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.valueOf(2)), eq(basePrice));
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
        Instant exitTime = entryTime.plusSeconds(3 * 60 * 60 + 1); // 3 hours 1 second = 180.017 minutes → ceil(180.017/60) = 4 hours

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 180.017 minutes → ceil(180.017/60) = 4 hours → 4 * 10.00 = 40.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.valueOf(4)), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_58Minutes_ShouldCharge1Hour() {
        Instant exitTime = entryTime.plusSeconds(58 * 60); // 58 minutes → ceil(58/60) = 1 hour

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 58 minutes → ceil(58/60) = 1 hour → 1 * 10.00 = 10.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.ONE), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_62Minutes_ShouldCharge2Hours() {
        Instant exitTime = entryTime.plusSeconds(62 * 60); // 62 minutes → ceil(62/60) = 2 hours

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 62 minutes → ceil(62/60) = 2 hours → 2 * 10.00 = 20.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.valueOf(2)), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_75Minutes_ShouldCharge2Hours() {
        Instant exitTime = entryTime.plusSeconds(75 * 60); // 75 minutes → ceil(75/60) = 2 hours

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 75 minutes → ceil(75/60) = 2 hours → 2 * 10.00 = 20.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.valueOf(2)), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_131Minutes_ShouldCharge3Hours() {
        Instant exitTime = entryTime.plusSeconds(131 * 60); // 131 minutes → ceil(131/60) = 3 hours

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 131 minutes → ceil(131/60) = 3 hours → 3 * 10.00 = 30.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.valueOf(3)), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_60Minutes_ShouldCharge1Hour() {
        Instant exitTime = entryTime.plusSeconds(60 * 60); // 60 minutes → ceil(60/60) = 1 hour

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 60 minutes → ceil(60/60) = 1 hour → 1 * 10.00 = 10.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.ONE), eq(basePrice));
        assertThat(result).isNotNull();
    }

    @Test
    void calculateFee_120Minutes_ShouldCharge2Hours() {
        Instant exitTime = entryTime.plusSeconds(120 * 60); // 120 minutes → ceil(120/60) = 2 hours

        BigDecimal result = parkingFeeCalculator.calculateFee(entryTime, exitTime, basePrice);

        // 120 minutes → ceil(120/60) = 2 hours → 2 * 10.00 = 20.00
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(BigDecimal.valueOf(2)), eq(basePrice));
        assertThat(result).isNotNull();
    }
}
