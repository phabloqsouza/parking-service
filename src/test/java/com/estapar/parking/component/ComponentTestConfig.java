package com.estapar.parking.component;

import com.estapar.parking.ParkingServiceApplication;
import com.estapar.parking.infrastructure.external.GarageSimulatorFeignClient;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(classes = ParkingServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ComponentTestConfig {

    @MockBean
    private GarageSimulatorFeignClient garageSimulatorFeignClient;
}
