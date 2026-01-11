package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.WebhookEventDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventHandlerFactory {
    
    private final Map<EventType, EventHandler> handlers;
    
    public EventHandlerFactory(EntryEventHandler entryHandler,
                              ParkedEventHandler parkedHandler,
                              ExitEventHandler exitHandler) {
        this.handlers = Map.of(
                EventType.ENTRY, entryHandler,
                EventType.PARKED, parkedHandler,
                EventType.EXIT, exitHandler
        );
    }
    
    public EventHandler getHandler(WebhookEventDto event) {
        EventType eventType = event.getEventType();
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        
        EventHandler handler = handlers.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for event type: " + eventType);
        }
        
        return handler;
    }
}
