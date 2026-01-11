package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingStrategyResolverImpl implements PricingStrategyResolver {
    
    private final PricingStrategyRepository repository;
    
    @Override
    @Transactional(readOnly = true)
    public PricingStrategy findStrategy(BigDecimal occupancyPercentage) {
        return repository.findActiveStrategyByOccupancyRange(occupancyPercentage)
                .orElseThrow(() -> new IllegalStateException(
                    String.format("No active pricing strategy found for occupancy percentage: %.2f", occupancyPercentage)));
    }
}
