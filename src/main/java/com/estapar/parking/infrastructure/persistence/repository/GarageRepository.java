package com.estapar.parking.infrastructure.persistence.repository;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GarageRepository extends JpaRepository<Garage, UUID> {
    
    Optional<Garage> findByIsDefaultTrue();
    
    boolean existsByIsDefaultTrue();
    
}
