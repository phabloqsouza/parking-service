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
import com.estapar.parking.service.PricingStrategyResolver;
import com.estapar.parking.util.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.estapar.parking.api.dto.EventType.ENTRY;
import static com.estapar.parking.api.exception.ErrorMessages.GARAGE_FULL;
import static com.estapar.parking.api.exception.ErrorMessages.VEHICLE_ALREADY_HAS_ACTIVE_SESSION;
import static com.estapar.parking.api.exception.ErrorMessages.conflict;
import static java.math.BigDecimal.valueOf;

@Component
@RequiredArgsConstructor
public class EntryEventHandler extends BaseEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryEventHandler.class);
    
    private final GarageRepository garageRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ParkingMapper parkingMapper;
    private final ParkingSessionService parkingSessionService;
    private final PricingStrategyResolver pricingStrategyResolver;
    private final BigDecimalUtils bigDecimalUtils;
    
    @Override
    public void handle(Garage garage, WebhookEventDto event) {
        EntryEventDto entryEvent = requireEventType(event, EntryEventDto.class);

        if (isGarageFull(garage)) {
            throw conflict(GARAGE_FULL);
        }

        if (parkingSessionService.existsActiveSession(garage, entryEvent.getLicensePlate())) {
            throw conflict(VEHICLE_ALREADY_HAS_ACTIVE_SESSION, entryEvent.getLicensePlate());
        }

        long occupied = garageRepository.calcOccupancy(garage.getId());
        var occupancyPercentage = bigDecimalUtils.calculatePercentage(
                valueOf(occupied), 
                valueOf(garage.getMaxCapacity()));
        
        var strategy = pricingStrategyResolver.findStrategy(occupancyPercentage);

        ParkingSession session = parkingMapper.toParkingSession(
                entryEvent.getLicensePlate(),
                entryEvent.getEntryTime(),
                garage,
                strategy.getMultiplier()
        );
        
        sessionRepository.save(session);

        logger.info("Entry event processed: vehicle={}, multiplier={}, entryTime={}",
                   entryEvent.getLicensePlate(), strategy.getMultiplier(), entryEvent.getEntryTime());
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return ENTRY.equals(event.getEventType());
    }
    
    private boolean isGarageFull(Garage garage) {
        long availableCapacity = garage.getMaxCapacity() - garageRepository.calcOccupancy(garage.getId());
        return availableCapacity <= 0;
    }
}
