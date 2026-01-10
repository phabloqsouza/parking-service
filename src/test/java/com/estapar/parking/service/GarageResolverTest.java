package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GarageResolver Unit Tests")
class GarageResolverTest {

    @Mock
    private GarageRepository garageRepository;

    @InjectMocks
    private GarageResolver garageResolver;

    private Garage defaultGarage;
    private Garage specificGarage;
    private UUID defaultGarageId;
    private UUID specificGarageId;

    @BeforeEach
    void setUp() {
        defaultGarageId = UUID.randomUUID();
        specificGarageId = UUID.randomUUID();

        defaultGarage = new Garage();
        defaultGarage.setId(defaultGarageId);
        defaultGarage.setName("Default Garage");
        defaultGarage.setIsDefault(true);
        defaultGarage.setCreatedAt(Instant.now());

        specificGarage = new Garage();
        specificGarage.setId(specificGarageId);
        specificGarage.setName("Specific Garage");
        specificGarage.setIsDefault(false);
        specificGarage.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should return specific garage when garageId is provided")
    void shouldReturnSpecificGarageWhenGarageIdProvided() {
        // Given
        when(garageRepository.findById(specificGarageId)).thenReturn(Optional.of(specificGarage));

        // When
        Garage result = garageResolver.resolveGarage(specificGarageId);

        // Then
        assertNotNull(result);
        assertEquals(specificGarageId, result.getId());
        assertEquals("Specific Garage", result.getName());
        verify(garageRepository).findById(specificGarageId);
        verify(garageRepository, never()).findByIsDefaultTrue();
    }

    @Test
    @DisplayName("Should return default garage when garageId is null")
    void shouldReturnDefaultGarageWhenGarageIdIsNull() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));

        // When
        Garage result = garageResolver.resolveGarage(null);

        // Then
        assertNotNull(result);
        assertEquals(defaultGarageId, result.getId());
        assertTrue(result.getIsDefault());
        verify(garageRepository, never()).findById(any());
        verify(garageRepository).findByIsDefaultTrue();
    }

    @Test
    @DisplayName("Should throw IllegalStateException when specific garage not found")
    void shouldThrowExceptionWhenSpecificGarageNotFound() {
        // Given
        when(garageRepository.findById(specificGarageId)).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                garageResolver.resolveGarage(specificGarageId));

        assertTrue(exception.getMessage().contains("Garage not found"));
        assertTrue(exception.getMessage().contains(specificGarageId.toString()));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when default garage not found")
    void shouldThrowExceptionWhenDefaultGarageNotFound() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                garageResolver.resolveGarage(null));

        assertTrue(exception.getMessage().contains("No default garage found"));
    }

    @Test
    @DisplayName("Should return default garage using getDefaultGarage method")
    void shouldReturnDefaultGarageUsingGetDefaultGarageMethod() {
        // Given
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));

        // When
        Garage result = garageResolver.getDefaultGarage();

        // Then
        assertNotNull(result);
        assertEquals(defaultGarageId, result.getId());
        assertTrue(result.getIsDefault());
        verify(garageRepository).findByIsDefaultTrue();
    }
}
