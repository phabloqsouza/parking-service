package com.estapar.parking.service;

import com.estapar.parking.infrastructure.external.GarageSimulatorFeignClient;
import com.estapar.parking.infrastructure.external.dto.GarageSimulatorResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GarageInitializationService Unit Tests")
class GarageInitializationServiceTest {

    @Mock
    private GarageSimulatorFeignClient simulatorClient;

    @Mock
    private GarageRepository garageRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @InjectMocks
    private GarageInitializationService initializationService;

    private GarageSimulatorResponseDto simulatorResponse;
    private Garage existingGarage;
    private Sector existingSector;
    private UUID garageId;
    private UUID sectorId;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();
        sectorId = UUID.randomUUID();

        existingGarage = new Garage();
        existingGarage.setId(garageId);
        existingGarage.setName("Default Garage");
        existingGarage.setIsDefault(true);
        existingGarage.setCreatedAt(Instant.now());

        existingSector = new Sector();
        existingSector.setId(sectorId);
        existingSector.setGarageId(garageId);
        existingSector.setSectorCode("A");
        existingSector.setBasePrice(new BigDecimal("10.00"));
        existingSector.setMaxCapacity(100);

        // Create simulator response
        GarageSimulatorResponseDto.SectorConfigDto sectorConfig = 
            new GarageSimulatorResponseDto.SectorConfigDto("A", new BigDecimal("10.00"), 100);
        GarageSimulatorResponseDto.SpotConfigDto spotConfig = 
            new GarageSimulatorResponseDto.SpotConfigDto(1, "A", new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        
        simulatorResponse = new GarageSimulatorResponseDto(
            List.of(sectorConfig),
            List.of(spotConfig)
        );
    }

