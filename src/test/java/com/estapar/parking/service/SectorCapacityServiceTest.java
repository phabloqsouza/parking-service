package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorCapacityServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @InjectMocks
    private SectorCapacityService sectorCapacityService;

    private Sector sector;

    @BeforeEach
    void setUp() {
        sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setOccupiedCount(5);
        sector.setMaxCapacity(100);
    }

    @Test
    void incrementCapacity_ShouldIncrementOccupiedCount() {
        int initialCount = sector.getOccupiedCount();
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);

        sectorCapacityService.incrementCapacity(sector);

        assertThat(sector.getOccupiedCount()).isEqualTo(initialCount + 1);
        verify(sectorRepository).save(sector);
    }

    @Test
    void decrementCapacity_WithPositiveCount_ShouldDecrementOccupiedCount() {
        sector.setOccupiedCount(5);
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);

        sectorCapacityService.decrementCapacity(sector);

        assertThat(sector.getOccupiedCount()).isEqualTo(4);
        verify(sectorRepository).save(sector);
    }

    @Test
    void decrementCapacity_WithZeroCount_ShouldNotGoNegative() {
        sector.setOccupiedCount(0);
        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);

        sectorCapacityService.decrementCapacity(sector);

        verify(sectorRepository).save(sectorCaptor.capture());
        assertThat(sectorCaptor.getValue().getOccupiedCount()).isEqualTo(0);
    }

    @Test
    void decrementCapacity_ShouldUseMaxFunction() {
        sector.setOccupiedCount(1);
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);

        sectorCapacityService.decrementCapacity(sector);

        assertThat(sector.getOccupiedCount()).isEqualTo(0);
        verify(sectorRepository).save(sector);
    }
}
