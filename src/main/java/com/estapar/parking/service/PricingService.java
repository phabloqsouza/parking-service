package com.estapar.parking.service;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.util.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final ParkingFeeCalculator feeCalculator;
    private final PricingStrategyResolver strategyResolver;
    private final BigDecimalUtils bigDecimalUtils;
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final GarageResolver garageResolver;
    private final ParkingMapper parkingMapper;

    public BigDecimal applyDynamicPricing(UUID garageId, Sector sector) {
        BigDecimal occupied = BigDecimal.valueOf(sector.getOccupiedCount());
        BigDecimal max = BigDecimal.valueOf(sector.getMaxCapacity());
        // Use 4 decimal places for intermediate calculation, then scale to percentage scale
        BigDecimal occupancyPercentage = bigDecimalUtils.calculatePercentage(occupied, max);

        // Apply pricing strategy based on occupancy
        BigDecimal multiplier = strategyResolver.findStrategy(garageId, occupancyPercentage).getMultiplier();

        return bigDecimalUtils.multiplyAndSetCurrencyScale(sector.getBasePrice(), multiplier);
    }

    public BigDecimal calculateFee(Instant entryTime, Instant exitTime, BigDecimal basePriceWithDynamicPricing) {
        return feeCalculator.calculateFee(entryTime, exitTime, basePriceWithDynamicPricing);
    }

    @Transactional(readOnly = true)
    public RevenueResponseDto getRevenue(UUID garageId, LocalDate date, String sectorCode) {
        Garage garage = garageResolver.getGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found: " + sectorCode));
        
        // Convert LocalDate to Instant (start and end of day in UTC)
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        
        // Sum revenue directly in database query (no need to fetch all records)
        BigDecimal totalRevenue = sessionRepository
                .sumRevenueByGarageAndSectorAndDate(garage.getId(), sector.getId(), startOfDay);
        
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        
        return parkingMapper.toRevenueResponseDto(bigDecimalUtils.setCurrencyScale(totalRevenue));
    }
}
