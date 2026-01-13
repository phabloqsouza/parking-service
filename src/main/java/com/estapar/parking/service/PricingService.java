package com.estapar.parking.service;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.util.BigDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.estapar.parking.api.exception.ErrorMessages.SECTOR_NOT_FOUND;
import static com.estapar.parking.api.exception.ErrorMessages.notFound;
import static java.math.BigDecimal.valueOf;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final ParkingFeeCalculator feeCalculator;
    private final PricingStrategyResolver strategyResolver;
    private final BigDecimalUtils bigDecimalUtils;
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final GarageRepository garageRepository;
    private final GarageResolver garageResolver;
    private final ParkingMapper parkingMapper;

    public BigDecimal calculateDynamicPrice(Sector sector) {
        long occupied = garageRepository.calcOccupancy(sector.getGarage().getId());

        // Use 4 decimal places for intermediate calculation, then scale to percentage scale
        BigDecimal occupancyPercentage = bigDecimalUtils
                .calculatePercentage(valueOf(occupied), valueOf(sector.getGarage().getMaxCapacity()));

        // Apply pricing strategy based on garage occupancy
        BigDecimal multiplier = strategyResolver.findStrategy(occupancyPercentage).getMultiplier();

        return bigDecimalUtils.multiplyAndSetCurrencyScale(sector.getBasePrice(), multiplier);
    }

    public BigDecimal calculateFee(ParkingSession session) {
        return feeCalculator.calculateFee(session.getEntryTime(), session.getExitTime(), session.getBasePrice());
    }

    @Transactional(readOnly = true)
    public RevenueResponseDto getRevenue(UUID garageId, LocalDate date, String sectorCode) {
        Garage garage = garageResolver.getGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> notFound(SECTOR_NOT_FOUND, sectorCode));
        
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        
        // Sum revenue directly in database query (no need to fetch all records)
        BigDecimal totalRevenue = sessionRepository
                .sumRevenueByGarageAndSectorAndDate(garage.getId(), sector.getId(), startOfDay);
        
        if (totalRevenue == null) {
            totalRevenue = bigDecimalUtils.zeroWithCurrencyScale();
        }
        
        return parkingMapper.toRevenueResponseDto(bigDecimalUtils.setCurrencyScale(totalRevenue));
    }
}
