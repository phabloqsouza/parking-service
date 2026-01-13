package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
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
import com.estapar.parking.service.ParkingSpotService;
import com.estapar.parking.service.PricingService;
import com.estapar.parking.service.SectorCapacityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkedEventHandlerTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private ParkingSessionService parkingSessionService;

    @Mock
    private ParkingSpotService parkingSpotService;

    @Mock
    private PricingService pricingService;

    @Mock
    private SectorCapacityService sectorCapacityService;

    @InjectMocks
    private ParkedEventHandler parkedEventHandler;

    private Garage garage;
    private ParkedEventDto parkedEvent;
    private ParkingSession session;
    private ParkingSpot spot;
    private Sector sector;
    private String licensePlate;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        licensePlate = "ABC1234";

        parkedEvent = new ParkedEventDto();
        parkedEvent.setEventType(EventType.PARKED);
        parkedEvent.setLicensePlate(licensePlate);
        parkedEvent.setLat(new BigDecimal("-23.550520"));
        parkedEvent.setLng(new BigDecimal("-46.633308"));

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setSectorCode("A");
        sector.setGarage(garage);

        spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setSector(sector);
        spot.setIsOccupied(false);
        spot.setLatitude(new BigDecimal("-23.550520"));
        spot.setLongitude(new BigDecimal("-46.633308"));

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setSpot(null);
    }

    @Test
    void handle_WithSpotFound_ShouldAssignSpotAndCalculatePrice() {
        BigDecimal basePrice = new BigDecimal("15.00");

        when(parkingSessionService.findActiveSession(garage, licensePlate)).thenReturn(session);
        when(spotRepository.findByGarageIdAndLatitudeAndLongitude(
                garage.getId(), parkedEvent.getLat(), parkedEvent.getLng()))
                .thenReturn(Optional.of(spot));
        when(pricingService.calculateDynamicPrice(sector)).thenReturn(basePrice);
        when(sessionRepository.save(session)).thenReturn(session);

        parkedEventHandler.handle(garage, parkedEvent);

        verify(parkingSessionService).findActiveSession(garage, licensePlate);
        verify(spotRepository).findByGarageIdAndLatitudeAndLongitude(
                garage.getId(), parkedEvent.getLat(), parkedEvent.getLng());
        verify(parkingSpotService).assignSpot(session, spot);
        verify(pricingService).calculateDynamicPrice(sector);
        verify(sectorCapacityService).incrementCapacity(sector);
        verify(sessionRepository).save(session);
        assertThat(session.getBasePrice()).isEqualTo(basePrice);
    }

    @Test
    void handle_WithSpotOccupied_ShouldThrowException() {
        spot.setIsOccupied(true);

        when(parkingSessionService.findActiveSession(garage, licensePlate)).thenReturn(session);
        when(spotRepository.findByGarageIdAndLatitudeAndLongitude(
                garage.getId(), parkedEvent.getLat(), parkedEvent.getLng()))
                .thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> parkedEventHandler.handle(garage, parkedEvent))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(parkingSessionService).findActiveSession(garage, licensePlate);
        verify(spotRepository).findByGarageIdAndLatitudeAndLongitude(
                garage.getId(), parkedEvent.getLat(), parkedEvent.getLng());
        verify(parkingSpotService, never()).assignSpot(any(), any());
    }

    @Test
    void handle_WithNoSpotFound_ShouldNotAssignSpot() {
        when(parkingSessionService.findActiveSession(garage, licensePlate)).thenReturn(session);
        when(spotRepository.findByGarageIdAndLatitudeAndLongitude(
                garage.getId(), parkedEvent.getLat(), parkedEvent.getLng()))
                .thenReturn(Optional.empty());

        parkedEventHandler.handle(garage, parkedEvent);

        verify(parkingSessionService).findActiveSession(garage, licensePlate);
        verify(spotRepository).findByGarageIdAndLatitudeAndLongitude(
                garage.getId(), parkedEvent.getLat(), parkedEvent.getLng());
        verify(parkingSpotService, never()).assignSpot(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithDuplicateEvent_ShouldIgnore() {
        ParkingSpot existingSpot = new ParkingSpot();
        existingSpot.setId(UUID.randomUUID());
        session.setSpot(existingSpot);

        when(parkingSessionService.findActiveSession(garage, licensePlate)).thenReturn(session);

        parkedEventHandler.handle(garage, parkedEvent);

        verify(parkingSessionService).findActiveSession(garage, licensePlate);
        verify(spotRepository, never()).findByGarageIdAndLatitudeAndLongitude(any(), any(), any());
        verify(parkingSpotService, never()).assignSpot(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithInvalidEventType_ShouldThrowException() {
        EntryEventDto entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate(licensePlate);

        assertThatThrownBy(() -> parkedEventHandler.handle(garage, entryEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ParkedEventDto");
    }

    @Test
    void supports_WithParkedEvent_ShouldReturnTrue() {
        boolean result = parkedEventHandler.supports(parkedEvent);

        assertThat(result).isTrue();
    }

    @Test
    void supports_WithNonParkedEvent_ShouldReturnFalse() {
        EntryEventDto entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);

        boolean result = parkedEventHandler.supports(entryEvent);

        assertThat(result).isFalse();
    }
}
