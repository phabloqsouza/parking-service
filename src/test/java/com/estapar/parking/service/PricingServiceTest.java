package com.estapar.parking.service;

import com.estapar.parking.config.DecimalConfig;
import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {
    
    private PricingService pricingService;
    
    @Mock
    private ParkingFeeCalculator feeCalculator;
    
    @Mock
    private PricingStrategyResolver strategyResolver;
    
    private DecimalConfig decimalConfig;
    
    @BeforeEach
    void setUp() {
        decimalConfig = new DecimalConfig();
        decimalConfig.setCurrencyScale(2);
        decimalConfig.setPercentageScale(2);
        decimalConfig.setRoundingMode(RoundingMode.HALF_UP);
        pricingService = new PricingService(feeCalculator, strategyResolver, decimalConfig);
    }
    
    @Test
    void calculateOccupancy_WithValidValues_ShouldReturnPercentage() {
        Integer occupiedCount = 25;
        Integer maxCapacity = 100;
        
        BigDecimal occupancy = pricingService.calculateOccupancy(occupiedCount, maxCapacity);
        
        assertEquals(new BigDecimal("25.00"), occupancy);
    }
    
    @Test
    void calculateOccupancy_WithNullOccupiedCount_ShouldReturnZero() {
        BigDecimal occupancy = pricingService.calculateOccupancy(null, 100);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), occupancy);
    }
    
    @Test
    void calculateOccupancy_WithNullMaxCapacity_ShouldReturnZero() {
        BigDecimal occupancy = pricingService.calculateOccupancy(25, null);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), occupancy);
    }
    
    @Test
    void calculateOccupancy_WithZeroMaxCapacity_ShouldReturnZero() {
        BigDecimal occupancy = pricingService.calculateOccupancy(25, 0);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), occupancy);
    }
    
    @Test
    void calculateOccupancy_WithPartialOccupancy_ShouldCalculateCorrectly() {
        Integer occupiedCount = 33;
        Integer maxCapacity = 100;
        
        BigDecimal occupancy = pricingService.calculateOccupancy(occupiedCount, maxCapacity);
        
        assertEquals(new BigDecimal("33.00"), occupancy);
    }
    
    @Test
    void applyPricing_WithStrategy_ShouldApplyMultiplier() {
        BigDecimal basePrice = new BigDecimal("10.00");
        BigDecimal occupancyPercentage = new BigDecimal("65.00");
        
        PricingStrategy strategy = new PricingStrategy();
        strategy.setId(UUID.randomUUID());
        strategy.setOccupancyMinPercentage(new BigDecimal("50.00"));
        strategy.setOccupancyMaxPercentage(new BigDecimal("75.00"));
        strategy.setMultiplier(new BigDecimal("1.10"));
        strategy.setIsActive(true);
        
        when(strategyResolver.findStrategy(occupancyPercentage)).thenReturn(strategy);
        
        BigDecimal result = pricingService.applyPricing(basePrice, occupancyPercentage);
        
        assertEquals(new BigDecimal("11.00"), result);
    }
    
    @Test
    void calculateFee_ShouldDelegateToFeeCalculator() {
        Instant entryTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant exitTime = Instant.parse("2025-01-01T11:00:00Z");
        BigDecimal basePriceWithDynamicPricing = new BigDecimal("11.00");
        BigDecimal expectedFee = new BigDecimal("11.00");
        
        when(feeCalculator.calculateFee(entryTime, exitTime, basePriceWithDynamicPricing))
            .thenReturn(expectedFee);
        
        BigDecimal fee = pricingService.calculateFee(entryTime, exitTime, basePriceWithDynamicPricing);
        
        assertEquals(expectedFee, fee);
    }
}
