package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingStrategyResolverTest {

    @Mock
    private PricingStrategyRepository repository;

    @InjectMocks
    private PricingStrategyResolver resolver;

    private PricingStrategy pricingStrategy;
    private BigDecimal occupancyPercentage;

    @BeforeEach
    void setUp() {
        pricingStrategy = new PricingStrategy();
        pricingStrategy.setId(UUID.randomUUID());
        pricingStrategy.setMultiplier(new BigDecimal("1.5"));
        pricingStrategy.setOccupancyMinPercentage(new BigDecimal("50.00"));
        pricingStrategy.setOccupancyMaxPercentage(new BigDecimal("75.00"));
        pricingStrategy.setIsActive(true);

        occupancyPercentage = new BigDecimal("60.00");
    }

    @Test
    void findStrategy_WithExistingStrategy_ShouldReturnStrategy() {
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage))
                .thenReturn(Optional.of(pricingStrategy));

        PricingStrategy result = resolver.findStrategy(occupancyPercentage);

        assertThat(result).isEqualTo(pricingStrategy);
        verify(repository).findActiveStrategyByOccupancyRange(occupancyPercentage);
    }

    @Test
    void findStrategy_WithNoStrategy_ShouldThrowException() {
        when(repository.findActiveStrategyByOccupancyRange(occupancyPercentage))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.findStrategy(occupancyPercentage))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository).findActiveStrategyByOccupancyRange(occupancyPercentage);
    }
}
