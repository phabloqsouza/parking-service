package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.ParkingSpotService;
import com.estapar.parking.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.estapar.parking.api.dto.EventType.EXIT;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExitEventHandler extends BaseEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExitEventHandler.class);
    
    private final ParkingSessionRepository sessionRepository;
    private final ParkingSessionService parkingSessionService;
    private final ParkingSpotService parkingSpotService;
    private final PricingService pricingService;
    
    @Override
    public void handle(Garage garage, WebhookEventDto event) {
        if (!(event instanceof ExitEventDto exitEvent)) {
            throw new IllegalArgumentException("Event must be an ExitEventDto");
        }
        
        String vehicleLicensePlate = exitEvent.getLicensePlate();
        Instant exitTime = exitEvent.getExitTime();
        
        // Find active session - throws exception if not found (required for pricing)
        ParkingSession session = parkingSessionService.findActiveSession(garage, vehicleLicensePlate);
        
        // Calculate final price
        // ParkingFeeCalculator handles null basePrice (user entered but didn't park) and free time duration
        BigDecimal finalPrice = pricingService.calculateFee(
                session.getEntryTime(), exitTime, session.getBasePrice());
        
        // Free spot if assigned and decrement sector capacity
        parkingSpotService.freeSpot(session);
        
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
        return EXIT.equals(event.getEventType());
    }
}
