package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarageResolverTest {

    @Mock
    private GarageRepository garageRepository;

    @InjectMocks
    private GarageResolver garageResolver;

    private Garage garage;
    private Garage defaultGarage;
    private UUID garageId;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();

        garage = new Garage();
        garage.setId(garageId);
        garage.setIsDefault(false);
        garage.setMaxCapacity(100);
        garage.setCreatedAt(Instant.now());

        defaultGarage = new Garage();
        defaultGarage.setId(UUID.randomUUID());
        defaultGarage.setIsDefault(true);
        defaultGarage.setMaxCapacity(200);
        defaultGarage.setCreatedAt(Instant.now());
    }

    @Test
    void getGarage_WithGarageId_ShouldReturnGarage() {
        when(garageRepository.findById(garageId)).thenReturn(Optional.of(garage));

        Garage result = garageResolver.getGarage(garageId);

        assertThat(result).isEqualTo(garage);
        verify(garageRepository).findById(garageId);
    }

    @Test
    void getGarage_WithNullGarageId_ShouldReturnDefaultGarage() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));

        Garage result = garageResolver.getGarage(null);

        assertThat(result).isEqualTo(defaultGarage);
        verify(garageRepository).findByIsDefaultTrue();
    }

    @Test
    void getGarage_WithNonExistentGarageId_ShouldThrowException() {
        when(garageRepository.findById(garageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageResolver.getGarage(garageId))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(garageRepository).findById(garageId);
    }

    @Test
    void getDefaultGarage_WithDefaultGarage_ShouldReturnDefaultGarage() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));

        Garage result = garageResolver.getDefaultGarage();

        assertThat(result).isEqualTo(defaultGarage);
        verify(garageRepository).findByIsDefaultTrue();
    }

    @Test
    void getDefaultGarage_WithNoDefaultGarage_ShouldThrowException() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageResolver.getDefaultGarage())
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(garageRepository).findByIsDefaultTrue();
    }
}
