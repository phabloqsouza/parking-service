package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.Sector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorRepository extends JpaRepository<Sector, UUID> {

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<Sector> findByGarageIdAndSectorCode(UUID garageId, String sectorCode);

}
