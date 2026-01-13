package com.estapar.parking.service;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.util.BigDecimalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private ParkingFeeCalculator feeCalculator;

    @Mock
    private PricingStrategyResolver strategyResolver;

    @Mock
    private BigDecimalUtils bigDecimalUtils;

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private GarageRepository garageRepository;

    @Mock
    private GarageResolver garageResolver;

    @Mock
    private ParkingMapper parkingMapper;

    @InjectMocks
    private PricingService pricingService;

    private Garage garage;
    private Sector sector;
    private PricingStrategy pricingStrategy;
    private ParkingSession session;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());
        garage.setMaxCapacity(100);

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setGarage(garage);
        sector.setBasePrice(new BigDecimal("10.00"));
        sector.setMaxCapacity(50);

        pricingStrategy = new PricingStrategy();
        pricingStrategy.setMultiplier(new BigDecimal("1.5"));

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setEntryTime(Instant.parse("2025-01-01T10:00:00.000Z"));
        session.setExitTime(Instant.parse("2025-01-01T12:00:00.000Z"));
        session.setBasePrice(new BigDecimal("10.00"));
    }

    @Test
    void calculateDynamicPrice_ShouldCalculateCorrectly() {
        long occupied = 50L;
        BigDecimal occupancyPercentage = new BigDecimal("50.00");
        BigDecimal expectedPrice = new BigDecimal("15.00");

        when(garageRepository.calcOccupancy(garage.getId())).thenReturn(occupied);
        when(bigDecimalUtils.calculatePercentage(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(occupancyPercentage);
        when(strategyResolver.findStrategy(occupancyPercentage)).thenReturn(pricingStrategy);
        when(bigDecimalUtils.multiplyAndSetCurrencyScale(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(expectedPrice);

        BigDecimal result = pricingService.calculateDynamicPrice(sector);

        assertThat(result).isEqualByComparingTo(expectedPrice);
        verify(garageRepository).calcOccupancy(garage.getId());
        verify(bigDecimalUtils).calculatePercentage(any(BigDecimal.class), any(BigDecimal.class));
        verify(strategyResolver).findStrategy(occupancyPercentage);
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(sector.getBasePrice(), pricingStrategy.getMultiplier());
    }

    @Test
    void calculateFee_ShouldDelegateToFeeCalculator() {
        BigDecimal expectedFee = new BigDecimal("20.00");

        when(feeCalculator.calculateFee(session.getEntryTime(), session.getExitTime(), session.getBasePrice()))
                .thenReturn(expectedFee);

        BigDecimal result = pricingService.calculateFee(session);

        assertThat(result).isEqualByComparingTo(expectedFee);
        verify(feeCalculator).calculateFee(session.getEntryTime(), session.getExitTime(), session.getBasePrice());
    }

    @Test
    void getRevenue_WithRevenue_ShouldReturnRevenueResponse() {
        UUID garageId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        BigDecimal totalRevenue = new BigDecimal("150.50");
        BigDecimal scaledRevenue = new BigDecimal("150.50");
        RevenueResponseDto expectedResponse = new RevenueResponseDto(
                scaledRevenue, "BRL", Instant.now());

        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.of(sector));
        when(sessionRepository.sumRevenueByGarageAndSectorAndDate(
                garage.getId(), sector.getId(), date.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .thenReturn(totalRevenue);
        when(bigDecimalUtils.setCurrencyScale(totalRevenue)).thenReturn(scaledRevenue);
        when(parkingMapper.toRevenueResponseDto(scaledRevenue)).thenReturn(expectedResponse);

        RevenueResponseDto result = pricingService.getRevenue(garageId, date, sectorCode);

        assertThat(result).isEqualTo(expectedResponse);
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
        verify(sessionRepository).sumRevenueByGarageAndSectorAndDate(
                garage.getId(), sector.getId(), date.atStartOfDay().toInstant(ZoneOffset.UTC));
        verify(bigDecimalUtils).setCurrencyScale(totalRevenue);
        verify(parkingMapper).toRevenueResponseDto(scaledRevenue);
    }

    @Test
    void getRevenue_WithNullRevenue_ShouldReturnZero() {
        UUID garageId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "A";
        BigDecimal zeroRevenue = BigDecimal.ZERO.setScale(2);
        RevenueResponseDto expectedResponse = new RevenueResponseDto(
                zeroRevenue, "BRL", Instant.now());

        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.of(sector));
        when(sessionRepository.sumRevenueByGarageAndSectorAndDate(
                garage.getId(), sector.getId(), date.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .thenReturn(null);
        when(bigDecimalUtils.zeroWithCurrencyScale()).thenReturn(zeroRevenue);
        when(bigDecimalUtils.setCurrencyScale(zeroRevenue)).thenReturn(zeroRevenue);
        when(parkingMapper.toRevenueResponseDto(zeroRevenue)).thenReturn(expectedResponse);

        RevenueResponseDto result = pricingService.getRevenue(garageId, date, sectorCode);

        assertThat(result).isEqualTo(expectedResponse);
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
        verify(sessionRepository).sumRevenueByGarageAndSectorAndDate(
                garage.getId(), sector.getId(), date.atStartOfDay().toInstant(ZoneOffset.UTC));
        verify(bigDecimalUtils).zeroWithCurrencyScale();
        verify(bigDecimalUtils).setCurrencyScale(zeroRevenue);
        verify(parkingMapper).toRevenueResponseDto(zeroRevenue);
    }

    @Test
    void getRevenue_WithSectorNotFound_ShouldThrowException() {
        UUID garageId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 15);
        String sectorCode = "INVALID";

        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingService.getRevenue(garageId, date, sectorCode))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
    }
}
