package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GarageRepository extends JpaRepository<Garage, UUID> {
    
    Optional<Garage> findByIsDefaultTrue();
    
    boolean existsByIsDefaultTrue();
    
    /**
     * Calculates current garage occupancy using two-level capacity tracking:
     * 
     * 1. Sum of all sector.occupied_count values (sessions that have a spot assigned - incremented on PARKED)
     * 2. Count of parking sessions without sector_id (entered but not parked - counted on ENTRY)
     * 
     * This ensures that:
     * - Vehicles that entered but haven't parked yet count toward garage capacity
     * - Sector capacity is only incremented when spot is actually assigned
     * - Both levels contribute to overall garage occupancy calculation
     */
    @Query(value = "SELECT " +
           "((SELECT COALESCE(SUM(s.occupied_count), 0) FROM sector s WHERE s.garage_id = :garageId) + " +
           "(SELECT COALESCE(COUNT(ps.id), 0) FROM parking_session ps " +
           " WHERE ps.garage_id = :garageId AND ps.sector_id IS NULL AND ps.exit_time IS NULL))",
           nativeQuery = true)
    long calcOccupancy(@Param("garageId") UUID garageId);
    
}
