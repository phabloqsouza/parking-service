package com.estapar.parking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParkingFeeCalculator Unit Tests")
class ParkingFeeCalculatorTest {

    private ParkingFeeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ParkingFeeCalculator();
    }

    @Test
    @DisplayName("Should return zero for parking less than 30 minutes")
    void shouldReturnZeroForParkingLessThan30Minutes() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(15));
        BigDecimal basePrice = new BigDecimal("10.00");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        assertEquals(BigDecimal.ZERO.setScale(2), fee);
    }

    @Test
    @DisplayName("Should return zero for exactly 30 minutes")
    void shouldReturnZeroForExactly30Minutes() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(30));
        BigDecimal basePrice = new BigDecimal("10.00");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        assertEquals(BigDecimal.ZERO.setScale(2), fee);
    }

    @Test
    @DisplayName("Should charge for 1 hour when parking 31 minutes (rounds up)")
    void shouldChargeOneHourFor31Minutes() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(31));
        BigDecimal basePrice = new BigDecimal("10.00");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        assertEquals(new BigDecimal("10.00"), fee);
    }

    @Test
    @DisplayName("Should charge for 1 hour when parking 60 minutes")
    void shouldChargeOneHourFor60Minutes() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(60));
        BigDecimal basePrice = new BigDecimal("10.00");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        assertEquals(new BigDecimal("10.00"), fee);
    }

    @Test
    @DisplayName("Should charge for 2 hours when parking 91 minutes (rounds up)")
    void shouldChargeTwoHoursFor91Minutes() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(91));
        BigDecimal basePrice = new BigDecimal("10.00");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        assertEquals(new BigDecimal("20.00"), fee);
    }

    @Test
    @DisplayName("Should charge correctly for multiple hours")
    void shouldChargeCorrectlyForMultipleHours() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofHours(3).plusMinutes(30));
        BigDecimal basePrice = new BigDecimal("15.50");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        // 3 hours 30 minutes = 180 + 30 = 210 minutes
        // Chargeable: 210 - 30 = 180 minutes = 3 hours (rounded up)
        // 3 * 15.50 = 46.50
        assertEquals(new BigDecimal("46.50"), fee);
    }

    @Test
    @DisplayName("Should handle decimal base price correctly")
    void shouldHandleDecimalBasePrice() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(90));
        BigDecimal basePrice = new BigDecimal("12.50");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        // 90 minutes - 30 free = 60 chargeable = 1 hour (rounded up)
        // 1 * 12.50 = 12.50
        assertEquals(new BigDecimal("12.50"), fee);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when entryTime is null")
    void shouldThrowExceptionWhenEntryTimeIsNull() {
        Instant exitTime = Instant.now();
        BigDecimal basePrice = new BigDecimal("10.00");

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateFee(null, exitTime, basePrice);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when exitTime is null")
    void shouldThrowExceptionWhenExitTimeIsNull() {
        Instant entryTime = Instant.now();
        BigDecimal basePrice = new BigDecimal("10.00");

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateFee(entryTime, null, basePrice);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when basePrice is null")
    void shouldThrowExceptionWhenBasePriceIsNull() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofHours(1));

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateFee(entryTime, exitTime, null);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when exitTime is before entryTime")
    void shouldThrowExceptionWhenExitTimeIsBeforeEntryTime() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.minus(Duration.ofHours(1));
        BigDecimal basePrice = new BigDecimal("10.00");

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateFee(entryTime, exitTime, basePrice);
        });
    }

    @Test
    @DisplayName("Should maintain BigDecimal precision with scale 2")
    void shouldMaintainPrecisionWithScale2() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plus(Duration.ofMinutes(61));
        BigDecimal basePrice = new BigDecimal("9.99");

        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);

        assertEquals(2, fee.scale());
        assertEquals(new BigDecimal("9.99"), fee);
    }
}