    @Test
    @DisplayName("Should initialize garage successfully when default garage exists")
    void shouldInitializeGarageSuccessfullyWhenDefaultGarageExists() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(simulatorResponse);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.of(existingSector));
        when(sectorRepository.findByGarageId(garageId)).thenReturn(List.of(existingSector));
        when(spotRepository.findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sectorRepository.save(any(Sector.class))).thenReturn(existingSector);
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> initializationService.initializeFromSimulator());

        // Then
        verify(garageRepository).findByIsDefaultTrue();
        verify(simulatorClient).getGarageConfiguration();
        verify(sectorRepository).findByGarageIdAndSectorCode(garageId, "A");
        verify(spotRepository).save(any(ParkingSpot.class));
    }

    @Test
    @DisplayName("Should create default garage when it does not exist")
    void shouldCreateDefaultGarageWhenItDoesNotExist() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        when(simulatorClient.getGarageConfiguration()).thenReturn(simulatorResponse);
        when(garageRepository.save(any(Garage.class))).thenAnswer(invocation -> {
            Garage garage = invocation.getArgument(0);
            garage.setId(garageId);
            return garage;
        });
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.empty());
        when(sectorRepository.findByGarageId(garageId)).thenReturn(Collections.emptyList());
        when(spotRepository.findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> {
            Sector sector = invocation.getArgument(0);
            sector.setId(sectorId);
            return sector;
        });
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> initializationService.initializeFromSimulator());

        // Then
        ArgumentCaptor<Garage> garageCaptor = ArgumentCaptor.forClass(Garage.class);
        verify(garageRepository).save(garageCaptor.capture());
        Garage savedGarage = garageCaptor.getValue();
        assertTrue(savedGarage.getIsDefault());
        assertEquals("Default Garage", savedGarage.getName());
    }

    @Test
    @DisplayName("Should create new sector when it does not exist")
    void shouldCreateNewSectorWhenItDoesNotExist() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(simulatorResponse);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.empty());
        when(sectorRepository.findByGarageId(garageId)).thenReturn(Collections.emptyList());
        when(spotRepository.findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> {
            Sector sector = invocation.getArgument(0);
            sector.setId(sectorId);
            return sector;
        });
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> initializationService.initializeFromSimulator());

        // Then
        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository, atLeastOnce()).save(sectorCaptor.capture());
        Sector savedSector = sectorCaptor.getValue();
        assertEquals("A", savedSector.getSectorCode());
        assertEquals(new BigDecimal("10.00"), savedSector.getBasePrice());
        assertEquals(100, savedSector.getMaxCapacity());
    }

    @Test
    @DisplayName("Should update existing sector when it exists")
    void shouldUpdateExistingSectorWhenItExists() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(simulatorResponse);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.of(existingSector));
        when(sectorRepository.findByGarageId(garageId)).thenReturn(List.of(existingSector));
        when(spotRepository.findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sectorRepository.save(any(Sector.class))).thenReturn(existingSector);
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> initializationService.initializeFromSimulator());

        // Then
        verify(sectorRepository).save(existingSector);
    }

    @Test
    @DisplayName("Should create new spot when it does not exist")
    void shouldCreateNewSpotWhenItDoesNotExist() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(simulatorResponse);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.of(existingSector));
        when(sectorRepository.findByGarageId(garageId)).thenReturn(List.of(existingSector));
        when(spotRepository.findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
                any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sectorRepository.save(any(Sector.class))).thenReturn(existingSector);
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> initializationService.initializeFromSimulator());

        // Then
        ArgumentCaptor<ParkingSpot> spotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(spotRepository).save(spotCaptor.capture());
        ParkingSpot savedSpot = spotCaptor.getValue();
        assertEquals(new BigDecimal("-23.561684"), savedSpot.getLatitude());
        assertEquals(new BigDecimal("-46.655981"), savedSpot.getLongitude());
        assertFalse(savedSpot.getIsOccupied());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when simulator returns null")
    void shouldThrowExceptionWhenSimulatorReturnsNull() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(null);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                initializationService.initializeFromSimulator());

        assertTrue(exception.getMessage().contains("Garage initialization failed"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when simulator returns empty configuration")
    void shouldThrowExceptionWhenSimulatorReturnsEmptyConfiguration() {
        // Given
        GarageSimulatorResponseDto emptyResponse = new GarageSimulatorResponseDto(
            Collections.emptyList(),
            Collections.emptyList()
        );
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(emptyResponse);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                initializationService.initializeFromSimulator());

        assertTrue(exception.getMessage().contains("Garage initialization failed"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when spot sector not found")
    void shouldThrowExceptionWhenSpotSectorNotFound() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(simulatorResponse);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.of(existingSector));
        when(sectorRepository.findByGarageId(garageId)).thenReturn(Collections.emptyList()); // No sectors found
        when(sectorRepository.save(any(Sector.class))).thenReturn(existingSector);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                initializationService.initializeFromSimulator());

        assertTrue(exception.getMessage().contains("Garage initialization failed"));
    }

    @Test
    @DisplayName("Should handle empty spots configuration gracefully")
    void shouldHandleEmptySpotsConfigurationGracefully() {
        // Given
        GarageSimulatorResponseDto.SectorConfigDto sectorConfig = 
            new GarageSimulatorResponseDto.SectorConfigDto("A", new BigDecimal("10.00"), 100);
        GarageSimulatorResponseDto responseWithNoSpots = new GarageSimulatorResponseDto(
            List.of(sectorConfig),
            Collections.emptyList()
        );
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(existingGarage));
        when(simulatorClient.getGarageConfiguration()).thenReturn(responseWithNoSpots);
        when(sectorRepository.findByGarageIdAndSectorCode(garageId, "A")).thenReturn(Optional.of(existingSector));
        when(sectorRepository.save(any(Sector.class))).thenReturn(existingSector);

        // When
        assertDoesNotThrow(() -> initializationService.initializeFromSimulator());

        // Then
        verify(spotRepository, never()).save(any(ParkingSpot.class));
    }
}
