package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.service.ParkingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntryEventHandler implements EventHandler {
    
    private final ParkingEventService parkingEventService;
    
    @Override
    public void handle(UUID garageId, WebhookEventDto event) {
        if (!(event instanceof EntryEventDto entryEvent)) {
            throw new IllegalArgumentException("Event must be an EntryEventDto");
        }
        
        String sector = entryEvent.getSector() != null ? entryEvent.getSector() : "A";
        parkingEventService.handleEntryEvent(
                garageId,
                entryEvent.getLicensePlate(),
                entryEvent.getEntryTime(),
                sector
        );
    }
}
