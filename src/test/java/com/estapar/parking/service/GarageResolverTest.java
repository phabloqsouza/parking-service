package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarageResolverTest {
    
    @Mock
    private GarageRepository garageRepository;
    
    private GarageResolver garageResolver;
    
    @BeforeEach
    void setUp() {
        garageResolver = new GarageResolver(garageRepository);
    }
    
    @Test
    void getGarage_WithValidId_ShouldReturnGarage() {
        UUID garageId = UUID.randomUUID();
        Garage garage = createGarage(garageId, "Test Garage", true);
        
        when(garageRepository.findById(garageId)).thenReturn(Optional.of(garage));
        
        Garage result = garageResolver.getGarage(garageId);
        
        assertEquals(garageId, result.getId());
        assertEquals("Test Garage", result.getName());
    }
    
    @Test
    void getGarage_WithNullId_ShouldReturnDefaultGarage() {
        UUID defaultGarageId = UUID.randomUUID();
        Garage defaultGarage = createGarage(defaultGarageId, "Default Garage", true);
        
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));
        
        Garage result = garageResolver.getGarage(null);
        
        assertEquals(defaultGarageId, result.getId());
        assertTrue(result.getIsDefault());
    }
    
    @Test
    void getGarage_WithNonExistentId_ShouldThrowException() {
        UUID garageId = UUID.randomUUID();
        
        when(garageRepository.findById(garageId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalStateException.class, () -> {
            garageResolver.getGarage(garageId);
        });
    }
    
    @Test
    void getDefaultGarage_WhenExists_ShouldReturnDefaultGarage() {
        UUID defaultGarageId = UUID.randomUUID();
        Garage defaultGarage = createGarage(defaultGarageId, "Default Garage", true);
        
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultGarage));
        
        Garage result = garageResolver.getDefaultGarage();
        
        assertEquals(defaultGarageId, result.getId());
        assertTrue(result.getIsDefault());
    }
    
    @Test
    void getDefaultGarage_WhenNotExists_ShouldThrowException() {
        when(garageRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        
        assertThrows(IllegalStateException.class, () -> {
            garageResolver.getDefaultGarage();
        });
    }
    
    private Garage createGarage(UUID id, String name, boolean isDefault) {
        Garage garage = new Garage();
        garage.setId(id);
        garage.setName(name);
        garage.setIsDefault(isDefault);
        garage.setCreatedAt(Instant.now());
        return garage;
    }
}
