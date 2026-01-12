package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.PricingStrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingStrategyResolver {
    
    private final PricingStrategyRepository repository;
    
    @Transactional(readOnly = true)
    public PricingStrategy findStrategy(UUID garageId, BigDecimal occupancyPercentage) {
        return repository.findActiveStrategyByGarageAndOccupancyRange(garageId, occupancyPercentage)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("No active pricing strategy found for garage %s and occupancy percentage: %.2f", 
                                 garageId, occupancyPercentage)));
    }
}
