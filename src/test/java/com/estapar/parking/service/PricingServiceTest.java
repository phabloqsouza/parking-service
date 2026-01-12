package com.estapar.parking.service;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.PricingStrategy;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.util.BigDecimalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
import static org.mockito.Mockito.*;

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
    private GarageResolver garageResolver;

    @Mock
    private ParkingMapper parkingMapper;

    @InjectMocks
    private PricingService pricingService;

    private UUID garageId;
    private Garage garage;
    private Sector sector;
    private PricingStrategy pricingStrategy;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();
        garage = new Garage();
        garage.setId(garageId);
        
        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setSectorCode("A");
        sector.setBasePrice(new BigDecimal("10.00"));
        sector.setOccupiedCount(25);
        sector.setMaxCapacity(100);
        
        pricingStrategy = new PricingStrategy();
        pricingStrategy.setMultiplier(new BigDecimal("0.90"));
    }

    @Test
    void applyDynamicPricing_ShouldCalculateAndApplyMultiplier() {
        when(bigDecimalUtils.calculatePercentage(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("25.00"));
        when(strategyResolver.findStrategy(eq(garageId), any(BigDecimal.class)))
                .thenReturn(pricingStrategy);
        when(bigDecimalUtils.multiplyAndSetCurrencyScale(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("9.00"));

        BigDecimal result = pricingService.applyDynamicPricing(garageId, sector);

        assertThat(result).isNotNull();
        verify(bigDecimalUtils).calculatePercentage(eq(BigDecimal.valueOf(25)), eq(BigDecimal.valueOf(100)));
        verify(strategyResolver).findStrategy(eq(garageId), eq(new BigDecimal("25.00")));
        verify(bigDecimalUtils).multiplyAndSetCurrencyScale(eq(sector.getBasePrice()), eq(pricingStrategy.getMultiplier()));
    }

    @Test
    void calculateFee_ShouldDelegateToFeeCalculator() {
        Instant entryTime = Instant.now();
        Instant exitTime = entryTime.plusSeconds(3600);
        BigDecimal basePrice = new BigDecimal("10.00");
        BigDecimal expectedFee = new BigDecimal("10.00");
        
        when(feeCalculator.calculateFee(entryTime, exitTime, basePrice)).thenReturn(expectedFee);

        BigDecimal result = pricingService.calculateFee(entryTime, exitTime, basePrice);

        assertThat(result).isEqualByComparingTo(expectedFee);
        verify(feeCalculator).calculateFee(entryTime, exitTime, basePrice);
    }

    @Test
    void getRevenue_WithData_ShouldReturnRevenueResponse() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        String sectorCode = "A";
        BigDecimal totalRevenue = new BigDecimal("100.50");
        
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.of(sector));
        when(sessionRepository.sumRevenueByGarageAndSectorAndDate(eq(garage.getId()), eq(sector.getId()), any(Instant.class)))
                .thenReturn(totalRevenue);
        when(bigDecimalUtils.setCurrencyScale(totalRevenue)).thenReturn(totalRevenue);
        
        RevenueResponseDto responseDto = new RevenueResponseDto();
        responseDto.setAmount(totalRevenue);
        when(parkingMapper.toRevenueResponseDto(totalRevenue)).thenReturn(responseDto);

        RevenueResponseDto result = pricingService.getRevenue(garageId, date, sectorCode);

        assertThat(result).isNotNull();
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
        verify(sessionRepository).sumRevenueByGarageAndSectorAndDate(eq(garage.getId()), eq(sector.getId()), any(Instant.class));
    }

    @Test
    void getRevenue_WithNoData_ShouldReturnZeroRevenue() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        String sectorCode = "A";
        BigDecimal zeroRevenue = BigDecimal.ZERO;
        
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.of(sector));
        when(sessionRepository.sumRevenueByGarageAndSectorAndDate(eq(garage.getId()), eq(sector.getId()), any(Instant.class)))
                .thenReturn(null);
        when(bigDecimalUtils.setCurrencyScale(zeroRevenue)).thenReturn(zeroRevenue);
        
        RevenueResponseDto responseDto = new RevenueResponseDto();
        responseDto.setAmount(zeroRevenue);
        when(parkingMapper.toRevenueResponseDto(zeroRevenue)).thenReturn(responseDto);

        RevenueResponseDto result = pricingService.getRevenue(garageId, date, sectorCode);

        assertThat(result).isNotNull();
        verify(parkingMapper).toRevenueResponseDto(zeroRevenue);
    }

    @Test
    void getRevenue_WithNonExistentSector_ShouldThrowException() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        String sectorCode = "INVALID";
        
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingService.getRevenue(garageId, date, sectorCode))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).contains("Sector not found");
                });
    }
}
