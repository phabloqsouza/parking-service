package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.service.ParkingSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryEventHandlerTest {

    @Mock
    private GarageRepository garageRepository;

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingMapper parkingMapper;

    @Mock
    private ParkingSessionService parkingSessionService;

    @InjectMocks
    private EntryEventHandler entryEventHandler;

    private Garage garage;
    private EntryEventDto entryEvent;
    private ParkingSession session;
    private String licensePlate;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());
        garage.setMaxCapacity(100);

        licensePlate = "ABC1234";

        entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate(licensePlate);
        entryEvent.setEntryTime(Instant.now());

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
    }

    @Test
    void handle_WithAvailableCapacity_ShouldCreateSession() {
        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(50L);
        when(parkingSessionService.existsActiveSession(garage, licensePlate)).thenReturn(false);
        when(parkingMapper.toParkingSession(entryEvent.getLicensePlate(), entryEvent.getEntryTime(), garage))
                .thenReturn(session);
        when(sessionRepository.save(session)).thenReturn(session);

        entryEventHandler.handle(garage, entryEvent);

        verify(garageRepository).calcOccupancy(garage.getId());
        verify(parkingSessionService).existsActiveSession(garage, licensePlate);
        verify(parkingMapper).toParkingSession(entryEvent.getLicensePlate(), entryEvent.getEntryTime(), garage);
        verify(sessionRepository).save(session);
    }

    @Test
    void handle_WithGarageFull_ShouldThrowException() {
        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(100L);

        assertThatThrownBy(() -> entryEventHandler.handle(garage, entryEvent))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(garageRepository).calcOccupancy(garage.getId());
        verify(parkingSessionService, never()).existsActiveSession(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithActiveSessionExists_ShouldThrowException() {
        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(50L);
        when(parkingSessionService.existsActiveSession(garage, licensePlate)).thenReturn(true);

        assertThatThrownBy(() -> entryEventHandler.handle(garage, entryEvent))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(garageRepository).calcOccupancy(garage.getId());
        verify(parkingSessionService).existsActiveSession(garage, licensePlate);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithInvalidEventType_ShouldThrowException() {
        ExitEventDto exitEvent = new ExitEventDto();
        exitEvent.setEventType(EventType.EXIT);
        exitEvent.setLicensePlate(licensePlate);

        assertThatThrownBy(() -> entryEventHandler.handle(garage, exitEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntryEventDto");
    }

    @Test
    void supports_WithEntryEvent_ShouldReturnTrue() {
        boolean result = entryEventHandler.supports(entryEvent);

        assertThat(result).isTrue();
    }

    @Test
    void supports_WithNonEntryEvent_ShouldReturnFalse() {
        ExitEventDto exitEvent = new ExitEventDto();
        exitEvent.setEventType(EventType.EXIT);

        boolean result = entryEventHandler.supports(exitEvent);

        assertThat(result).isFalse();
    }
}
