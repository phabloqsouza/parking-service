package com.estapar.parking.infrastructure.external;

import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
    name = "garageSimulator",
    url = "${parking.simulator.url:http://localhost:8080}",
    configuration = com.estapar.parking.config.FeignConfig.class
)
public interface GarageSimulatorFeignClient {
    
    @GetMapping("/garage")
    @CircuitBreaker(name = "garage-simulator", fallbackMethod = "getGarageConfigurationFallback")
    @Retry(name = "garage-simulator")
    GarageSimulatorResponseDto getGarageConfiguration();
    
    default GarageSimulatorResponseDto getGarageConfigurationFallback(Exception ex) {
        throw new RuntimeException("Garage simulator service is unavailable: " + ex.getMessage(), ex);
    }
}
