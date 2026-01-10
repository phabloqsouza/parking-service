package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class RevenueService {
    
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final GarageResolver garageResolver;
    
    public RevenueService(ParkingSessionRepository sessionRepository,
                         SectorRepository sectorRepository,
                         GarageResolver garageResolver) {
        this.sessionRepository = sessionRepository;
        this.sectorRepository = sectorRepository;
        this.garageResolver = garageResolver;
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getRevenue(UUID garageId, LocalDate date, String sectorCode) {
        Garage garage = garageResolver.resolveGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + sectorCode));
        
        // Convert LocalDate to Instant (start and end of day in UTC)
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        
        // Find completed sessions for the date and sector
        List<ParkingSession> sessions = sessionRepository
                .findCompletedSessionsByGarageAndSectorAndDate(garage.getId(), sector.getId(), startOfDay);
        
        // Sum final prices (only completed sessions with exit_time and final_price)
        BigDecimal totalRevenue = sessions.stream()
                .filter(session -> session.getExitTime() != null 
                        && session.getExitTime().isAfter(startOfDay) 
                        && session.getExitTime().isBefore(endOfDay)
                        && session.getFinalPrice() != null)
                .map(ParkingSession::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalRevenue.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
