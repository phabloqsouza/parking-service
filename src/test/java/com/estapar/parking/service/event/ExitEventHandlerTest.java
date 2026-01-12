package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.PricingService;
import com.estapar.parking.service.SectorCapacityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExitEventHandlerTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private SectorCapacityService sectorCapacityService;

    @Mock
    private ParkingSessionService parkingSessionService;

    @InjectMocks
    private ExitEventHandler exitEventHandler;

    private Garage garage;
    private Sector sector;
    private ParkingSession session;
    private ParkingSpot spot;
    private ExitEventDto exitEvent;
    private Instant entryTime;
    private Instant exitTime;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setSectorCode("A");

        spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setIsOccupied(true);

        entryTime = Instant.parse("2025-01-01T10:00:00.000Z");
        exitTime = entryTime.plusSeconds(3600); // 1 hour later

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate("ABC1234");
        session.setEntryTime(entryTime);
        session.setSector(sector);
        session.setSpot(spot);
        session.setBasePrice(new BigDecimal("10.00"));

        exitEvent = new ExitEventDto();
        exitEvent.setEventType(EventType.EXIT);
        exitEvent.setLicensePlate("ABC1234");
        exitEvent.setExitTime(exitTime);
    }

    @Test
    void handle_WithValidExitEventAndSpotAssigned_ShouldUpdateSessionAndFreeSpot() {
        BigDecimal finalPrice = new BigDecimal("10.00");
        
        when(parkingSessionService.findActiveSession(garage, exitEvent.getLicensePlate()))
                .thenReturn(session);
        when(pricingService.calculateFee(entryTime, exitTime, session.getBasePrice()))
                .thenReturn(finalPrice);
        when(spotRepository.save(spot)).thenReturn(spot);
        when(sessionRepository.save(session)).thenReturn(session);

        exitEventHandler.handle(garage, exitEvent);

        assertThat(spot.getIsOccupied()).isFalse();
        assertThat(session.getExitTime()).isEqualTo(exitTime);
        assertThat(session.getFinalPrice()).isEqualByComparingTo(finalPrice);
        verify(parkingSessionService).findActiveSession(garage, exitEvent.getLicensePlate());
        verify(pricingService).calculateFee(entryTime, exitTime, session.getBasePrice());
        verify(spotRepository).save(spot);
        verify(sectorCapacityService).decrementCapacity(sector);
        verify(sessionRepository).save(session);
    }

    @Test
    void handle_WithNoSpotAssigned_ShouldNotFreeSpot() {
        session.setSpot(null);
        BigDecimal finalPrice = new BigDecimal("10.00");
        
        when(parkingSessionService.findActiveSession(garage, exitEvent.getLicensePlate()))
                .thenReturn(session);
        when(pricingService.calculateFee(entryTime, exitTime, session.getBasePrice()))
                .thenReturn(finalPrice);
        when(sessionRepository.save(session)).thenReturn(session);

        exitEventHandler.handle(garage, exitEvent);

        verify(spotRepository, never()).save(any());
        verify(sectorCapacityService).decrementCapacity(sector);
        verify(sessionRepository).save(session);
    }

    @Test
    void handle_WithNoActiveSession_ShouldThrowException() {
        when(parkingSessionService.findActiveSession(garage, exitEvent.getLicensePlate()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No active session"));

        assertThatThrownBy(() -> exitEventHandler.handle(garage, exitEvent))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(spotRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void supports_WithExitEvent_ShouldReturnTrue() {
        boolean result = exitEventHandler.supports(exitEvent);

        assertThat(result).isTrue();
    }

    @Test
    void supports_WithNonExitEvent_ShouldReturnFalse() {
        WebhookEventDto event = mock(WebhookEventDto.class);
        when(event.getEventType()).thenReturn(EventType.ENTRY);

        boolean result = exitEventHandler.supports(event);

        assertThat(result).isFalse();
    }

    @Test
    void handle_WithInvalidEventType_ShouldThrowException() {
        WebhookEventDto invalidEvent = mock(WebhookEventDto.class);

        assertThatThrownBy(() -> exitEventHandler.handle(garage, invalidEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event must be an ExitEventDto");
    }
}
