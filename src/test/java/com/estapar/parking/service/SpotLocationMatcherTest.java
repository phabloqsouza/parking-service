package com.estapar.parking.service;

import com.estapar.parking.exception.AmbiguousSpotMatchException;
import com.estapar.parking.exception.SpotNotFoundException;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpotLocationMatcherTest {
    
    private SpotLocationMatcher matcher;
    private BigDecimal tolerance;
    
    @BeforeEach
    void setUp() {
        tolerance = new BigDecimal("0.000001");
        matcher = new SpotLocationMatcher(tolerance);
    }
    
    @Test
    void findSpot_ExactMatch_ShouldReturnSpot() {
        BigDecimal lat = new BigDecimal("-23.561684");
        BigDecimal lng = new BigDecimal("-46.655981");
        
        ParkingSpot spot = createSpot(lat, lng);
        List<ParkingSpot> spots = List.of(spot);
        
        ParkingSpot result = matcher.findSpot(spots, lat, lng);
        
        assertEquals(spot.getId(), result.getId());
    }
    
    @Test
    void findSpot_WithinTolerance_ShouldReturnSpot() {
        BigDecimal spotLat = new BigDecimal("-23.561684");
        BigDecimal spotLng = new BigDecimal("-46.655981");
        BigDecimal searchLat = new BigDecimal("-23.5616845");
        BigDecimal searchLng = new BigDecimal("-46.6559815");
        
        ParkingSpot spot = createSpot(spotLat, spotLng);
        List<ParkingSpot> spots = List.of(spot);
        
        ParkingSpot result = matcher.findSpot(spots, searchLat, searchLng);
        
        assertEquals(spot.getId(), result.getId());
    }
    
    @Test
    void findSpot_OutsideTolerance_ShouldThrowException() {
        BigDecimal spotLat = new BigDecimal("-23.561684");
        BigDecimal spotLng = new BigDecimal("-46.655981");
        BigDecimal searchLat = new BigDecimal("-23.000000");
        BigDecimal searchLng = new BigDecimal("-46.000000");
        
        ParkingSpot spot = createSpot(spotLat, spotLng);
        List<ParkingSpot> spots = List.of(spot);
        
        assertThrows(SpotNotFoundException.class, () -> {
            matcher.findSpot(spots, searchLat, searchLng);
        });
    }
    
    @Test
    void findSpot_NoSpots_ShouldThrowException() {
        List<ParkingSpot> spots = new ArrayList<>();
        BigDecimal lat = new BigDecimal("-23.561684");
        BigDecimal lng = new BigDecimal("-46.655981");
        
        assertThrows(SpotNotFoundException.class, () -> {
            matcher.findSpot(spots, lat, lng);
        });
    }
    
    @Test
    void findSpot_MultipleMatches_ShouldThrowException() {
        BigDecimal lat = new BigDecimal("-23.561684");
        BigDecimal lng = new BigDecimal("-46.655981");
        
        ParkingSpot spot1 = createSpot(lat, lng);
        ParkingSpot spot2 = createSpot(lat, lng);
        List<ParkingSpot> spots = List.of(spot1, spot2);
        
        assertThrows(AmbiguousSpotMatchException.class, () -> {
            matcher.findSpot(spots, lat, lng);
        });
    }
    
    @Test
    void findSpot_AtToleranceBoundary_ShouldReturnSpot() {
        BigDecimal spotLat = new BigDecimal("-23.561684");
        BigDecimal spotLng = new BigDecimal("-46.655981");
        BigDecimal searchLat = spotLat.add(tolerance);
        BigDecimal searchLng = spotLng.add(tolerance);
        
        ParkingSpot spot = createSpot(spotLat, spotLng);
        List<ParkingSpot> spots = List.of(spot);
        
        ParkingSpot result = matcher.findSpot(spots, searchLat, searchLng);
        
        assertEquals(spot.getId(), result.getId());
    }
    
    private ParkingSpot createSpot(BigDecimal lat, BigDecimal lng) {
        ParkingSpot spot = new ParkingSpot();
        spot.setId(UUID.randomUUID());
        spot.setLatitude(lat);
        spot.setLongitude(lng);
        spot.setIsOccupied(false);
        return spot;
    }
}
