package com.estapar.parking.service.event;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.service.ParkingSessionService;
import com.estapar.parking.service.PricingStrategyResolver;
import com.estapar.parking.util.BigDecimalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private PricingStrategyResolver pricingStrategyResolver;

    @Mock
    private BigDecimalUtils bigDecimalUtils;

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
    void handle_WithAvailableCapacity_ShouldCreateSessionAndStoreMultiplier() {
        long occupied = 50L;
        BigDecimal occupancyPercentage = new BigDecimal("50.00");
        PricingStrategy strategy = new PricingStrategy();
        BigDecimal multiplier = new BigDecimal("1.00");
        strategy.setMultiplier(multiplier);

        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(occupied);
        when(parkingSessionService.existsActiveSession(garage, licensePlate)).thenReturn(false);
        when(bigDecimalUtils.calculatePercentage(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(occupancyPercentage);
        when(pricingStrategyResolver.findStrategy(occupancyPercentage)).thenReturn(strategy);
        when(parkingMapper.toParkingSession(entryEvent.getLicensePlate(), entryEvent.getEntryTime(), garage, multiplier))
                .thenAnswer(invocation -> {
                    session.setPricingMultiplier(multiplier);
                    return session;
                });
        when(sessionRepository.save(session)).thenReturn(session);

        entryEventHandler.handle(garage, entryEvent);

        verify(garageRepository, times(2)).calcOccupancy(garage.getId());
        verify(parkingSessionService).existsActiveSession(garage, licensePlate);
        verify(bigDecimalUtils).calculatePercentage(any(BigDecimal.class), any(BigDecimal.class));
        verify(pricingStrategyResolver).findStrategy(occupancyPercentage);
        verify(parkingMapper).toParkingSession(entryEvent.getLicensePlate(), entryEvent.getEntryTime(), garage, multiplier);
        verify(sessionRepository).save(session);
        assertThat(session.getPricingMultiplier()).isEqualTo(multiplier);
    }

    @Test
    void handle_WithGarageFull_ShouldThrowException() {
        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(100L);

        assertThatThrownBy(() -> entryEventHandler.handle(garage, entryEvent))
                .isInstanceOf(ResponseStatusException.class);
        verify(garageRepository).calcOccupancy(garage.getId());
        verify(parkingSessionService, never()).existsActiveSession(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_WithActiveSessionExists_ShouldThrowException() {
        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(50L);
        when(parkingSessionService.existsActiveSession(garage, licensePlate)).thenReturn(true);

        assertThatThrownBy(() -> entryEventHandler.handle(garage, entryEvent))
                .isInstanceOf(ResponseStatusException.class);
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
