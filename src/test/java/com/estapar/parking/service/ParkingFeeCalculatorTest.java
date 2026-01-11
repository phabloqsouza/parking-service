package com.estapar.parking.service;

import com.estapar.parking.config.DecimalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ParkingFeeCalculatorTest {
    
    private ParkingFeeCalculator calculator;
    private DecimalConfig decimalConfig;
    
    @BeforeEach
    void setUp() {
        decimalConfig = new DecimalConfig();
        decimalConfig.setCurrencyScale(2);
        decimalConfig.setRoundingMode(RoundingMode.HALF_UP);
        calculator = new ParkingFeeCalculator(decimalConfig);
    }
    
    @Test
    void calculateFee_First30Minutes_ShouldReturnZero() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T10:29:00Z");
        BigDecimal basePrice = new BigDecimal("10.00");
        
        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), fee);
    }
    
    @Test
    void calculateFee_Exactly30Minutes_ShouldReturnZero() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T10:30:00Z");
        BigDecimal basePrice = new BigDecimal("10.00");
        
        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), fee);
    }
    
    @Test
    void calculateFee_31Minutes_ShouldCharge1Hour() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T10:31:00Z");
        BigDecimal basePrice = new BigDecimal("10.00");
        
        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);
        
        assertEquals(new BigDecimal("10.00"), fee);
    }
    
    @Test
    void calculateFee_91Minutes_ShouldCharge2Hours() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T11:31:00Z");
        BigDecimal basePrice = new BigDecimal("10.00");
        
        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);
        
        assertEquals(new BigDecimal("20.00"), fee);
    }
    
    @Test
    void calculateFee_WithDifferentBasePrice_ShouldCalculateCorrectly() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T11:00:00Z");
        BigDecimal basePrice = new BigDecimal("15.50");
        
        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);
        
        assertEquals(new BigDecimal("15.50"), fee);
    }
    
    @Test
    void calculateFee_ExitTimeBeforeEntryTime_ShouldThrowException() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T09:00:00Z");
        BigDecimal basePrice = new BigDecimal("10.00");
        
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateFee(entryTime, exitTime, basePrice);
        });
    }
    
    @Test
    void calculateFee_65Minutes_ShouldCharge2Hours() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T11:05:00Z");
        BigDecimal basePrice = new BigDecimal("10.00");
        
        BigDecimal fee = calculator.calculateFee(entryTime, exitTime, basePrice);
        
        assertEquals(new BigDecimal("20.00"), fee);
    }
}
