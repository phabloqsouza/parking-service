package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingStrategyResolver {
    
    private final PricingStrategyRepository repository;
    
    @Transactional(readOnly = true)
    public PricingStrategy findStrategy(BigDecimal occupancyPercentage) {
        return repository.findActiveStrategyByOccupancyRange(occupancyPercentage)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                    String.format("No active pricing strategy found for occupancy percentage: %.2f", 
                                 occupancyPercentage)));
    }
}
