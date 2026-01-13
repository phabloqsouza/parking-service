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
        
        // Configure timezone based on useLocalTimezone setting
        if (useLocalTimezone) {
            // Use application timezone (America/Sao_Paulo by default) for API serialization
            TimeZone timeZone = TimeZone.getTimeZone(applicationTimezone);
            mapper.setTimeZone(timeZone);
        } else {
            // Serialize Instant as UTC (ISO 8601 with Z) when useLocalTimezone is false
            mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        
        return mapper;
    }
}
