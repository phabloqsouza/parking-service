package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.WebhookEventDto;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseEventHandler implements EventHandler {
    
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public abstract void handle(com.estapar.parking.infrastructure.persistence.entity.Garage garage, WebhookEventDto event);
    
    /**
     * Validates that the event is of the expected type and casts it.
     * 
     * @param event the event to validate
     * @param expectedType the expected event type class
     * @param <T> the event type
     * @return the casted event
     * @throws IllegalArgumentException if the event is not of the expected type
     */
    protected <T> T requireEventType(WebhookEventDto event, Class<T> expectedType) {
        if (!expectedType.isInstance(event)) {
            throw new IllegalArgumentException("Event must be a " + expectedType.getSimpleName());
        }
        return expectedType.cast(event);
    }
}
