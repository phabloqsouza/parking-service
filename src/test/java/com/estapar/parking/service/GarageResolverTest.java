package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarageResolverTest {

    @Mock
    private GarageRepository garageRepository;

    @InjectMocks
    private GarageResolver garageResolver;

    private UUID garageId;
    private Garage garage;
    private Garage defaultGarage;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();
        garage = new Garage();
        garage.setId(garageId);
        garage.setIsDefault(false);
        garage.setCreatedAt(Instant.now());

        defaultGarage = new Garage();
        defaultGarage.setId(UUID.randomUUID());
        defaultGarage.setIsDefault(true);
        defaultGarage.setCreatedAt(Instant.now());
    }

    @Test
    void getGarage_WithValidGarageId_ShouldReturnGarage() {
        when(garageRepository.findById(garageId)).thenReturn(Optional.of(garage));

        Garage result = garageResolver.getGarage(garageId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(garageId);
    }

    @Test
    void getGarage_WithNullGarageId_ShouldReturnDefaultGarage() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));

        Garage result = garageResolver.getGarage(null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(defaultGarage.getId());
        assertThat(result.getIsDefault()).isTrue();
    }

    @Test
    void getGarage_WithNonExistentGarageId_ShouldThrowException() {
        when(garageRepository.findById(garageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageResolver.getGarage(garageId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).contains("Garage not found");
                });
    }

    @Test
    void getDefaultGarage_WithExistingDefaultGarage_ShouldReturnGarage() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));

        Garage result = garageResolver.getDefaultGarage();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(defaultGarage.getId());
        assertThat(result.getIsDefault()).isTrue();
    }

    @Test
    void getDefaultGarage_WithNoDefaultGarage_ShouldThrowException() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageResolver.getDefaultGarage())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).contains("No default garage found");
                });
    }
}
