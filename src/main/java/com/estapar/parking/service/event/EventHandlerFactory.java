package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.WebhookEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventHandlerFactory {
    
    private final EntryEventHandler entryHandler;
    private final ParkedEventHandler parkedHandler;
    private final ExitEventHandler exitHandler;
    
    private Map<EventType, EventHandler> getHandlers() {
        return Map.of(
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
        
        Map<EventType, EventHandler> handlers = getHandlers();
        EventHandler handler = handlers.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for event type: " + eventType);
        }
        
        return handler;
    }
}
