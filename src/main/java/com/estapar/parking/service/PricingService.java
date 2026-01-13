package com.estapar.parking.service;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.util.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.estapar.parking.api.exception.ErrorMessages.SECTOR_NOT_FOUND;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    public BigDecimal applyDynamicPricing(Sector sector) {
        BigDecimal occupied = valueOf(sector.getOccupiedCount());
        BigDecimal max = valueOf(sector.getMaxCapacity());
        // Use 4 decimal places for intermediate calculation, then scale to percentage scale
        BigDecimal occupancyPercentage = bigDecimalUtils.calculatePercentage(occupied, max);

        // Apply pricing strategy based on occupancy
        BigDecimal multiplier = strategyResolver.findStrategy(occupancyPercentage).getMultiplier();

        return bigDecimalUtils.multiplyAndSetCurrencyScale(sector.getBasePrice(), multiplier);
    }
    
    public BigDecimal applyDynamicPricing(Sector sector, Long availableCapacityAtEntry, Integer garageMaxCapacity) {
        if (availableCapacityAtEntry == null || garageMaxCapacity == null) {
            // Fallback to current capacity if entry-time capacity is not available
            return applyDynamicPricing(sector);
        }
        
        // Calculate total occupied at entry time
        long totalOccupiedAtEntry = garageMaxCapacity - availableCapacityAtEntry;
        
        // Calculate sector's occupied count at entry time
        // We approximate by using the proportion of sector capacity to garage capacity
        BigDecimal garageMax = valueOf(garageMaxCapacity);
        BigDecimal sectorMax = valueOf(sector.getMaxCapacity());
        // Use higher precision for proportion calculation (4 decimal places)
        BigDecimal sectorProportion = bigDecimalUtils.divide(sectorMax, garageMax, 4);
        
        // Estimate sector occupied count at entry time
        BigDecimal sectorOccupiedAtEntry = bigDecimalUtils.setScale(
            bigDecimalUtils.multiplyAndSetCurrencyScale(sectorProportion, valueOf(totalOccupiedAtEntry)),
            0);
        
        // Use the estimated occupied count at entry time for pricing
        BigDecimal occupancyPercentage = bigDecimalUtils.calculatePercentage(
            sectorOccupiedAtEntry, sectorMax);

        // Apply pricing strategy based on occupancy at entry time
        BigDecimal multiplier = strategyResolver.findStrategy(occupancyPercentage).getMultiplier();

        return bigDecimalUtils.multiplyAndSetCurrencyScale(sector.getBasePrice(), multiplier);
    }

    public BigDecimal calculateFee(Instant entryTime, Instant exitTime, BigDecimal basePriceWithDynamicPricing) {
        return feeCalculator.calculateFee(entryTime, exitTime, basePriceWithDynamicPricing);
    }

    @Transactional(readOnly = true)
    public RevenueResponseDto getRevenue(UUID garageId, LocalDate date, String sectorCode) {
        Garage garage = garageResolver.getGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, 
                    String.format(SECTOR_NOT_FOUND, sectorCode)));
        
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        
        // Sum revenue directly in database query (no need to fetch all records)
        BigDecimal totalRevenue = sessionRepository
                .sumRevenueByGarageAndSectorAndDate(garage.getId(), sector.getId(), startOfDay);
        
        if (totalRevenue == null) {
            totalRevenue = ZERO;
        }
        
        return parkingMapper.toRevenueResponseDto(bigDecimalUtils.setCurrencyScale(totalRevenue));
    }
}
