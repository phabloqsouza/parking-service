package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevenueService Unit Tests")
class RevenueServiceTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private GarageResolver garageResolver;

    @InjectMocks
    private RevenueService revenueService;

    private Garage garage;
    private Sector sector;
    private UUID garageId;
    private UUID sectorId;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();
        sectorId = UUID.randomUUID();

        garage = new Garage();
        garage.setId(garageId);
        garage.setName("Test Garage");

        sector = new Sector();
        sector.setId(sectorId);
        sector.setGarageId(garageId);
        sector.setSectorCode("A");
        sector.setBasePrice(new BigDecimal("10.00"));
        sector.setMaxCapacity(100);
    }

    @Test
    @DisplayName("Should calculate revenue correctly for completed sessions")
    void shouldCalculateRevenueCorrectly() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);

        ParkingSession session1 = new ParkingSession();
        session1.setId(UUID.randomUUID());
        session1.setFinalPrice(new BigDecimal("15.50"));
        session1.setExitTime(startOfDay.plusSeconds(3600)); // 1 hour after start of day

        ParkingSession session2 = new ParkingSession();
        session2.setId(UUID.randomUUID());
        session2.setFinalPrice(new BigDecimal("20.00"));
        session2.setExitTime(startOfDay.plusSeconds(7200)); // 2 hours after start of day

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(garageId, sectorId, startOfDay))
                .thenReturn(Arrays.asList(session1, session2));

        // When
        BigDecimal revenue = revenueService.getRevenue(null, date, sectorCode);

        // Then
        assertEquals(new BigDecimal("35.50"), revenue);
        verify(garageResolver).resolveGarage(null);
        verify(sectorRepository).findByGarageIdAndSectorCode(garageId, sectorCode);
        verify(sessionRepository).findCompletedSessionsByGarageAndSectorAndDate(garageId, sectorId, startOfDay);
    }

    @Test
    @DisplayName("Should return zero when no completed sessions found")
    void shouldReturnZeroWhenNoCompletedSessions() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(garageId, sectorId, startOfDay))
                .thenReturn(List.of());

        // When
        BigDecimal revenue = revenueService.getRevenue(null, date, sectorCode);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(2), revenue);
    }

    @Test
    @DisplayName("Should filter out incomplete sessions")
    void shouldFilterOutIncompleteSessions() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        ParkingSession completedSession = new ParkingSession();
        completedSession.setId(UUID.randomUUID());
        completedSession.setFinalPrice(new BigDecimal("15.50"));
        completedSession.setExitTime(startOfDay.plusSeconds(3600));

        ParkingSession incompleteSession = new ParkingSession();
        incompleteSession.setId(UUID.randomUUID());
        incompleteSession.setFinalPrice(null); // No final price
        incompleteSession.setExitTime(null); // No exit time

        ParkingSession sessionOutsideDateRange = new ParkingSession();
        sessionOutsideDateRange.setId(UUID.randomUUID());
        sessionOutsideDateRange.setFinalPrice(new BigDecimal("10.00"));
        sessionOutsideDateRange.setExitTime(endOfDay.plusSeconds(1)); // After end of day

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(garageId, sectorId, startOfDay))
                .thenReturn(Arrays.asList(completedSession, incompleteSession, sessionOutsideDateRange));

        // When
        BigDecimal revenue = revenueService.getRevenue(null, date, sectorCode);

        // Then
        assertEquals(new BigDecimal("15.50"), revenue);
    }

    @Test
    @DisplayName("Should round revenue to 2 decimal places")
    void shouldRoundRevenueToTwoDecimalPlaces() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);

        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setFinalPrice(new BigDecimal("15.555")); // More than 2 decimals
        session.setExitTime(startOfDay.plusSeconds(3600));

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(garageId, sectorId, startOfDay))
                .thenReturn(List.of(session));

        // When
        BigDecimal revenue = revenueService.getRevenue(null, date, sectorCode);

        // Then
        assertEquals(new BigDecimal("15.56"), revenue); // Rounded up
        assertEquals(2, revenue.scale());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when sector not found")
    void shouldThrowExceptionWhenSectorNotFound() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "Z"; // Non-existent sector

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                revenueService.getRevenue(null, date, sectorCode));

        assertTrue(exception.getMessage().contains("Sector not found"));
        verify(sessionRepository, never()).findCompletedSessionsByGarageAndSectorAndDate(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle multiple sessions with different prices")
    void shouldHandleMultipleSessionsWithDifferentPrices() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<ParkingSession> sessions = Arrays.asList(
                createSessionWithPrice(new BigDecimal("10.50"), startOfDay.plusSeconds(3600)),
                createSessionWithPrice(new BigDecimal("25.75"), startOfDay.plusSeconds(7200)),
                createSessionWithPrice(new BigDecimal("5.25"), startOfDay.plusSeconds(10800))
        );

        when(garageResolver.resolveGarage(null)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode)).thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(garageId, sectorId, startOfDay))
                .thenReturn(sessions);

        // When
        BigDecimal revenue = revenueService.getRevenue(null, date, sectorCode);

        // Then
        assertEquals(new BigDecimal("41.50"), revenue); // 10.50 + 25.75 + 5.25
    }

    private ParkingSession createSessionWithPrice(BigDecimal price, Instant exitTime) {
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setFinalPrice(price);
        session.setExitTime(exitTime);
        return session;
    }
}
