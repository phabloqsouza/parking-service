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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntryEventHandlerTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private ParkingMapper parkingMapper;

    @Mock
    private SectorCapacityService sectorCapacityService;

    @Mock
    private ParkingSessionService parkingSessionService;

    @InjectMocks
    private EntryEventHandler entryEventHandler;

    private Garage garage;
    private Sector sector;
    private EntryEventDto entryEvent;
    private ParkingSession session;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setSectorCode("A");
        sector.setOccupiedCount(50);
        sector.setMaxCapacity(100);

        entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate("ABC1234");
        entryEvent.setSector("A");
        entryEvent.setEntryTime(Instant.now());

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
    }

    @Test
    void handle_WithValidEntryEvent_ShouldCreateSession() {
        BigDecimal basePrice = new BigDecimal("10.00");
        
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), entryEvent.getSector()))
                .thenReturn(Optional.of(sector));
        when(parkingSessionService.findActiveSessionOptional(garage, entryEvent.getLicensePlate()))
                .thenReturn(Optional.empty());
        when(pricingService.applyDynamicPricing(garage.getId(), sector)).thenReturn(basePrice);
        when(parkingMapper.toParkingSession(
                eq(entryEvent.getLicensePlate()),
                eq(entryEvent.getEntryTime()),
                eq(garage),
                eq(sector),
                eq(basePrice)
        )).thenReturn(session);
        when(sectorRepository.save(sector)).thenReturn(sector);
        when(sessionRepository.save(session)).thenReturn(session);

        entryEventHandler.handle(garage, entryEvent);

        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), entryEvent.getSector());
        verify(pricingService).applyDynamicPricing(garage.getId(), sector);
        verify(sectorCapacityService).incrementCapacity(sector);
        verify(parkingMapper).toParkingSession(
                eq(entryEvent.getLicensePlate()),
                eq(entryEvent.getEntryTime()),
                eq(garage),
                eq(sector),
                eq(basePrice)
        );
        verify(sessionRepository).save(session);
    }

    @Test
    void handle_WithFullSector_ShouldThrowException() {
        sector.setOccupiedCount(100);
        sector.setMaxCapacity(100);
        
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), entryEvent.getSector()))
                .thenReturn(Optional.of(sector));

        assertThatThrownBy(() -> entryEventHandler.handle(garage, entryEvent))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).contains("Sector").contains("full");
                });

        verify(sectorRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithActiveSessionExists_ShouldThrowException() {
        ParkingSession activeSession = new ParkingSession();
        activeSession.setId(UUID.randomUUID());
        
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), entryEvent.getSector()))
                .thenReturn(Optional.of(sector));
        when(parkingSessionService.findActiveSessionOptional(garage, entryEvent.getLicensePlate()))
                .thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> entryEventHandler.handle(garage, entryEvent))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).contains("already has an active parking session");
                });

        verify(sectorRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void supports_WithEntryEvent_ShouldReturnTrue() {
        EntryEventDto event = new EntryEventDto();
        event.setEventType(EventType.ENTRY);

        boolean result = entryEventHandler.supports(event);

        assertThat(result).isTrue();
    }

    @Test
    void supports_WithNonEntryEvent_ShouldReturnFalse() {
        WebhookEventDto event = mock(WebhookEventDto.class);
        when(event.getEventType()).thenReturn(EventType.PARKED);

        boolean result = entryEventHandler.supports(event);

        assertThat(result).isFalse();
    }

    @Test
    void validate_WithFullSector_ShouldThrowException() {
        sector.setOccupiedCount(100);
        sector.setMaxCapacity(100);
        
        when(parkingSessionService.findActiveSessionOptional(garage, entryEvent.getLicensePlate()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entryEventHandler.validate(garage, sector, entryEvent))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void validate_WithActiveSession_ShouldThrowException() {
        ParkingSession activeSession = new ParkingSession();
        activeSession.setId(UUID.randomUUID());
        
        when(parkingSessionService.findActiveSessionOptional(garage, entryEvent.getLicensePlate()))
                .thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> entryEventHandler.validate(garage, sector, entryEvent))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }
}
