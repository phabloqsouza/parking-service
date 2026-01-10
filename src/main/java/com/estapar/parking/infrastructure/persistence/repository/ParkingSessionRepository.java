package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {
    
    Optional<ParkingSession> findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(
            UUID garageId, String vehicleLicensePlate);
    
    List<ParkingSession> findBySectorIdAndExitTimeIsNull(UUID sectorId);
    
    Optional<ParkingSession> findBySpotIdAndExitTimeIsNull(UUID spotId);
    
    @Query(value = "SELECT * FROM parking_session ps WHERE ps.garage_id = :garageId " +
           "AND ps.sector_id = :sectorId " +
           "AND DATE(ps.entry_time) = DATE(:date) " +
           "AND ps.exit_time IS NOT NULL " +
           "AND ps.final_price IS NOT NULL", nativeQuery = true)
    List<ParkingSession> findCompletedSessionsByGarageAndSectorAndDate(
            @Param("garageId") UUID garageId,
            @Param("sectorId") UUID sectorId,
            @Param("date") Instant date);
    
    @Query("SELECT ps FROM ParkingSession ps WHERE ps.garageId = :garageId " +
           "AND ps.entryTime BETWEEN :startDate AND :endDate")
    List<ParkingSession> findByGarageIdAndEntryTimeBetween(
            @Param("garageId") UUID garageId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
