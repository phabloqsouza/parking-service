package com.estapar.parking.component;

import com.estapar.parking.infrastructure.persistence.entity.*;
import com.estapar.parking.infrastructure.persistence.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TestDataSetup {

    private final GarageRepository garageRepository;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final PricingStrategyRepository pricingStrategyRepository;

    public TestDataSetup(
            GarageRepository garageRepository,
            SectorRepository sectorRepository,
            ParkingSpotRepository spotRepository,
            PricingStrategyRepository pricingStrategyRepository) {
        this.garageRepository = garageRepository;
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.pricingStrategyRepository = pricingStrategyRepository;
    }

    @Transactional
    public Garage createDefaultGarage() {
        if (garageRepository.existsByIsDefaultTrue()) {
            return garageRepository.findByIsDefaultTrue().orElseThrow();
        }

        Garage garage = new Garage();
        garage.setId(UUID.randomUUID());
        garage.setIsDefault(true);
        garage.setCreatedAt(Instant.now());
        garage.setSectors(new ArrayList<>());
        return garageRepository.save(garage);
    }

    @Transactional
    public Sector createSector(Garage garage, String sectorCode, BigDecimal basePrice, Integer maxCapacity) {
        Sector sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setGarage(garage);
        sector.setSectorCode(sectorCode);
        sector.setBasePrice(basePrice);
        sector.setMaxCapacity(maxCapacity);
        sector.setOccupiedCount(0);
        sector.setVersion(0);
        sector.setSpots(new ArrayList<>());
        
        Sector saved = sectorRepository.save(sector);
        if (garage.getSectors() == null) {
            garage.setSectors(new ArrayList<>());
        }
        garage.getSectors().add(saved);
        return saved;
    }

    @Transactional
    public ParkingSpot createSpot(Sector sector, BigDecimal latitude, BigDecimal longitude) {
        ParkingSpot spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setSector(sector);
        spot.setLatitude(latitude);
        spot.setLongitude(longitude);
        spot.setIsOccupied(false);
        spot.setVersion(0);
        
        ParkingSpot saved = spotRepository.save(spot);
        if (sector.getSpots() == null) {
            sector.setSpots(new ArrayList<>());
        }
        sector.getSpots().add(saved);
        return saved;
    }

    @Transactional
    public PricingStrategy createPricingStrategy(
            BigDecimal minOccupancy,
            BigDecimal maxOccupancy,
            BigDecimal multiplier) {
        PricingStrategy strategy = new PricingStrategy();
        strategy.setId(UUID.randomUUID());
        strategy.setOccupancyMinPercentage(minOccupancy);
        strategy.setOccupancyMaxPercentage(maxOccupancy);
        strategy.setMultiplier(multiplier);
        strategy.setIsActive(true);
        strategy.setCreatedAt(Instant.now());
        return pricingStrategyRepository.save(strategy);
    }

    @Transactional
    public void createDefaultPricingStrategies() {
        createPricingStrategy(BigDecimal.ZERO, new BigDecimal("24.99"), new BigDecimal("0.90"));
        createPricingStrategy(new BigDecimal("25.00"), new BigDecimal("49.99"), BigDecimal.ONE);
        createPricingStrategy(new BigDecimal("50.00"), new BigDecimal("74.99"), new BigDecimal("1.10"));
        createPricingStrategy(new BigDecimal("75.00"), new BigDecimal("100.00"), new BigDecimal("1.25"));
    }

    @Transactional
    public void setSectorOccupiedCount(Sector sector, int count) {
        sector.setOccupiedCount(count);
        sectorRepository.save(sector);
    }

    @Transactional
    public void cleanupTestData() {
        pricingStrategyRepository.deleteAll();
        spotRepository.deleteAll();
        sectorRepository.deleteAll();
        garageRepository.deleteAll();
    }
}
