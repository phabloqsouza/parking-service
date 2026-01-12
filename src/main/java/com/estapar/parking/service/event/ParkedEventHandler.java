package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ParkedEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import com.estapar.parking.service.ParkingSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkedEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ParkedEventHandler.class);
    
    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionService parkingSessionService;
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handle(Garage garage, WebhookEventDto event) {
        if (!(event instanceof ParkedEventDto parkedEvent)) {
            throw new IllegalArgumentException("Event must be a ParkedEventDto");
        }
        
        // Find active session
        ParkingSession session = parkingSessionService.findActiveSession(garage, parkedEvent.getLicensePlate());
        
        // Check if already parked (idempotent handling)
        if (session.getSpot() != null) {
            logger.warn("Vehicle {} already has spot assigned (spot_id: {}). Duplicate PARKED event ignored.", 
                       parkedEvent.getLicensePlate(), session.getSpot().getId());
            return;
        }
        
        Optional<ParkingSpot> optionalSpot = findSpot(session, parkedEvent);

        if (optionalSpot.isEmpty()) {
            return;
        }

        ParkingSpot spot = optionalSpot.get();

        spot.setIsOccupied(true);
        spotRepository.save(spot);

        session.setSpot(spot);
        sessionRepository.save(session);

        logger.info("Parked event processed: vehicle={}, spot_id={}", parkedEvent.getLicensePlate(), spot.getId());

    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return EventType.PARKED.equals(event.getEventType());
    }
    
    private Optional<ParkingSpot> findSpot(ParkingSession session, ParkedEventDto parkedEvent) {
        Optional<ParkingSpot> optionalSpot = spotRepository
                .findBySectorIdAndLatitudeAndLongitude(
                        session.getSector().getId(), 
                        parkedEvent.getLat(), 
                        parkedEvent.getLng());
        
        if (optionalSpot.isEmpty()) {
            logger.warn("No parking spot found for exact coordinates ({}, {}) in sector {}. " +
                       "Spot assignment skipped (graceful degradation).", 
                       parkedEvent.getLat(), parkedEvent.getLng(), session.getSector().getSectorCode());
            return Optional.empty();
        }
        
        ParkingSpot matchedSpot = optionalSpot.get();
        
        // Check if spot is already occupied (optimistic locking)
        if (matchedSpot.getIsOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format("Spot %s is already occupied", matchedSpot.getId()));
        }

        return optionalSpot;
    }
}
