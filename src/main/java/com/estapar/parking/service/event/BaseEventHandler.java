package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.WebhookEventDto;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseEventHandler implements EventHandler {
    
    /**
     * Handles a parking event within a transaction with REPEATABLE_READ isolation level.
     * 
     * REPEATABLE_READ isolation is used to ensure:
     * - Consistent capacity calculations during concurrent entry events
     * - Stable pricing multiplier determination based on garage occupancy
     * - Prevention of non-repeatable reads when checking garage capacity
     * - Consistent view of data throughout the event processing
     * 
     * This isolation level prevents phantom reads and ensures that capacity checks
     * and pricing calculations see a consistent snapshot of the database state.
     * 
     * @param garage the garage where the event occurred
     * @param event the webhook event to process
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
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
