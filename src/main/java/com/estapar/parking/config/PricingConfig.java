package com.estapar.parking.config;

import com.estapar.parking.service.ParkingFeeCalculator;
import com.estapar.parking.service.PricingService;
import com.estapar.parking.service.PricingStrategyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfig {
    
    @Bean
    public ParkingFeeCalculator parkingFeeCalculator(DecimalConfig decimalConfig) {
        return new ParkingFeeCalculator(decimalConfig);
    }
    
    @Bean
    public PricingService pricingService(ParkingFeeCalculator feeCalculator,
                                        PricingStrategyResolver strategyResolver,
                                        DecimalConfig decimalConfig) {
        return new PricingService(feeCalculator, strategyResolver, decimalConfig);
    }
}
