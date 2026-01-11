package com.estapar.parking.component.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration
public class ComponentTestConfig {
    // Spring context configuration for component tests
    // Tests use RestAssured to call the actual running service
}
