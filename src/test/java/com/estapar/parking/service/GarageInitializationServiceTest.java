package com.estapar.parking.service;

import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.infrastructure.external.GarageSimulatorFeignClient;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GarageInitializationServiceTest {

    @Mock
    private GarageSimulatorFeignClient simulatorClient;

    @Mock
    private GarageRepository garageRepository;

    @Mock
    private ParkingMapper parkingMapper;

    @InjectMocks
    private GarageInitializationService garageInitializationService;

    private GarageSimulatorResponseDto config;
    private Garage garage;

    @BeforeEach
    void setUp() {
        config = new GarageSimulatorResponseDto(
                List.of(new GarageSimulatorResponseDto.SectorConfigDto("A", BigDecimal.TEN, 100)),
                List.of(new GarageSimulatorResponseDto.SpotConfigDto(1, "A", BigDecimal.valueOf(-23.561684), BigDecimal.valueOf(-46.655981)))
        );
        garage = new Garage();
        garage.setId(UUID.randomUUID());
        garage.setIsDefault(true);
    }

    @Test
    void initializeFromSimulator_WhenDefaultGarageExists_ShouldSkipInitialization() {
        when(garageRepository.existsByIsDefaultTrue()).thenReturn(true);

        garageInitializationService.initializeFromSimulator();

        verify(garageRepository).existsByIsDefaultTrue();
        verify(simulatorClient, never()).getGarageConfiguration();
        verify(parkingMapper, never()).toGarage(any());
        verify(garageRepository, never()).save(any());
    }

    @Test
    void initializeFromSimulator_WhenNoDefaultGarage_ShouldCreateNewGarage() {
        when(garageRepository.existsByIsDefaultTrue()).thenReturn(false);
        when(simulatorClient.getGarageConfiguration()).thenReturn(config);
        when(parkingMapper.toGarage(config)).thenReturn(garage);
        when(garageRepository.save(garage)).thenReturn(garage);

        garageInitializationService.initializeFromSimulator();

        verify(garageRepository).existsByIsDefaultTrue();
        verify(simulatorClient).getGarageConfiguration();
        verify(parkingMapper).toGarage(config);
        verify(garageRepository).save(garage);
    }

    @Test
    void initializeFromSimulator_WithNullConfig_ShouldThrowException() {
        when(garageRepository.existsByIsDefaultTrue()).thenReturn(false);
        when(simulatorClient.getGarageConfiguration()).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, 
                () -> garageInitializationService.initializeFromSimulator());

        verify(garageRepository).existsByIsDefaultTrue();
        verify(simulatorClient).getGarageConfiguration();
        verify(parkingMapper, never()).toGarage(any());
        verify(garageRepository, never()).save(any());
    }
}
