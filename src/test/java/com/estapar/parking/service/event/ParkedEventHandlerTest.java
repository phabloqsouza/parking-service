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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkedEventHandlerTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private ParkingSessionService parkingSessionService;

    @InjectMocks
    private ParkedEventHandler parkedEventHandler;

    private Garage garage;
    private Sector sector;
    private ParkingSession session;
    private ParkingSpot spot;
    private ParkedEventDto parkedEvent;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setSectorCode("A");

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate("ABC1234");
        session.setSector(sector);

        spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setSector(sector);
        spot.setLatitude(BigDecimal.valueOf(-23.561684));
        spot.setLongitude(BigDecimal.valueOf(-46.655981));
        spot.setIsOccupied(false);

        parkedEvent = new ParkedEventDto();
        parkedEvent.setEventType(EventType.PARKED);
        parkedEvent.setLicensePlate("ABC1234");
        parkedEvent.setLat(BigDecimal.valueOf(-23.561684));
        parkedEvent.setLng(BigDecimal.valueOf(-46.655981));
    }

    @Test
    void handle_WithValidParkedEvent_ShouldAssignSpot() {
        when(parkingSessionService.findActiveSession(garage, parkedEvent.getLicensePlate()))
                .thenReturn(session);
        when(spotRepository.findBySectorIdAndLatitudeAndLongitude(
                eq(sector.getId()), eq(parkedEvent.getLat()), eq(parkedEvent.getLng())))
                .thenReturn(Optional.of(spot));
        when(spotRepository.save(spot)).thenReturn(spot);
        when(sessionRepository.save(session)).thenReturn(session);

        parkedEventHandler.handle(garage, parkedEvent);

        verify(parkingSessionService).findActiveSession(garage, parkedEvent.getLicensePlate());
        verify(spotRepository).findBySectorIdAndLatitudeAndLongitude(
                eq(sector.getId()), eq(parkedEvent.getLat()), eq(parkedEvent.getLng()));
        assertThat(spot.getIsOccupied()).isTrue();
        verify(spotRepository).save(spot);
        verify(sessionRepository).save(session);
    }

    @Test
    void handle_WithSpotNotFound_ShouldReturnGracefully() {
        when(parkingSessionService.findActiveSession(garage, parkedEvent.getLicensePlate()))
                .thenReturn(session);
        when(spotRepository.findBySectorIdAndLatitudeAndLongitude(
                eq(sector.getId()), eq(parkedEvent.getLat()), eq(parkedEvent.getLng())))
                .thenReturn(Optional.empty());

        parkedEventHandler.handle(garage, parkedEvent);

        verify(spotRepository, never()).save(any());
        verify(sessionRepository, never()).save(session);
    }

    @Test
    void handle_WithSpotAlreadyOccupied_ShouldThrowException() {
        spot.setIsOccupied(true);
        
        when(parkingSessionService.findActiveSession(garage, parkedEvent.getLicensePlate()))
                .thenReturn(session);
        when(spotRepository.findBySectorIdAndLatitudeAndLongitude(
                eq(sector.getId()), eq(parkedEvent.getLat()), eq(parkedEvent.getLng())))
                .thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> parkedEventHandler.handle(garage, parkedEvent))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).contains("already occupied");
                });

        verify(spotRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithDuplicateParkedEvent_ShouldReturnGracefully() {
        session.setSpot(spot);
        
        when(parkingSessionService.findActiveSession(garage, parkedEvent.getLicensePlate()))
                .thenReturn(session);

        parkedEventHandler.handle(garage, parkedEvent);

        verify(spotRepository, never()).findBySectorIdAndLatitudeAndLongitude(any(), any(), any());
        verify(spotRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void supports_WithParkedEvent_ShouldReturnTrue() {
        boolean result = parkedEventHandler.supports(parkedEvent);

        assertThat(result).isTrue();
    }

    @Test
    void supports_WithNonParkedEvent_ShouldReturnFalse() {
        WebhookEventDto event = mock(WebhookEventDto.class);
        when(event.getEventType()).thenReturn(EventType.ENTRY);

        boolean result = parkedEventHandler.supports(event);

        assertThat(result).isFalse();
    }

    @Test
    void handle_WithInvalidEventType_ShouldThrowException() {
        WebhookEventDto invalidEvent = mock(WebhookEventDto.class);

        assertThatThrownBy(() -> parkedEventHandler.handle(garage, invalidEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event must be a ParkedEventDto");
    }
}
