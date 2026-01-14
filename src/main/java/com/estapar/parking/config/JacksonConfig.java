package com.estapar.parking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {
    
    @Value("${parking.application.timezone:America/Sao_Paulo}")
    private String applicationTimezone;
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        SimpleModule customModule = new SimpleModule();
        ZoneId zoneId = ZoneId.of(applicationTimezone);
        customModule.addDeserializer(Instant.class, new InstantDeserializer(zoneId));
        
        mapper.registerModule(javaTimeModule);
        mapper.registerModule(customModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        TimeZone timeZone = TimeZone.getTimeZone(applicationTimezone);
        mapper.setTimeZone(timeZone);
        
        return mapper;
    }
}
