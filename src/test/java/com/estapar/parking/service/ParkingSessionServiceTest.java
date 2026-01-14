package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSessionServiceTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @InjectMocks
    private ParkingSessionService parkingSessionService;

    private Garage garage;
    private ParkingSession session;
    private String licensePlate;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());

        licensePlate = "ABC1234";

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setGarage(garage);
        session.setVehicleLicensePlate(licensePlate);
        session.setEntryTime(Instant.now());
        session.setExitTime(null);
    }

    @Test
    void findActiveSession_WithActiveSession_ShouldReturnSession() {
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(
                garage.getId(), licensePlate)).thenReturn(Optional.of(session));

        ParkingSession result = parkingSessionService.findActiveSession(garage, licensePlate);

        assertThat(result).isEqualTo(session);
        verify(sessionRepository).findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(
                garage.getId(), licensePlate);
    }

    @Test
    void findActiveSession_WithNoActiveSession_ShouldThrowException() {
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(
                garage.getId(), licensePlate)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingSessionService.findActiveSession(garage, licensePlate))
                .isInstanceOf(ResponseStatusException.class);
        verify(sessionRepository).findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(
                garage.getId(), licensePlate);
    }

    @Test
    void existsActiveSession_WithActiveSession_ShouldReturnTrue() {
        when(sessionRepository.existsActiveSession(garage.getId(), licensePlate)).thenReturn(true);

        boolean result = parkingSessionService.existsActiveSession(garage, licensePlate);

        assertThat(result).isTrue();
        verify(sessionRepository).existsActiveSession(garage.getId(), licensePlate);
    }

    @Test
    void existsActiveSession_WithNoActiveSession_ShouldReturnFalse() {
        when(sessionRepository.existsActiveSession(garage.getId(), licensePlate)).thenReturn(false);

        boolean result = parkingSessionService.existsActiveSession(garage, licensePlate);

        assertThat(result).isFalse();
        verify(sessionRepository).existsActiveSession(garage.getId(), licensePlate);
    }
}
