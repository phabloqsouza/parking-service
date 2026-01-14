package com.estapar.parking.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class InstantDeserializer extends StdDeserializer<Instant> {
    
    private static final DateTimeFormatter WEBHOOK_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final ZoneId timezone;
    
    public InstantDeserializer(ZoneId timezone) {
        super(Instant.class);
        this.timezone = timezone;
    }
    
    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText().trim();
        
        if (dateString.isEmpty()) {
            return null;
        }
        
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, WEBHOOK_DATE_FORMAT);
            return localDateTime.atZone(timezone).toInstant();
        } catch (DateTimeParseException e) {
            throw new IOException("Cannot deserialize value of type java.time.Instant from String \"" + 
                                 dateString + "\". Expected format: yyyy-MM-dd'T'HH:mm:ss (e.g., 2026-01-14T15:40:37)", e);
        }
    }
}
