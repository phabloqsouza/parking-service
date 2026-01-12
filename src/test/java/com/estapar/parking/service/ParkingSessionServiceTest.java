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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSessionServiceTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @InjectMocks
    private ParkingSessionService parkingSessionService;

    private Garage garage;
    private String licensePlate;
    private ParkingSession session;

    @BeforeEach
    void setUp() {
        garage = new Garage();
        garage.setId(UUID.randomUUID());
        licensePlate = "ABC1234";
        
        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setVehicleLicensePlate(licensePlate);
        session.setEntryTime(Instant.now());
    }

    @Test
    void findActiveSession_WithExistingSession_ShouldReturnSession() {
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate))
                .thenReturn(Optional.of(session));

        ParkingSession result = parkingSessionService.findActiveSession(garage, licensePlate);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(session.getId());
        assertThat(result.getVehicleLicensePlate()).isEqualTo(licensePlate);
    }

    @Test
    void findActiveSession_WithNoActiveSession_ShouldThrowException() {
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingSessionService.findActiveSession(garage, licensePlate))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).contains("No active parking session found");
                });
    }

    @Test
    void findActiveSessionOptional_WithExistingSession_ShouldReturnOptionalWithSession() {
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate))
                .thenReturn(Optional.of(session));

        Optional<ParkingSession> result = parkingSessionService.findActiveSessionOptional(garage, licensePlate);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }

    @Test
    void findActiveSessionOptional_WithNoActiveSession_ShouldReturnEmpty() {
        when(sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), licensePlate))
                .thenReturn(Optional.empty());

        Optional<ParkingSession> result = parkingSessionService.findActiveSessionOptional(garage, licensePlate);

        assertThat(result).isEmpty();
    }
}
