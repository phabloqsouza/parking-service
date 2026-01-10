package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID> {
    
    List<ParkingSpot> findBySectorId(UUID sectorId);
    
    List<ParkingSpot> findBySectorIdAndIsOccupiedFalse(UUID sectorId);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.sectorId = :sectorId " +
           "AND ps.latitude BETWEEN :minLat AND :maxLat " +
           "AND ps.longitude BETWEEN :minLng AND :maxLng")
    List<ParkingSpot> findBySectorIdAndLatitudeBetweenAndLongitudeBetween(
            @Param("sectorId") UUID sectorId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.latitude BETWEEN :minLat AND :maxLat " +
           "AND ps.longitude BETWEEN :minLng AND :maxLng")
    List<ParkingSpot> findByLatitudeBetweenAndLongitudeBetween(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng);
}
