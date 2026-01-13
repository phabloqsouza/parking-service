package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {
    
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT ps FROM ParkingSession ps WHERE ps.garage.id = :garageId " +
           "AND ps.vehicleLicensePlate = :vehicleLicensePlate " +
           "AND ps.exitTime IS NULL")
    Optional<ParkingSession> findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(
            @Param("garageId") UUID garageId, 
            @Param("vehicleLicensePlate") String vehicleLicensePlate);
    
    @Query(value = "SELECT COALESCE(SUM(ps.final_price), 0) FROM parking_session ps " +
           "INNER JOIN parking_spot pspot ON ps.spot_id = pspot.id " +
           "INNER JOIN sector s ON pspot.sector_id = s.id " +
           "WHERE s.garage_id = :garageId " +
           "AND s.id = :sectorId " +
           "AND DATE(ps.entry_time) = DATE(:date) " +
           "AND ps.exit_time IS NOT NULL " +
           "AND ps.final_price IS NOT NULL", nativeQuery = true)
    BigDecimal sumRevenueByGarageAndSectorAndDate(
            @Param("garageId") UUID garageId,
            @Param("sectorId") UUID sectorId,
            @Param("date") Instant date);
}
