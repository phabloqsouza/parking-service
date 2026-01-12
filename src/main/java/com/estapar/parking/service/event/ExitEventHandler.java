package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.exception.ParkingSessionNotFoundException;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.PricingService;
import com.estapar.parking.service.SectorCapacityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExitEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExitEventHandler.class);
    
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final PricingService pricingService;
    private final SectorCapacityService sectorCapacityService;
    private final ParkingSessionService parkingSessionService;
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handle(Garage garage, WebhookEventDto event) {
        if (!(event instanceof ExitEventDto exitEvent)) {
            throw new IllegalArgumentException("Event must be an ExitEventDto");
        }
        
        String vehicleLicensePlate = exitEvent.getLicensePlate();
        Instant exitTime = exitEvent.getExitTime();
        
        // Find active session
        ParkingSession session = parkingSessionService.findActiveSession(garage, vehicleLicensePlate);
        
        // Calculate final price
        BigDecimal finalPrice = pricingService.calculateFee(
                session.getEntryTime(), exitTime, session.getBasePrice());
        
        // Free spot if assigned
        if (session.getSpot() != null) {
            ParkingSpot spot = session.getSpot();
            spot.setIsOccupied(false);
            spotRepository.save(spot);
        }
        
        // Decrement sector capacity
        Sector sector = session.getSector();
        sectorCapacityService.decrementCapacity(sector);
        
        // Update session
        session.setExitTime(exitTime);
        session.setFinalPrice(finalPrice);
        sessionRepository.save(session);
        
        logger.info("Exit event processed: vehicle={}, finalPrice={}, duration={} minutes", 
                   vehicleLicensePlate, finalPrice, 
                   java.time.Duration.between(session.getEntryTime(), exitTime).toMinutes());
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return EventType.EXIT.equals(event.getEventType());
    }
}
