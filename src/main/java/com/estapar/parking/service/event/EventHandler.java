package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.WebhookEventDto;

import java.util.UUID;

public interface EventHandler {
    void handle(UUID garageId, WebhookEventDto event);
}
