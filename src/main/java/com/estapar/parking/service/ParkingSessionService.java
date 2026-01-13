package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.estapar.parking.api.exception.ErrorMessages.NO_ACTIVE_SESSION;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingSessionService {
    
    private final ParkingSessionRepository sessionRepository;
    
    @Transactional(readOnly = true)
    public ParkingSession findActiveSession(Garage garage, String licensePlate) {
        return sessionRepository
                .findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                    String.format(NO_ACTIVE_SESSION, licensePlate)));
    }
    
    @Transactional(readOnly = true)
    public boolean existsActiveSession(Garage garage, String licensePlate) {
        return sessionRepository.existsActiveSession(garage.getId(), licensePlate);
    }
}
