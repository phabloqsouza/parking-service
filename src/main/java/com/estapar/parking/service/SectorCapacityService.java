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
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void incrementCapacity(Sector sector) {
        sector.setOccupiedCount(sector.getOccupiedCount() + 1);
        sectorRepository.save(sector);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void decrementCapacity(Sector sector) {
        int currentCount = sector.getOccupiedCount();
        sector.setOccupiedCount(Math.max(0, currentCount - 1));
        sectorRepository.save(sector);
    }
}
