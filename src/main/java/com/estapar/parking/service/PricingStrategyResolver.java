package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.estapar.parking.api.exception.ErrorMessages.PRICING_STRATEGY_NOT_FOUND;
import static com.estapar.parking.api.exception.ErrorMessages.notFound;

@Service
@RequiredArgsConstructor
public class PricingStrategyResolver {
    
    private final PricingStrategyRepository repository;
    
    @Transactional(readOnly = true)
    public PricingStrategy findStrategy(BigDecimal occupancyPercentage) {
        return repository.findActiveStrategyByOccupancyRange(occupancyPercentage)
                .orElseThrow(() -> notFound(PRICING_STRATEGY_NOT_FOUND, occupancyPercentage));
    }
}
