package com.estapar.parking.service;

import com.estapar.parking.exception.*;
import com.estapar.parking.infrastructure.persistence.entity.*;
import com.estapar.parking.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParkingEventService Unit Tests")
class ParkingEventServiceTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private SpotLocationMatcher spotLocationMatcher;

    @Mock
    private GarageResolver garageResolver;

    @InjectMocks
    private ParkingEventService parkingEventService;

    private Garage garage;
    private Sector sector;
    private ParkingSpot parkingSpot;
    private UUID garageId;
    private UUID sectorId;
    private UUID spotId;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();
        sectorId = UUID.randomUUID();
        spotId = UUID.randomUUID();

        garage = new Garage();
        garage.setId(garageId);
        garage.setName("Test Garage");
        garage.setIsDefault(true);

        sector = new Sector();
        sector.setId(sectorId);
        sector.setGarageId(garageId);
        sector.setSectorCode("A");
        sector.setBasePrice(new BigDecimal("10.00"));
        sector.setMaxCapacity(100);
        sector.setOccupiedCount(50);
        sector.setVersion(0);

        parkingSpot = new ParkingSpot();
        parkingSpot.setId(spotId);
        parkingSpot.setSectorId(sectorId);
        parkingSpot.setLatitude(new BigDecimal("-23.561684"));
        parkingSpot.setLongitude(new BigDecimal("-46.655981"));
        parkingSpot.setIsOccupied(false);
        parkingSpot.setVersion(0);
    }

    @Test
    @DisplayName("Should handle ENTRY event successfully")
    void shouldHandleEntryEventSuccessfully() {
        // Given
        String licensePlate = "ABC1234";
        Instant entryTime = Instant.now();
        String sectorCode = "A";

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.empty());
        when(pricingService.calculateOccupancy(50, 100)).thenReturn(new BigDecimal("50.00"));
        when(pricingService.calculateBasePriceWithDynamicPricing(new BigDecimal("10.00"), new BigDecimal("50.00")))
                .thenReturn(new BigDecimal("10.00"));
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);
        when(sessionRepository.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> parkingEventService.handleEntryEvent(null, licensePlate, entryTime, sectorCode));

        // Then
        verify(garageResolver).resolveGarage(null);
        verify(sectorRepository).findByGarageIdAndSectorCode(garageId, sectorCode);
        verify(sessionRepository).findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate);
        verify(pricingService).calculateOccupancy(50, 100);
        verify(pricingService).calculateBasePriceWithDynamicPricing(new BigDecimal("10.00"), new BigDecimal("50.00"));
        verify(sectorRepository).save(any(Sector.class));
        verify(sessionRepository).save(any(ParkingSession.class));
    }

    @Test
    @DisplayName("Should throw SectorFullException when sector is full")
    void shouldThrowSectorFullExceptionWhenSectorIsFull() {
        // Given
        String licensePlate = "ABC1234";
        Instant entryTime = Instant.now();
        String sectorCode = "A";
        sector.setOccupiedCount(100);

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));

        // When/Then
        SectorFullException exception = assertThrows(SectorFullException.class, () ->
                parkingEventService.handleEntryEvent(null, licensePlate, entryTime, sectorCode));

        assertTrue(exception.getMessage().contains("full"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when vehicle already has active session")
    void shouldThrowExceptionWhenVehicleAlreadyHasActiveSession() {
        // Given
        String licensePlate = "ABC1234";
        Instant entryTime = Instant.now();
        String sectorCode = "A";
        ParkingSession existingSession = new ParkingSession();
        existingSession.setId(UUID.randomUUID());
        existingSession.setVehicleLicensePlate(licensePlate);

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.of(existingSession));

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                parkingEventService.handleEntryEvent(null, licensePlate, entryTime, sectorCode));

        assertTrue(exception.getMessage().contains("already has an active parking session"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle PARKED event successfully")
    void shouldHandleParkedEventSuccessfully() {
        // Given
        String licensePlate = "ABC1234";
        BigDecimal lat = new BigDecimal("-23.561684");
        BigDecimal lng = new BigDecimal("-46.655981");
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate(licensePlate);
        session.setSectorId(sectorId);
        session.setEntryTime(Instant.now());

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.of(session));
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
        when(spotRepository.findBySectorId(sectorId)).thenReturn(List.of(parkingSpot));
        when(spotLocationMatcher.findSpotByCoordinates(List.of(parkingSpot), lat, lng)).thenReturn(parkingSpot);
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(parkingSpot);
        when(sessionRepository.save(any(ParkingSession.class))).thenReturn(session);

        // When
        assertDoesNotThrow(() -> parkingEventService.handleParkedEvent(null, licensePlate, lat, lng));

        // Then
        verify(spotLocationMatcher).findSpotByCoordinates(List.of(parkingSpot), lat, lng);
        verify(spotRepository).save(any(ParkingSpot.class));
        verify(sessionRepository).save(any(ParkingSession.class));
    }

    @Test
    @DisplayName("Should handle PARKED event gracefully when spot not found")
    void shouldHandleParkedEventGracefullyWhenSpotNotFound() {
        // Given
        String licensePlate = "ABC1234";
        BigDecimal lat = new BigDecimal("999.000000");
        BigDecimal lng = new BigDecimal("999.000000");
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate(licensePlate);
        session.setSectorId(sectorId);

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.of(session));
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
        when(spotRepository.findBySectorId(sectorId)).thenReturn(List.of(parkingSpot));
        when(spotLocationMatcher.findSpotByCoordinates(List.of(parkingSpot), lat, lng))
                .thenThrow(new SpotNotFoundException("Spot not found"));

        // When
        assertDoesNotThrow(() -> parkingEventService.handleParkedEvent(null, licensePlate, lat, lng));

        // Then
        verify(spotLocationMatcher).findSpotByCoordinates(List.of(parkingSpot), lat, lng);
        verify(spotRepository, never()).save(any(ParkingSpot.class));
        // Session should not be updated when spot not found (graceful degradation)
    }

    @Test
    @DisplayName("Should handle EXIT event successfully")
    void shouldHandleExitEventSuccessfully() {
        // Given
        String licensePlate = "ABC1234";
        Instant entryTime = Instant.now().minusSeconds(3600); // 1 hour ago
        Instant exitTime = Instant.now();
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate(licensePlate);
        session.setSectorId(sectorId);
        session.setSpotId(spotId);
        session.setEntryTime(entryTime);
        session.setBasePrice(new BigDecimal("10.00"));
        session.setVersion(0);

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(parkingSpot));
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
        when(pricingService.calculateFinalPrice(entryTime, exitTime, new BigDecimal("10.00")))
                .thenReturn(new BigDecimal("10.00"));
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(parkingSpot);
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);
        when(sessionRepository.save(any(ParkingSession.class))).thenReturn(session);

        // When
        assertDoesNotThrow(() -> parkingEventService.handleExitEvent(null, licensePlate, exitTime));

        // Then
        verify(spotRepository).findById(spotId);
        verify(spotRepository).save(any(ParkingSpot.class));
        verify(sectorRepository).save(any(Sector.class));
        verify(sessionRepository).save(any(ParkingSession.class));
        verify(pricingService).calculateFinalPrice(entryTime, exitTime, new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should handle EXIT event when spot_id is null")
    void shouldHandleExitEventWhenSpotIdIsNull() {
        // Given
        String licensePlate = "ABC1234";
        Instant entryTime = Instant.now().minusSeconds(3600);
        Instant exitTime = Instant.now();
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate(licensePlate);
        session.setSectorId(sectorId);
        session.setSpotId(null); // No spot assigned
        session.setEntryTime(entryTime);
        session.setBasePrice(new BigDecimal("10.00"));

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.of(session));
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
        when(pricingService.calculateFinalPrice(entryTime, exitTime, new BigDecimal("10.00")))
                .thenReturn(new BigDecimal("10.00"));
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);
        when(sessionRepository.save(any(ParkingSession.class))).thenReturn(session);

        // When
        assertDoesNotThrow(() -> parkingEventService.handleExitEvent(null, licensePlate, exitTime));

        // Then
        verify(spotRepository, never()).findById(any());
        verify(spotRepository, never()).save(any());
        verify(sectorRepository).save(any(Sector.class));
        verify(sessionRepository).save(any(ParkingSession.class));
    }

    @Test
    @DisplayName("Should throw ParkingSessionNotFoundException when no active session found for EXIT")
    void shouldThrowExceptionWhenNoActiveSessionFoundForExit() {
        // Given
        String licensePlate = "ABC1234";
        Instant exitTime = Instant.now();

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.empty());

        // When/Then
        ParkingSessionNotFoundException exception = assertThrows(ParkingSessionNotFoundException.class, () ->
                parkingEventService.handleExitEvent(null, licensePlate, exitTime));

        assertTrue(exception.getMessage().contains("No active parking session found"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should ignore duplicate PARKED events (idempotent)")
    void shouldIgnoreDuplicateParkedEvents() {
        // Given
        String licensePlate = "ABC1234";
        BigDecimal lat = new BigDecimal("-23.561684");
        BigDecimal lng = new BigDecimal("-46.655981");
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate(licensePlate);
        session.setSectorId(sectorId);
        session.setSpotId(spotId); // Already has spot assigned

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garageId, licensePlate))
                .thenReturn(Optional.of(session));

        // When
        assertDoesNotThrow(() -> parkingEventService.handleParkedEvent(null, licensePlate, lat, lng));

        // Then
        verify(spotLocationMatcher, never()).findSpotByCoordinates(any(), any(), any());
        verify(spotRepository, never()).save(any());
        verify(sessionRepository, never()).save(any(ParkingSession.class));
    }
}
