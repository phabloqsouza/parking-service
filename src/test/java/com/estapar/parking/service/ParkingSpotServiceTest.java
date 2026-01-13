package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceTest {

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private SectorCapacityService sectorCapacityService;

    @InjectMocks
    private ParkingSpotService parkingSpotService;

    private ParkingSession session;
    private ParkingSpot spot;
    private Sector sector;

    @BeforeEach
    void setUp() {
        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setOccupiedCount(5);

        spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setSector(sector);
        spot.setIsOccupied(false);
        spot.setLatitude(new BigDecimal("-23.550520"));
        spot.setLongitude(new BigDecimal("-46.633308"));

        session = new ParkingSession();
        session.setId(UUID.randomUUID());
        session.setSpot(null);
        session.setSector(null);
    }

    @Test
    void assignSpot_ShouldSetSpotAsOccupiedAndAssignToSession() {
        when(spotRepository.save(spot)).thenReturn(spot);

        parkingSpotService.assignSpot(session, spot);

        assertThat(spot.getIsOccupied()).isTrue();
        assertThat(session.getSpot()).isEqualTo(spot);
        assertThat(session.getSector()).isEqualTo(sector);
        verify(spotRepository).save(spot);
    }

    @Test
    void freeSpot_WithSpotAssigned_ShouldFreeSpotAndDecrementCapacity() {
        session.setSpot(spot);
        session.setSector(sector);
        when(spotRepository.save(spot)).thenReturn(spot);

        parkingSpotService.freeSpot(session);

        assertThat(spot.getIsOccupied()).isFalse();
        verify(spotRepository).save(spot);
        verify(sectorCapacityService).decrementCapacity(sector);
    }

    @Test
    void freeSpot_WithNoSpotAssigned_ShouldDoNothing() {
        session.setSpot(null);
        session.setSector(null);

        parkingSpotService.freeSpot(session);

        verify(spotRepository, never()).save(any());
        verify(sectorCapacityService, never()).decrementCapacity(any());
    }
}
