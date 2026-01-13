package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.PricingService;
import com.estapar.parking.service.SectorCapacityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class EntryEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryEventHandler.class);
    
    private final SectorRepository sectorRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PricingService pricingService;
    private final ParkingMapper parkingMapper;
    private final SectorCapacityService sectorCapacityService;
    private final ParkingSessionService parkingSessionService;
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handle(Garage garage, WebhookEventDto event) {
        EntryEventDto entryEvent = (EntryEventDto) event;
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), entryEvent.getSector())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found: " + entryEvent.getSector()));

        validate(garage, sector, entryEvent);

        BigDecimal basePrice = pricingService.applyDynamicPricing(sector);

        sectorCapacityService.incrementCapacity(sector);

        ParkingSession session = parkingMapper.toParkingSession(
                entryEvent.getLicensePlate(),
                entryEvent.getEntryTime(),
                garage,
                basePrice
        );
        sessionRepository.save(session);

        logger.info("Entry event processed: vehicle={}, sector={}, basePrice={}",
                entryEvent.getLicensePlate(), entryEvent.getSector(), basePrice);
    }

    @Override
    public boolean supports(WebhookEventDto event) {
        return EventType.ENTRY.equals(event.getEventType());
    }

    public void validate(Garage garage, Sector sector, EntryEventDto entryEvent) {
        if (sector.isFull()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format("Sector %s is full (capacity: %d/%d)",
                             entryEvent.getSector(), sector.getOccupiedCount(), sector.getMaxCapacity()));
        }

        parkingSessionService.findActiveSessionOptional(garage, entryEvent.getLicensePlate())
                .ifPresent(session -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            String.format("Vehicle %s already has an active parking session", entryEvent.getLicensePlate()));
                });
    }
}
