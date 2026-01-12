package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;

public interface EventHandler {
    void handle(Garage garage, WebhookEventDto event);

    boolean supports(WebhookEventDto event);
}
