package com.estapar.parking.api.dto;

import java.util.UUID;

public interface ParkingEvent {
    EventType getEventType();
    String getLicensePlate();
    UUID getGarageId();
    String getSector();
}
