package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.api.exception.ErrorMessages;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.service.ParkingSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static com.estapar.parking.api.dto.EventType.ENTRY;
import static com.estapar.parking.api.exception.ErrorMessages.GARAGE_FULL;
import static com.estapar.parking.api.exception.ErrorMessages.VEHICLE_ALREADY_HAS_ACTIVE_SESSION;
import static java.math.BigDecimal.ZERO;
import static org.springframework.http.HttpStatus.CONFLICT;

@Component
@RequiredArgsConstructor
public class EntryEventHandler extends BaseEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryEventHandler.class);
    
    private final SectorRepository sectorRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ParkingMapper parkingMapper;
    private final ParkingSessionService parkingSessionService;
    
    @Override
    public void handle(Garage garage, WebhookEventDto event) {
        if (!(event instanceof EntryEventDto entryEvent)) {
            throw new IllegalArgumentException("Event must be an EntryEventDto");
        }

        if (sectorRepository.calcAvailableCapacity(garage.getId()) <= 0) {
            throw new ResponseStatusException(CONFLICT, GARAGE_FULL);
        }

        if (parkingSessionService.existsActiveSession(garage, entryEvent.getLicensePlate())) {
            throw new ResponseStatusException(CONFLICT,
                    String.format(VEHICLE_ALREADY_HAS_ACTIVE_SESSION, entryEvent.getLicensePlate()));
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
}
