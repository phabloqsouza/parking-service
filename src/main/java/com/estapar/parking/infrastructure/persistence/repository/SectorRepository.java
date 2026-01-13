package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorRepository extends JpaRepository<Sector, UUID> {

    Optional<Sector> findByGarageIdAndSectorCode(UUID garageId, String sectorCode);

}
