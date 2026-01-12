package com.estapar.parking.service;

import com.estapar.parking.exception.ParkingSessionNotFoundException;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingSessionService {
    
    private final ParkingSessionRepository sessionRepository;
    
    @Transactional(readOnly = true)
    public ParkingSession findActiveSession(Garage garage, String licensePlate) {
        return sessionRepository
                .findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate)
                .orElseThrow(() -> new ParkingSessionNotFoundException(
                    String.format("No active parking session found for vehicle: %s", licensePlate)));
    }
    
    @Transactional(readOnly = true)
    public Optional<ParkingSession> findActiveSessionOptional(Garage garage, String licensePlate) {
        return sessionRepository
                .findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate);
    }
}
