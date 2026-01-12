package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GarageResolver {
    
    private final GarageRepository garageRepository;
    
    @Transactional(readOnly = true)
    public Garage getGarage(UUID garageId) {
        if (garageId != null) {
            return garageRepository.findById(garageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Garage not found: " + garageId));
        }
        return getDefaultGarage();
    }
    
    @Transactional(readOnly = true)
    public Garage getDefaultGarage() {
        return garageRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No default garage found. System must be initialized."));
    }
}
