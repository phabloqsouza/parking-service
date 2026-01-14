package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ParkedEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.ParkingSpotService;
import com.estapar.parking.service.PricingService;
import com.estapar.parking.service.SectorCapacityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.estapar.parking.api.dto.EventType.PARKED;
import static com.estapar.parking.api.exception.ErrorMessages.SPOT_ALREADY_OCCUPIED;
import static com.estapar.parking.api.exception.ErrorMessages.conflict;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkedEventHandler extends BaseEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ParkedEventHandler.class);
    
    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionService parkingSessionService;
    private final ParkingSpotService parkingSpotService;
    private final PricingService pricingService;
    private final SectorCapacityService sectorCapacityService;
    
    @Override
    public void handle(Garage garage, WebhookEventDto event) {
        ParkedEventDto parkedEvent = requireEventType(event, ParkedEventDto.class);
        
        ParkingSession session = parkingSessionService.findActiveSession(garage, parkedEvent.getLicensePlate());
        
        if (session.getSpot() != null) {
            logger.warn("Vehicle {} already has spot assigned (spot_id: {}). Duplicate PARKED event ignored.", 
                       parkedEvent.getLicensePlate(), session.getSpot().getId());
            return;
        }
        
        findSpot(garage, parkedEvent).ifPresent(
            spot -> {
                Sector sector = spot.getSector();
                parkingSpotService.assignSpot(session, spot);
                
                BigDecimal basePrice = pricingService.calculateDynamicPrice(sector);
                
                sectorCapacityService.incrementCapacity(sector);
                
                session.setBasePrice(basePrice);
                sessionRepository.save(session);
                logger.info("Parked event processed: vehicle={}, spot_id={}, sector={}, basePrice={}", 
                           parkedEvent.getLicensePlate(), spot.getId(), sector.getSectorCode(), basePrice);
            }
        );
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return EventType.PARKED.equals(event.getEventType());
    }
    
    /**
     * Finds parking spot by exact coordinates.
     * Uses precise matching (no tolerance) - coordinates must match exactly.
     * If spot not found, session remains without sector_id and spot_id,
     * but still counts toward garage capacity.
     */
    private Optional<ParkingSpot> findSpot(Garage garage, ParkedEventDto parkedEvent) {
        return spotRepository
                .findByGarageIdAndLatitudeAndLongitude(
                        garage.getId(), 
                        parkedEvent.getLat(), 
                        parkedEvent.getLng())
                .map(spot -> {
                    if (spot.getIsOccupied()) {
                        throw conflict(SPOT_ALREADY_OCCUPIED, spot.getId());
                    }
                    return spot;
                })
                .or(() -> {
                    logger.warn("No parking spot found for exact coordinates ({}, {}) in garage {}. " +
                               "Spot assignment skipped (graceful degradation).", 
                               parkedEvent.getLat(), parkedEvent.getLng(), garage.getId());
                    return Optional.empty();
                });
    }
}
