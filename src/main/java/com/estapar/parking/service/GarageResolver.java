package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GarageResolver {
    
    private final GarageRepository garageRepository;
    
    public GarageResolver(GarageRepository garageRepository) {
        this.garageRepository = garageRepository;
    }
    
    @Transactional(readOnly = true)
    public Garage resolveGarage(UUID garageId) {
        if (garageId != null) {
            return garageRepository.findById(garageId)
                    .orElseThrow(() -> new IllegalStateException("Garage not found: " + garageId));
        }
        return getDefaultGarage();
    }
    
    @Transactional(readOnly = true)
    public Garage getDefaultGarage() {
        return garageRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("No default garage found. System must be initialized."));
    }
}
