package com.estapar.parking.service;

import com.estapar.parking.config.DecimalConfig;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevenueService {
    
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final GarageResolver garageResolver;
    private final DecimalConfig decimalConfig;
    
    @Transactional(readOnly = true)
    public BigDecimal getRevenue(UUID garageId, LocalDate date, String sectorCode) {
        Garage garage = garageResolver.getGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + sectorCode));
        
        // Convert LocalDate to Instant (start and end of day in UTC)
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        
        // Sum revenue directly in database query (no need to fetch all records)
        BigDecimal totalRevenue = sessionRepository
                .sumRevenueByGarageAndSectorAndDate(garage.getId(), sector.getId(), startOfDay);
        
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        
        return totalRevenue.setScale(decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
    }
}
