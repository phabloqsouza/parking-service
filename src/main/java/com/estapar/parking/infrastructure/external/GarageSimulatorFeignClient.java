package com.estapar.parking.infrastructure.external;

import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
    name = "garageSimulator",
    url = "${parking.simulator.url:http://localhost:3000}",
    configuration = com.estapar.parking.config.FeignConfig.class
)
public interface GarageSimulatorFeignClient {
    
    @GetMapping("/garage")
    GarageSimulatorResponseDto getGarageConfiguration();
}
