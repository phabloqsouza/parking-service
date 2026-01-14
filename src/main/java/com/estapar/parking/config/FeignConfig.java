package com.estapar.parking.config;

import feign.Retryer;
import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    
    @Value("${parking.simulator.retry.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${parking.simulator.retry.initial-interval-millis:2000}")
    private long initialIntervalMillis;
    
    @Value("${parking.simulator.retry.max-interval-millis:32000}")
    private long maxIntervalMillis;
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            initialIntervalMillis,
            maxIntervalMillis,
            maxAttempts
        );
    }
    
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(5000, 30000, true);
    }
}
