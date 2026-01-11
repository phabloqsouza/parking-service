package com.estapar.parking.service;

import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.exception.*;
import com.estapar.parking.infrastructure.persistence.entity.*;
import com.estapar.parking.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParkingEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(ParkingEventService.class);
    
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final PricingService pricingService;
    private final GarageResolver garageResolver;
    private final ParkingMapper parkingMapper;
    
    @Transactional
    public void handleEntryEvent(UUID garageId, String vehicleLicensePlate, Instant entryTime, String sectorCode) {
        Garage garage = garageResolver.getGarage(garageId);
        
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(garage.getId(), sectorCode)
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + sectorCode));
        
        // Check capacity with optimistic locking
        int occupiedCount = sector.getOccupiedCount() != null ? sector.getOccupiedCount() : 0;
        int maxCapacity = sector.getMaxCapacity() != null ? sector.getMaxCapacity() : 0;
        if (occupiedCount >= maxCapacity) {
            throw new SectorFullException(
                String.format("Sector %s is full (capacity: %d/%d)", sectorCode, occupiedCount, maxCapacity));
        }
        
        // Check if vehicle already has active session
        sessionRepository.findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), vehicleLicensePlate)
                .ifPresent(session -> {
                    throw new IllegalStateException(
                        String.format("Vehicle %s already has an active parking session", vehicleLicensePlate));
                });
        
        // Calculate dynamic pricing
        BigDecimal occupancyPercentage = pricingService.calculateOccupancy(
                sector.getOccupiedCount(), sector.getMaxCapacity());
        BigDecimal basePriceWithDynamicPricing = pricingService.applyPricing(
                sector.getBasePrice(), occupancyPercentage);
        
        // Reserve capacity (increment occupied count) with optimistic locking
        sector.setOccupiedCount((sector.getOccupiedCount() != null ? sector.getOccupiedCount() : 0) + 1);
        sectorRepository.save(sector);
        
        // Create parking session using mapper
        ParkingSession session = parkingMapper.toParkingSession(
                vehicleLicensePlate,
                entryTime,
                garage.getId(),
                sector.getId(),
                basePriceWithDynamicPricing
        );
        
        sessionRepository.save(session);
        logger.info("Entry event processed: vehicle={}, sector={}, basePrice={}", 
                   vehicleLicensePlate, sectorCode, basePriceWithDynamicPricing);
    }
    
    @Transactional
    public void handleParkedEvent(UUID garageId, String vehicleLicensePlate, BigDecimal latitude, BigDecimal longitude) {
        Garage garage = garageResolver.getGarage(garageId);
        
        // Find active session
        ParkingSession session = sessionRepository
                .findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), vehicleLicensePlate)
                .orElseThrow(() -> new ParkingSessionNotFoundException(
                    String.format("No active parking session found for vehicle: %s", vehicleLicensePlate)));
        
        // Check if already parked (idempotent handling)
        if (session.getSpotId() != null) {
            logger.warn("Vehicle {} already has spot assigned (spot_id: {}). Duplicate PARKED event ignored.", 
                       vehicleLicensePlate, session.getSpotId());
            return;
        }
        
        // Get sector to find spots within sector
        Sector sector = sectorRepository.findById(session.getSectorId())
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + session.getSectorId()));
        
        // Calculate tolerance bounds for database query
        BigDecimal tolerance = BigDecimal.valueOf(0.000001);
        BigDecimal minLat = latitude.subtract(tolerance);
        BigDecimal maxLat = latitude.add(tolerance);
        BigDecimal minLng = longitude.subtract(tolerance);
        BigDecimal maxLng = longitude.add(tolerance);
        
        // Find matching spots directly from database (filtered by tolerance)
        List<ParkingSpot> matchingSpots = spotRepository
                .findBySectorIdAndLatitudeAndLongitudeWithinTolerance(
                        sector.getId(), minLat, maxLat, minLng, maxLng);
        
        if (matchingSpots.isEmpty()) {
            logger.warn("No parking spot found within tolerance for coordinates ({}, {}) in sector {}. " +
                       "Spot assignment skipped (graceful degradation).", 
                       latitude, longitude, sector.getSectorCode());
            // Keep spot_id as null - graceful degradation
            return;
        }
        
        if (matchingSpots.size() > 1) {
            logger.error("Multiple parking spots ({}) found within tolerance for coordinates ({}, {}) in sector {}. " +
                        "Ambiguous match - spot assignment skipped.", 
                        matchingSpots.size(), latitude, longitude, sector.getSectorCode());
            throw new com.estapar.parking.exception.AmbiguousSpotMatchException(
                String.format("Multiple parking spots (%d) found within tolerance for coordinates (%.8f, %.8f). " +
                             "Ambiguous match requires investigation or stricter tolerance.", 
                             matchingSpots.size(), latitude, longitude));
        }
        
        try {
            // Single matching spot found
            ParkingSpot matchedSpot = matchingSpots.get(0);
            
            // Check if spot is already occupied (optimistic locking)
            if (Boolean.TRUE.equals(matchedSpot.getIsOccupied())) {
                throw new SpotAlreadyOccupiedException(
                    String.format("Spot %s is already occupied", matchedSpot.getId()));
            }
            
            // Validate spot belongs to same sector
            if (!matchedSpot.getSectorId().equals(session.getSectorId())) {
                throw new IllegalStateException(
                    String.format("Matched spot %s does not belong to session sector %s", 
                                 matchedSpot.getId(), session.getSectorId()));
            }
            
            // Assign spot with optimistic locking
            matchedSpot.setIsOccupied(true);
            spotRepository.save(matchedSpot);
            
            session.setSpotId(matchedSpot.getId());
            sessionRepository.save(session);
            
            logger.info("Parked event processed: vehicle={}, spot_id={}", vehicleLicensePlate, matchedSpot.getId());
            
        } catch (AmbiguousSpotMatchException e) {
            logger.warn("Ambiguous spot match for vehicle {} at coordinates ({}, {}): {}. " +
                       "Session will continue with spot_id=null (graceful degradation).", 
                       vehicleLicensePlate, latitude, longitude, e.getMessage());
            // Keep spot_id as null - graceful degradation, capacity already counted on ENTRY
        }
    }
    
    @Transactional
    public void handleExitEvent(UUID garageId, String vehicleLicensePlate, Instant exitTime) {
        Garage garage = garageResolver.getGarage(garageId);
        
        // Find active session
        ParkingSession session = sessionRepository
                .findByGarageIdAndVehicleLicensePlateAndExitTimeIsNull(garage.getId(), vehicleLicensePlate)
                .orElseThrow(() -> new ParkingSessionNotFoundException(
                    String.format("No active parking session found for vehicle: %s", vehicleLicensePlate)));
        
        // Calculate final price
        BigDecimal finalPrice = pricingService.calculateFee(
                session.getEntryTime(), exitTime, session.getBasePrice());
        
        // Free spot if assigned
        if (session.getSpotId() != null) {
            spotRepository.findById(session.getSpotId()).ifPresent(spot -> {
                spot.setIsOccupied(false);
                spotRepository.save(spot);
            });
        }
        
        // Decrement sector capacity
        Sector sector = sectorRepository.findById(session.getSectorId())
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + session.getSectorId()));
        int currentCount = sector.getOccupiedCount() != null ? sector.getOccupiedCount() : 0;
        sector.setOccupiedCount(Math.max(0, currentCount - 1));
        sectorRepository.save(sector);
        
        // Update session
        session.setExitTime(exitTime);
        session.setFinalPrice(finalPrice);
        sessionRepository.save(session);
        
        logger.info("Exit event processed: vehicle={}, finalPrice={}, duration={} minutes", 
                   vehicleLicensePlate, finalPrice, 
                   java.time.Duration.between(session.getEntryTime(), exitTime).toMinutes());
    }
}
