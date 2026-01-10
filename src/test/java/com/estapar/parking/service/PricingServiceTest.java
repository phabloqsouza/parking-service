package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PricingService Unit Tests")
class PricingServiceTest {

    @Mock
    private ParkingFeeCalculator feeCalculator;

    @Mock
    private PricingStrategyResolver strategyResolver;

    @InjectMocks
    private PricingService pricingService;

    private PricingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PricingStrategy(
                UUID.randomUUID(),
                new BigDecimal("0.00"),
                new BigDecimal("50.00"),
                new BigDecimal("0.90"), // -10%
                "Low occupancy discount",
                true,
                Instant.now(),
                null
        );
    }

    @Test
    @DisplayName("Should calculate occupancy percentage correctly")
    void shouldCalculateOccupancyPercentage() {
        Integer occupiedCount = 25;
        Integer maxCapacity = 100;

        BigDecimal occupancy = pricingService.calculateOccupancy(occupiedCount, maxCapacity);

        assertEquals(new BigDecimal("25.00"), occupancy);
    }

    @Test
    @DisplayName("Should return zero when occupiedCount is null")
    void shouldReturnZeroWhenOccupiedCountIsNull() {
        BigDecimal occupancy = pricingService.calculateOccupancy(null, 100);

        assertEquals(BigDecimal.ZERO.setScale(2), occupancy);
    }

    @Test
    @DisplayName("Should return zero when maxCapacity is null")
    void shouldReturnZeroWhenMaxCapacityIsNull() {
        BigDecimal occupancy = pricingService.calculateOccupancy(25, null);

        assertEquals(BigDecimal.ZERO.setScale(2), occupancy);
    }

    @Test
    @DisplayName("Should return zero when maxCapacity is zero")
    void shouldReturnZeroWhenMaxCapacityIsZero() {
        BigDecimal occupancy = pricingService.calculateOccupancy(25, 0);

        assertEquals(BigDecimal.ZERO.setScale(2), occupancy);
    }

    @Test
    @DisplayName("Should calculate occupancy with decimal precision")
    void shouldCalculateOccupancyWithDecimalPrecision() {
        Integer occupiedCount = 33;
        Integer maxCapacity = 100;

        BigDecimal occupancy = pricingService.calculateOccupancy(occupiedCount, maxCapacity);

        assertEquals(new BigDecimal("33.00"), occupancy);
    }

    @Test
    @DisplayName("Should calculate 100% occupancy correctly")
    void shouldCalculate100PercentOccupancy() {
        Integer occupiedCount = 100;
        Integer maxCapacity = 100;

        BigDecimal occupancy = pricingService.calculateOccupancy(occupiedCount, maxCapacity);

        assertEquals(new BigDecimal("100.00"), occupancy);
    }

    @Test
    @DisplayName("Should apply dynamic pricing multiplier correctly")
    void shouldApplyDynamicPricingMultiplier() {
        BigDecimal basePrice = new BigDecimal("10.00");
        BigDecimal occupancyPercentage = new BigDecimal("25.00");

        when(strategyResolver.findStrategyByOccupancy(occupancyPercentage)).thenReturn(strategy);

        BigDecimal priceWithPricing = pricingService.calculateBasePriceWithDynamicPricing(
                basePrice, occupancyPercentage);

        assertEquals(new BigDecimal("9.00"), priceWithPricing);
        verify(strategyResolver).findStrategyByOccupancy(occupancyPercentage);
    }

    @Test
    @DisplayName("Should apply multiplier of 1.25 for high occupancy")
    void shouldApplyHighOccupancyMultiplier() {
        PricingStrategy highOccupancyStrategy = new PricingStrategy(
                UUID.randomUUID(),
                new BigDecimal("75.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1.25"), // +25%
                "High occupancy premium",
                true,
                Instant.now(),
                null
        );

        BigDecimal basePrice = new BigDecimal("10.00");
        BigDecimal occupancyPercentage = new BigDecimal("80.00");

        when(strategyResolver.findStrategyByOccupancy(occupancyPercentage)).thenReturn(highOccupancyStrategy);

        BigDecimal priceWithPricing = pricingService.calculateBasePriceWithDynamicPricing(
                basePrice, occupancyPercentage);

        assertEquals(new BigDecimal("12.50"), priceWithPricing);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when basePrice is null")
    void shouldThrowExceptionWhenBasePriceIsNull() {
        BigDecimal occupancyPercentage = new BigDecimal("25.00");

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.calculateBasePriceWithDynamicPricing(null, occupancyPercentage);
        });

        verifyNoInteractions(strategyResolver);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when occupancyPercentage is null")
    void shouldThrowExceptionWhenOccupancyPercentageIsNull() {
        BigDecimal basePrice = new BigDecimal("10.00");

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.calculateBasePriceWithDynamicPricing(basePrice, null);
        });

        verifyNoInteractions(strategyResolver);
    }

    @Test
    @DisplayName("Should maintain scale 2 for dynamic pricing result")
    void shouldMaintainScale2ForDynamicPricing() {
        BigDecimal basePrice = new BigDecimal("9.99");
        BigDecimal occupancyPercentage = new BigDecimal("25.00");

        when(strategyResolver.findStrategyByOccupancy(occupancyPercentage)).thenReturn(strategy);

        BigDecimal priceWithPricing = pricingService.calculateBasePriceWithDynamicPricing(
                basePrice, occupancyPercentage);

        assertEquals(2, priceWithPricing.scale());
    }

    @Test
    @DisplayName("Should delegate final price calculation to fee calculator")
    void shouldDelegateFinalPriceCalculation() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plusSeconds(3600);
        BigDecimal basePriceWithPricing = new BigDecimal("12.50");
        BigDecimal expectedFee = new BigDecimal("12.50");

        when(feeCalculator.calculateFee(entryTime, exitTime, basePriceWithPricing))
                .thenReturn(expectedFee);

        BigDecimal finalPrice = pricingService.calculateFinalPrice(entryTime, exitTime, basePriceWithPricing);

        assertEquals(expectedFee, finalPrice);
        verify(feeCalculator).calculateFee(entryTime, exitTime, basePriceWithPricing);
    }
}
