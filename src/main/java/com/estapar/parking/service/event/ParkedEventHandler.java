package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.ParkedEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.ParkingEventService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ParkedEventHandler implements EventHandler {
    
    private final ParkingEventService parkingEventService;
    
    public ParkedEventHandler(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }
    
    @Override
    public void handle(UUID garageId, WebhookEventDto event) {
        if (!(event instanceof ParkedEventDto parkedEvent)) {
            throw new IllegalArgumentException("Event must be a ParkedEventDto");
        }
        
        parkingEventService.handleParkedEvent(
                garageId,
                parkedEvent.getLicensePlate(),
                parkedEvent.getLat(),
                parkedEvent.getLng()
        );
    }
}
