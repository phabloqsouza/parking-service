package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SectorCapacityService {
    
    private final SectorRepository sectorRepository;
    
    /**
     * Increments the occupied count for a sector.
     * 
     * Uses MANDATORY propagation to ensure this method is always called within
     * an existing transaction context. This is critical for maintaining data
     * consistency, as capacity updates must be part of the same transaction
     * as the event processing to prevent race conditions and ensure atomicity.
     * 
     * @param sector the sector whose capacity should be incremented
     * @throws IllegalTransactionStateException if called without an active transaction
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void incrementCapacity(Sector sector) {
        sector.setOccupiedCount(sector.getOccupiedCount() + 1);
        sectorRepository.save(sector);
    }
    
    /**
     * Decrements the occupied count for a sector.
     * 
     * Uses MANDATORY propagation to ensure this method is always called within
     * an existing transaction context. This is critical for maintaining data
     * consistency, as capacity updates must be part of the same transaction
     * as the event processing to prevent race conditions and ensure atomicity.
     * 
     * @param sector the sector whose capacity should be decremented
     * @throws IllegalTransactionStateException if called without an active transaction
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void decrementCapacity(Sector sector) {
        int currentCount = sector.getOccupiedCount();
        sector.setOccupiedCount(Math.max(0, currentCount - 1));
        sectorRepository.save(sector);
    }
}
