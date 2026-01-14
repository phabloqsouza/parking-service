package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.service.ParkingFeeCalculator;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.ParkingSpotService;
import com.estapar.parking.util.BigDecimalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExitEventHandlerTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingSessionService parkingSessionService;

    @Mock
    private ParkingSpotService parkingSpotService;

    @Mock
    private ParkingFeeCalculator feeCalculator;

    @Mock
    private BigDecimalUtils bigDecimalUtils;

    @InjectMocks
    private ExitEventHandler exitEventHandler;

    private Garage garage;
    private ExitEventDto exitEvent;
    private ParkingSession session;
    private ParkingSpot spot;
    private Sector sector;
    private String licensePlate;
    private Instant exitTime;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        licensePlate = "ABC1234";
        exitTime = Instant.parse("2025-01-01T12:00:00.000Z");

        exitEvent = new ExitEventDto();
        exitEvent.setEventType(EventType.EXIT);
        exitEvent.setLicensePlate(licensePlate);
        exitEvent.setExitTime(exitTime);

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setBasePrice(new BigDecimal("10.00"));

        spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setSector(sector);

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setEntryTime(Instant.parse("2025-01-01T10:00:00.000Z"));
        session.setExitTime(null);
        session.setPricingMultiplier(new BigDecimal("1.00"));
        session.setSpot(spot);
    }

    @Test
    void handle_WithActiveSession_ShouldCalculateFeeAndSave() {
        BigDecimal effectivePrice = new BigDecimal("10.00");
        BigDecimal finalPrice = new BigDecimal("20.00");

        when(parkingSessionService.findActiveSession(garage, licensePlate)).thenReturn(session);
        when(bigDecimalUtils.multiplyAndSetCurrencyScale(sector.getBasePrice(), session.getPricingMultiplier()))
                .thenReturn(effectivePrice);
        when(feeCalculator.calculateFee(session.getEntryTime(), exitTime, effectivePrice)).thenReturn(finalPrice);
        when(sessionRepository.save(session)).thenReturn(session);

        exitEventHandler.handle(garage, exitEvent);

        assertThat(session.getExitTime()).isEqualTo(exitTime);
        assertThat(session.getFinalPrice()).isEqualTo(finalPrice);
        verify(parkingSessionService).findActiveSession(garage, licensePlate);
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(sector.getBasePrice(), session.getPricingMultiplier());
        verify(feeCalculator).calculateFee(session.getEntryTime(), exitTime, effectivePrice);
        verify(parkingSpotService).freeSpot(session);
        verify(sessionRepository).save(session);
    }

    @Test
    void handle_WithNoSpotAssigned_ShouldUseZeroBasePrice() {
        session.setSpot(null);
        BigDecimal effectivePrice = BigDecimal.ZERO;
        BigDecimal finalPrice = BigDecimal.ZERO;

        when(parkingSessionService.findActiveSession(garage, licensePlate)).thenReturn(session);
        when(bigDecimalUtils.zeroWithCurrencyScale()).thenReturn(effectivePrice);
        when(bigDecimalUtils.multiplyAndSetCurrencyScale(effectivePrice, session.getPricingMultiplier()))
                .thenReturn(effectivePrice);
        when(feeCalculator.calculateFee(session.getEntryTime(), exitTime, effectivePrice)).thenReturn(finalPrice);
        when(sessionRepository.save(session)).thenReturn(session);

        exitEventHandler.handle(garage, exitEvent);

        assertThat(session.getExitTime()).isEqualTo(exitTime);
        assertThat(session.getFinalPrice()).isEqualTo(finalPrice);
        verify(parkingSessionService).findActiveSession(garage, licensePlate);
        verify(bigDecimalUtils).zeroWithCurrencyScale();
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(effectivePrice, session.getPricingMultiplier());
        verify(feeCalculator).calculateFee(session.getEntryTime(), exitTime, effectivePrice);
        verify(parkingSpotService).freeSpot(session);
        verify(sessionRepository).save(session);
    }


    @Test
    void handle_WithInvalidEventType_ShouldThrowException() {
        EntryEventDto entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate(licensePlate);

        assertThatThrownBy(() -> exitEventHandler.handle(garage, entryEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ExitEventDto");
    }

    @Test
    void supports_WithExitEvent_ShouldReturnTrue() {
        boolean result = exitEventHandler.supports(exitEvent);

        assertThat(result).isTrue();
    }

    @Test
    void supports_WithNonExitEvent_ShouldReturnFalse() {
        EntryEventDto entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);

        boolean result = exitEventHandler.supports(entryEvent);

        assertThat(result).isFalse();
    }
}
