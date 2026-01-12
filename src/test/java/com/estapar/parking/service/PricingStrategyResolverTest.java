package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingStrategyResolverTest {

    @Mock
    private PricingStrategyRepository repository;

    @InjectMocks
    private PricingStrategyResolver pricingStrategyResolver;

    private UUID garageId;
    private BigDecimal occupancyPercentage;
    private PricingStrategy pricingStrategy;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();
        occupancyPercentage = new BigDecimal("35.50");
        pricingStrategy = new PricingStrategy();
        pricingStrategy.setId(UUID.randomUUID());
        pricingStrategy.setMultiplier(new BigDecimal("1.00"));
        pricingStrategy.setOccupancyMinPercentage(new BigDecimal("25.00"));
        pricingStrategy.setOccupancyMaxPercentage(new BigDecimal("50.00"));
        pricingStrategy.setIsActive(true);
        pricingStrategy.setCreatedAt(Instant.now());
    }

    @Test
    void findStrategy_WithValidOccupancyRange_ShouldReturnStrategy() {
        when(repository.findActiveStrategyByGarageAndOccupancyRange(eq(garageId), eq(occupancyPercentage)))
                .thenReturn(Optional.of(pricingStrategy));

        PricingStrategy result = pricingStrategyResolver.findStrategy(garageId, occupancyPercentage);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pricingStrategy.getId());
        assertThat(result.getMultiplier()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void findStrategy_WithNoMatchingStrategy_ShouldThrowException() {
        when(repository.findActiveStrategyByGarageAndOccupancyRange(eq(garageId), eq(occupancyPercentage)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingStrategyResolver.findStrategy(garageId, occupancyPercentage))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).contains("No active pricing strategy found");
                });
    }
}
