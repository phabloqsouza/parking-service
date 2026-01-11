package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingStrategyResolverImplTest {
    
    @Mock
    private PricingStrategyRepository repository;
    
    private PricingStrategyResolverImpl resolver;
    
    @BeforeEach
    void setUp() {
        resolver = new PricingStrategyResolverImpl(repository);
    }
    
    @Test
    void findStrategy_WithValidOccupancy_ShouldReturnStrategy() {
        BigDecimal occupancyPercentage = new BigDecimal("65.00");
        
        PricingStrategy strategy = new PricingStrategy();
        strategy.setId(UUID.randomUUID());
        strategy.setOccupancyMinPercentage(new BigDecimal("50.00"));
        strategy.setOccupancyMaxPercentage(new BigDecimal("75.00"));
        strategy.setMultiplier(new BigDecimal("1.10"));
        strategy.setIsActive(true);
        
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage))
            .thenReturn(Optional.of(strategy));
        
        PricingStrategy result = resolver.findStrategy(occupancyPercentage);
        
        assertEquals(strategy.getId(), result.getId());
        assertEquals(new BigDecimal("1.10"), result.getMultiplier());
    }
    
    @Test
    void findStrategy_WithNoMatchingStrategy_ShouldThrowException() {
        BigDecimal occupancyPercentage = new BigDecimal("65.00");
        
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage))
            .thenReturn(Optional.empty());
        
        assertThrows(IllegalStateException.class, () -> {
            resolver.findStrategy(occupancyPercentage);
        });
    }
}
