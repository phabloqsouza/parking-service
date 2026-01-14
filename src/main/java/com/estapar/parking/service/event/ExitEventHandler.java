package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.ParkingSpotService;
import com.estapar.parking.service.ParkingFeeCalculator;
import com.estapar.parking.util.BigDecimalUtils;
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
    private final ParkingFeeCalculator feeCalculator;
    private final BigDecimalUtils bigDecimalUtils;
    
    @Override
    public void handle(Garage garage, WebhookEventDto event) {
        ExitEventDto exitEvent = requireEventType(event, ExitEventDto.class);
        
        ParkingSession session = parkingSessionService.findActiveSession(garage, exitEvent.getLicensePlate());
        session.setExitTime(exitEvent.getExitTime());
        
        BigDecimal multiplier = session.getPricingMultiplier();
        BigDecimal basePrice;
        if (session.getSpot() != null) {
            basePrice = session.getSpot().getSector().getBasePrice();
        } else {
            basePrice = bigDecimalUtils.zeroWithCurrencyScale();
        }
        
        BigDecimal effectivePrice = bigDecimalUtils.multiplyAndSetCurrencyScale(basePrice, multiplier);
        BigDecimal finalPrice = feeCalculator.calculateFee(
                session.getEntryTime(), 
                session.getExitTime(), 
                effectivePrice
        );
        
        parkingSpotService.freeSpot(session);
        
        session.setFinalPrice(finalPrice);
        sessionRepository.save(session);
        
        long durationMinutes = Duration.between(session.getEntryTime(), exitEvent.getExitTime()).toMinutes();
        logger.info("Exit event processed: vehicle={}, finalPrice={},  exitTime={}",
                exitEvent.getLicensePlate(), finalPrice, durationMinutes, exitEvent.getExitTime());
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return EXIT.equals(event.getEventType());
    }
}
