package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.estapar.parking.api.exception.ErrorMessages.GARAGE_NOT_FOUND;
import static com.estapar.parking.api.exception.ErrorMessages.NO_DEFAULT_GARAGE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GarageResolver {
    
    private final GarageRepository garageRepository;
    
    @Transactional(readOnly = true)
    public Garage getGarage(UUID garageId) {
        if (garageId != null) {
            return garageRepository.findById(garageId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, 
                        String.format(GARAGE_NOT_FOUND, garageId)));
        }
        return getDefaultGarage();
    }
    
    @Transactional(readOnly = true)
    public Garage getDefaultGarage() {
        return garageRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, NO_DEFAULT_GARAGE));
    }
}
