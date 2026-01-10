package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorRepository extends JpaRepository<Sector, UUID> {
    
    List<Sector> findByGarageId(UUID garageId);
    
    Optional<Sector> findByGarageIdAndSectorCode(UUID garageId, String sectorCode);
    
    @Query("SELECT s FROM Sector s WHERE s.garageId = :garageId AND s.occupiedCount < s.maxCapacity")
    List<Sector> findAvailableSectorsByGarageId(@Param("garageId") UUID garageId);
}
