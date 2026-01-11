package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.ParkingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExitEventHandler implements EventHandler {
    
    private final ParkingEventService parkingEventService;
    
    @Override
    public void handle(UUID garageId, WebhookEventDto event) {
        if (!(event instanceof ExitEventDto exitEvent)) {
            throw new IllegalArgumentException("Event must be an ExitEventDto");
        }
        
        parkingEventService.handleExitEvent(
                garageId,
                exitEvent.getLicensePlate(),
                exitEvent.getExitTime()
        );
    }
}
