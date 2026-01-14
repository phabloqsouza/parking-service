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
    
    /**
     * Assigns a parking spot to a session.
     * 
     * Uses MANDATORY propagation to ensure this method is always called within
     * an existing transaction context. This enforces transaction boundaries
     * and ensures that spot assignment and capacity updates happen atomically
     * as part of the event handler's transaction.
     * 
     * @param session the parking session
     * @param spot the spot to assign
     * @throws IllegalTransactionStateException if called without an active transaction
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void assignSpot(ParkingSession session, ParkingSpot spot) {
        spot.setIsOccupied(true);
        spotRepository.save(spot);
        
        session.setSpot(spot);
    }
    
    /**
     * Frees a parking spot and decrements sector capacity.
     * 
     * Uses MANDATORY propagation to ensure this method is always called within
     * an existing transaction context. This ensures that spot freeing and capacity
     * decrement happen atomically as part of the event handler's transaction.
     * 
     * @param session the parking session containing the spot to free
     * @throws IllegalTransactionStateException if called without an active transaction
     */
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
