package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.estapar.parking.api.exception.ErrorMessages.NO_ACTIVE_SESSION;
import static com.estapar.parking.api.exception.ErrorMessages.notFound;

@Service
@RequiredArgsConstructor
public class ParkingSessionService {
    
    private final ParkingSessionRepository sessionRepository;
    
    @Transactional(readOnly = true)
    public ParkingSession findActiveSession(Garage garage, String licensePlate) {
        return sessionRepository
                .findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate)
                .orElseThrow(() -> notFound(NO_ACTIVE_SESSION, licensePlate));
    }
    
    @Transactional(readOnly = true)
    public boolean existsActiveSession(Garage garage, String licensePlate) {
        return sessionRepository.existsActiveSession(garage.getId(), licensePlate);
    }
}
