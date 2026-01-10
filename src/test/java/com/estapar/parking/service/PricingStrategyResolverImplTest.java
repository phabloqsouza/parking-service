package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PricingStrategyResolverImpl Unit Tests")
class PricingStrategyResolverImplTest {

    @Mock
    private PricingStrategyRepository repository;

    @InjectMocks
    private PricingStrategyResolverImpl resolver;

    private PricingStrategy strategy;
    private UUID strategyId;

    @BeforeEach
    void setUp() {
        strategyId = UUID.randomUUID();
        strategy = new PricingStrategy();
        strategy.setId(strategyId);
        strategy.setOccupancyMinPercentage(new BigDecimal("0.00"));
        strategy.setOccupancyMaxPercentage(new BigDecimal("25.00"));
        strategy.setMultiplier(new BigDecimal("0.90"));
        strategy.setDescription("Low occupancy discount");
        strategy.setIsActive(true);
        strategy.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should find strategy by occupancy percentage successfully")
    void shouldFindStrategyByOccupancyPercentageSuccessfully() {
        // Given
        BigDecimal occupancyPercentage = new BigDecimal("15.50"); // Within 0-25% range
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage)).thenReturn(Optional.of(strategy));

        // When
        PricingStrategy result = resolver.findStrategyByOccupancy(occupancyPercentage);

        // Then
        assertNotNull(result);
        assertEquals(strategyId, result.getId());
        assertEquals(new BigDecimal("0.90"), result.getMultiplier());
        verify(repository).findActiveStrategyByOccupancyRange(occupancyPercentage);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no strategy found")
    void shouldThrowExceptionWhenNoStrategyFound() {
        // Given
        BigDecimal occupancyPercentage = new BigDecimal("15.50");
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage)).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                resolver.findStrategyByOccupancy(occupancyPercentage));

        assertTrue(exception.getMessage().contains("No active pricing strategy found"));
        assertTrue(exception.getMessage().contains("15.50"));
    }

    @Test
    @DisplayName("Should find strategy at minimum boundary")
    void shouldFindStrategyAtMinimumBoundary() {
        // Given
        BigDecimal occupancyPercentage = new BigDecimal("0.00"); // Minimum boundary
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage)).thenReturn(Optional.of(strategy));

        // When
        PricingStrategy result = resolver.findStrategyByOccupancy(occupancyPercentage);

        // Then
        assertNotNull(result);
        verify(repository).findActiveStrategyByOccupancyRange(occupancyPercentage);
    }

    @Test
    @DisplayName("Should find strategy at maximum boundary")
    void shouldFindStrategyAtMaximumBoundary() {
        // Given
        BigDecimal occupancyPercentage = new BigDecimal("24.99"); // Just below max boundary
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage)).thenReturn(Optional.of(strategy));

        // When
        PricingStrategy result = resolver.findStrategyByOccupancy(occupancyPercentage);

        // Then
        assertNotNull(result);
        verify(repository).findActiveStrategyByOccupancyRange(occupancyPercentage);
    }

    @Test
    @DisplayName("Should find strategy for high occupancy")
    void shouldFindStrategyForHighOccupancy() {
        // Given
        PricingStrategy highOccupancyStrategy = new PricingStrategy();
        highOccupancyStrategy.setId(UUID.randomUUID());
        highOccupancyStrategy.setOccupancyMinPercentage(new BigDecimal("75.00"));
        highOccupancyStrategy.setOccupancyMaxPercentage(new BigDecimal("100.00"));
        highOccupancyStrategy.setMultiplier(new BigDecimal("1.25")); // +25% increase
        highOccupancyStrategy.setIsActive(true);

        BigDecimal occupancyPercentage = new BigDecimal("90.00");
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage)).thenReturn(Optional.of(highOccupancyStrategy));

        // When
        PricingStrategy result = resolver.findStrategyByOccupancy(occupancyPercentage);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("1.25"), result.getMultiplier());
        verify(repository).findActiveStrategyByOccupancyRange(occupancyPercentage);
    }
}
