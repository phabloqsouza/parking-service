package com.estapar.parking.service;

import com.estapar.parking.config.DecimalConfig;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {
    
    @Mock
    private ParkingSessionRepository sessionRepository;
    
    @Mock
    private SectorRepository sectorRepository;
    
    @Mock
    private GarageResolver garageResolver;
    
    private DecimalConfig decimalConfig;
    private RevenueService revenueService;
    
    @BeforeEach
    void setUp() {
        decimalConfig = new DecimalConfig();
        decimalConfig.setCurrencyScale(2);
        decimalConfig.setRoundingMode(RoundingMode.HALF_UP);
        revenueService = new RevenueService(sessionRepository, sectorRepository, garageResolver, decimalConfig);
    }
    
    @Test
    void getRevenue_WithCompletedSessions_ShouldReturnTotal() {
        UUID garageId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 1);
        String sectorCode = "A";
        
        Garage garage = createGarage(garageId);
        Sector sector = createSector(garageId, sectorCode);
        
        ParkingSession session1 = createCompletedSession(garageId, sector.getId(), new BigDecimal("10.00"));
        ParkingSession session2 = createCompletedSession(garageId, sector.getId(), new BigDecimal("15.50"));
        
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode))
            .thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(any(), any(), any()))
            .thenReturn(List.of(session1, session2));
        
        BigDecimal revenue = revenueService.getRevenue(garageId, date, sectorCode);
        
        assertEquals(new BigDecimal("25.50"), revenue);
    }
    
    @Test
    void getRevenue_WithNoSessions_ShouldReturnZero() {
        UUID garageId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 1);
        String sectorCode = "A";
        
        Garage garage = createGarage(garageId);
        Sector sector = createSector(garageId, sectorCode);
        
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode))
            .thenReturn(Optional.of(sector));
        when(sessionRepository.findCompletedSessionsByGarageAndSectorAndDate(any(), any(), any()))
            .thenReturn(List.of());
        
        BigDecimal revenue = revenueService.getRevenue(garageId, date, sectorCode);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), revenue);
    }
    
    @Test
    void getRevenue_WithNonExistentSector_ShouldThrowException() {
        UUID garageId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 1);
        String sectorCode = "X";
        
        Garage garage = createGarage(garageId);
        
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, sectorCode))
            .thenReturn(Optional.empty());
        
        assertThrows(IllegalStateException.class, () -> {
            revenueService.getRevenue(garageId, date, sectorCode);
        });
    }
    
    private Garage createGarage(UUID id) {
        Garage garage = new Garage();
        garage.setId(id);
        garage.setName("Test Garage");
        garage.setIsDefault(true);
        garage.setCreatedAt(Instant.now());
        return garage;
    }
    
    private Sector createSector(UUID garageId, String sectorCode) {
        Sector sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setGarageId(garageId);
        sector.setSectorCode(sectorCode);
        sector.setBasePrice(new BigDecimal("10.00"));
        sector.setMaxCapacity(100);
        sector.setOccupiedCount(0);
        return sector;
    }
    
    private ParkingSession createCompletedSession(UUID garageId, UUID sectorId, BigDecimal finalPrice) {
        ParkingSession session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setGarageId(garageId);
        session.setSectorId(sectorId);
        session.setVehicleLicensePlate("ABC1234");
        session.setEntryTime(Instant.parse("2025-01-01T10:00:00Z"));
        session.setExitTime(Instant.parse("2025-01-01T11:00:00Z"));
        session.setBasePrice(new BigDecimal("10.00"));
        session.setFinalPrice(finalPrice);
        session.setCreatedAt(Instant.now());
        return session;
    }
}
