package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.WebhookEventDto;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseEventHandler implements EventHandler {
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public abstract void handle(com.estapar.parking.infrastructure.persistence.entity.Garage garage, WebhookEventDto event);
    
    protected <T extends WebhookEventDto> T castEvent(WebhookEventDto event, Class<T> eventClass) {
        if (!eventClass.isInstance(event)) {
            throw new IllegalArgumentException("Event must be a " + eventClass.getSimpleName());
        }
        return eventClass.cast(event);
    }
}
