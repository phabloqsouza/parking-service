package com.estapar.parking.service;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.service.event.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookEventService {
    
    private final GarageResolver garageResolver;
    private final List<EventHandler> eventHandlers;
    
    public void processEvent(UUID garageId, WebhookEventDto eventDto) {
        Garage garage = garageResolver.getGarage(garageId);
        
        eventHandlers.stream()
                .filter(eventHandler -> eventHandler.supports(eventDto))
                .findFirst()
                .ifPresent(handler -> handler.handle(garage, eventDto));
    }
}
