package com.estapar.parking.config;

import com.estapar.parking.service.SpotLocationMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class SpotLocationMatcherConfig {
    
    @Value("${parking.spot.coordinate-tolerance:0.000001}")
    private BigDecimal coordinateTolerance;
    
    @Bean
    public SpotLocationMatcher spotLocationMatcher() {
        return new SpotLocationMatcher(coordinateTolerance);
    }
}
