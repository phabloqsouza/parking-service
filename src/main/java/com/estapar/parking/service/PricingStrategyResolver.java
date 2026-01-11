package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;

import java.math.BigDecimal;

public interface PricingStrategyResolver {
    
    PricingStrategy findStrategy(BigDecimal occupancyPercentage);
}
