package com.estapar.parking.component.config;

import com.estapar.parking.ParkingServiceApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.ObjectMapperType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.PostConstruct;

@CucumberContextConfiguration
@SpringBootTest(classes = ParkingServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ComponentTestConfig {

    @LocalServerPort
    protected int port;

    @PostConstruct
    public void initRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(new ObjectMapperConfig(ObjectMapperType.JACKSON_2));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
