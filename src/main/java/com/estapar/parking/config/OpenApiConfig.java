package com.estapar.parking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:3003}")
    private String serverPort;
    
    @Bean
    public OpenAPI parkingServiceOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Parking Service API Server");
        
        Contact contact = new Contact();
        contact.setName("Parking Service API Support");
        
        License license = new License()
                .name("Proprietary")
                .url("https://example.com/license");
        
        Info info = new Info()
                .title("Parking Garage Management API")
                .version("1.0.0")
                .contact(contact)
                .description("REST API for managing parking garage operations, including vehicle entry/exit, " +
                           "spot management, dynamic pricing, and revenue tracking.")
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
