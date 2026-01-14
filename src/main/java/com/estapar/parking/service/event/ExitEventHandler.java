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
import java.time.Duration;

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
        ExitEventDto exitEvent = requireEventType(event, ExitEventDto.class);
        
        ParkingSession session = parkingSessionService.findActiveSession(garage, exitEvent.getLicensePlate());
        session.setExitTime(exitEvent.getExitTime());
        
        BigDecimal finalPrice = pricingService.calculateFee(session);
        
        parkingSpotService.freeSpot(session);
        
        session.setFinalPrice(finalPrice);
        sessionRepository.save(session);
        
        long durationMinutes = Duration.between(session.getEntryTime(), exitEvent.getExitTime()).toMinutes();
        logger.info("Exit event processed: vehicle={}, finalPrice={}, duration={} minutes",
                exitEvent.getLicensePlate(), finalPrice, durationMinutes);
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return EXIT.equals(event.getEventType());
    }
}
