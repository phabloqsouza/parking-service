package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {
    
    private final ParkingSpotRepository spotRepository;
    private final SectorCapacityService sectorCapacityService;
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void assignSpot(ParkingSession session, ParkingSpot spot) {
        spot.setIsOccupied(true);
        spotRepository.save(spot);
        
        session.setSpot(spot);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void freeSpot(ParkingSession session) {
        if (session.getSpot() == null) {
            return;
        }
        
        ParkingSpot spot = session.getSpot();
        Sector sector = spot.getSector();
        
        spot.setIsOccupied(false);
        spotRepository.save(spot);
        
        sectorCapacityService.decrementCapacity(sector);
    }
}
