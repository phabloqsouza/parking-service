package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID> {
    
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.sector.garage.id = :garageId " +
           "AND ps.latitude = :latitude AND ps.longitude = :longitude")
    Optional<ParkingSpot> findByGarageIdAndLatitudeAndLongitude(
            @Param("garageId") UUID garageId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude);
}
