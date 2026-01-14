package com.estapar.parking.service;

import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

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

    private Garage garage;
    private Sector sector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pricingService, "applicationTimezone", "America/Sao_Paulo");
        
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setGarage(garage);
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
        Instant startOfDay = date.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant();

        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.of(sector));
        when(sessionRepository.sumRevenueByGarageAndSectorAndDate(
                eq(garage.getId()), eq(sector.getId()), eq(startOfDay)))
                .thenReturn(totalRevenue);
        when(bigDecimalUtils.setCurrencyScale(totalRevenue)).thenReturn(scaledRevenue);
        when(parkingMapper.toRevenueResponseDto(scaledRevenue)).thenReturn(expectedResponse);

        RevenueResponseDto result = pricingService.getRevenue(garageId, date, sectorCode);

        assertThat(result).isEqualTo(expectedResponse);
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
        verify(sessionRepository).sumRevenueByGarageAndSectorAndDate(
                eq(garage.getId()), eq(sector.getId()), eq(startOfDay));
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
        Instant startOfDay = date.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant();

        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode))
                .thenReturn(Optional.of(sector));
        when(sessionRepository.sumRevenueByGarageAndSectorAndDate(
                eq(garage.getId()), eq(sector.getId()), eq(startOfDay)))
                .thenReturn(null);
        when(bigDecimalUtils.zeroWithCurrencyScale()).thenReturn(zeroRevenue);
        when(bigDecimalUtils.setCurrencyScale(zeroRevenue)).thenReturn(zeroRevenue);
        when(parkingMapper.toRevenueResponseDto(zeroRevenue)).thenReturn(expectedResponse);

        RevenueResponseDto result = pricingService.getRevenue(garageId, date, sectorCode);

        assertThat(result).isEqualTo(expectedResponse);
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
        verify(sessionRepository).sumRevenueByGarageAndSectorAndDate(
                eq(garage.getId()), eq(sector.getId()), eq(startOfDay));
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
                .isInstanceOf(ResponseStatusException.class);
        verify(garageResolver).getGarage(garageId);
        verify(sectorRepository).findByGarageIdAndSectorCode(garage.getId(), sectorCode);
    }
}
