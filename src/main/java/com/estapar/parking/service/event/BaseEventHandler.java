package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.WebhookEventDto;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseEventHandler implements EventHandler {
    
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
    public abstract void handle(com.estapar.parking.infrastructure.persistence.entity.Garage garage, WebhookEventDto event);
    
    protected <T> T requireEventType(WebhookEventDto event, Class<T> expectedType) {
        if (!expectedType.isInstance(event)) {
            throw new IllegalArgumentException("Event must be a " + expectedType.getSimpleName());
        }
        return expectedType.cast(event);
    }
}
