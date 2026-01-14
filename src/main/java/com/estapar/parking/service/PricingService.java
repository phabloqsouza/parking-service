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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.estapar.parking.api.exception.ErrorMessages.SECTOR_NOT_FOUND;
import static com.estapar.parking.api.exception.ErrorMessages.notFound;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final BigDecimalUtils bigDecimalUtils;
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final GarageResolver garageResolver;
    private final ParkingMapper parkingMapper;

    @Transactional(readOnly = true)
    public RevenueResponseDto getRevenue(UUID garageId, LocalDate date, String sectorCode) {
        Garage garage = garageResolver.getGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> notFound(SECTOR_NOT_FOUND, sectorCode));
        
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        
        BigDecimal totalRevenue = sessionRepository
                .sumRevenueByGarageAndSectorAndDate(garage.getId(), sector.getId(), startOfDay);
        
        if (totalRevenue == null) {
            totalRevenue = bigDecimalUtils.zeroWithCurrencyScale();
        }
        
        return parkingMapper.toRevenueResponseDto(bigDecimalUtils.setCurrencyScale(totalRevenue));
    }
}
