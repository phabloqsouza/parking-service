package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.service.ParkingSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.estapar.parking.api.dto.EventType.ENTRY;
import static com.estapar.parking.api.exception.ErrorMessages.GARAGE_FULL;
import static com.estapar.parking.api.exception.ErrorMessages.VEHICLE_ALREADY_HAS_ACTIVE_SESSION;
import static com.estapar.parking.api.exception.ErrorMessages.conflict;

@Component
@RequiredArgsConstructor
public class EntryEventHandler extends BaseEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryEventHandler.class);
    
    private final GarageRepository garageRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ParkingMapper parkingMapper;
    private final ParkingSessionService parkingSessionService;
    
    @Override
    public void handle(Garage garage, WebhookEventDto event) {
        EntryEventDto entryEvent = requireEventType(event, EntryEventDto.class);

        if (isGarageFull(garage)) {
            throw conflict(GARAGE_FULL);
        }

        if (parkingSessionService.existsActiveSession(garage, entryEvent.getLicensePlate())) {
            throw conflict(VEHICLE_ALREADY_HAS_ACTIVE_SESSION, entryEvent.getLicensePlate());
        }

        ParkingSession session = parkingMapper.toParkingSession(
                entryEvent.getLicensePlate(),
                entryEvent.getEntryTime(),
                garage
        );
        sessionRepository.save(session);

        logger.info("Entry event processed: vehicle={}", entryEvent.getLicensePlate());
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return ENTRY.equals(event.getEventType());
    }
    
    /**
     * Checks if garage is at full capacity.
     * Capacity calculation includes sessions without sector (entered but not parked) 
     * plus sum of all sector occupied_count values.
     */
    private boolean isGarageFull(Garage garage) {
        long availableCapacity = garage.getMaxCapacity() - garageRepository.calcOccupancy(garage.getId());
        return availableCapacity <= 0;
    }
}
