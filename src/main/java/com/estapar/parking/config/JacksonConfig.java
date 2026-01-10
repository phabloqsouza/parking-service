package com.estapar.parking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {
    
    @Value("${parking.application.timezone:America/Sao_Paulo}")
    private String applicationTimezone;
    
    @Value("${parking.api.use-local-timezone:false}")
    private boolean useLocalTimezone;
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Set timezone for application (UTC-3 by default)
        TimeZone timeZone = TimeZone.getTimeZone(applicationTimezone);
        mapper.setTimeZone(timeZone);
        
        // If useLocalTimezone is false, serialize Instant as UTC (ISO 8601 with Z)
        if (!useLocalTimezone) {
            mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        
        return mapper;
    }
}
