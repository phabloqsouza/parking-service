package com.estapar.parking.service;

import com.estapar.parking.exception.AmbiguousSpotMatchException;
import com.estapar.parking.exception.SpotNotFoundException;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpotLocationMatcher Unit Tests")
class SpotLocationMatcherTest {

    private SpotLocationMatcher matcher;
    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    @BeforeEach
    void setUp() {
        matcher = new SpotLocationMatcher(TOLERANCE);
    }

    @Test
    @DisplayName("Should find exact matching spot")
    void shouldFindExactMatchingSpot() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spot = createSpot(UUID.randomUUID(), lat, lng);
        List<ParkingSpot> spots = List.of(spot);

        ParkingSpot result = matcher.findSpotByCoordinates(spots, lat, lng);

        assertEquals(spot.getId(), result.getId());
        assertEquals(spot.getLatitude(), result.getLatitude());
        assertEquals(spot.getLongitude(), result.getLongitude());
    }

    @Test
    @DisplayName("Should find spot within tolerance")
    void shouldFindSpotWithinTolerance() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spot = createSpot(UUID.randomUUID(), lat, lng);
        List<ParkingSpot> spots = List.of(spot);

        // Within tolerance: add 0.0000005 (half of tolerance)
        BigDecimal searchLat = lat.add(new BigDecimal("0.0000005"));
        BigDecimal searchLng = lng.add(new BigDecimal("0.0000005"));

        ParkingSpot result = matcher.findSpotByCoordinates(spots, searchLat, searchLng);

        assertEquals(spot.getId(), result.getId());
    }

    @Test
    @DisplayName("Should find spot at tolerance boundary")
    void shouldFindSpotAtToleranceBoundary() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spot = createSpot(UUID.randomUUID(), lat, lng);
        List<ParkingSpot> spots = List.of(spot);

        // Exactly at tolerance boundary
        BigDecimal searchLat = lat.add(TOLERANCE);
        BigDecimal searchLng = lng.add(TOLERANCE);

        ParkingSpot result = matcher.findSpotByCoordinates(spots, searchLat, searchLng);

        assertEquals(spot.getId(), result.getId());
    }

    @Test
    @DisplayName("Should throw SpotNotFoundException when no spot matches")
    void shouldThrowExceptionWhenNoSpotMatches() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spot = createSpot(UUID.randomUUID(), lat, lng);
        List<ParkingSpot> spots = List.of(spot);

        // Outside tolerance: add more than tolerance
        BigDecimal searchLat = lat.add(new BigDecimal("0.000002"));
        BigDecimal searchLng = lng.add(new BigDecimal("0.000002"));

        assertThrows(SpotNotFoundException.class, () -> {
            matcher.findSpotByCoordinates(spots, searchLat, searchLng);
        });
    }

    @Test
    @DisplayName("Should throw SpotNotFoundException when spots list is empty")
    void shouldThrowExceptionWhenSpotsListIsEmpty() {
        List<ParkingSpot> spots = new ArrayList<>();
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        assertThrows(SpotNotFoundException.class, () -> {
            matcher.findSpotByCoordinates(spots, lat, lng);
        });
    }

    @Test
    @DisplayName("Should throw AmbiguousSpotMatchException when multiple spots match")
    void shouldThrowExceptionWhenMultipleSpotsMatch() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spot1 = createSpot(UUID.randomUUID(), lat, lng);
        ParkingSpot spot2 = createSpot(UUID.randomUUID(), lat, lng);
        List<ParkingSpot> spots = List.of(spot1, spot2);

        assertThrows(AmbiguousSpotMatchException.class, () -> {
            matcher.findSpotByCoordinates(spots, lat, lng);
        });
    }

    @Test
    @DisplayName("Should find correct spot when multiple spots exist but only one matches")
    void shouldFindCorrectSpotWhenMultipleSpotsExist() {
        BigDecimal lat1 = new BigDecimal("-23.550520");
        BigDecimal lng1 = new BigDecimal("-46.633308");
        BigDecimal lat2 = new BigDecimal("-23.551000"); // Far away
        BigDecimal lng2 = new BigDecimal("-46.634000"); // Far away

        ParkingSpot spot1 = createSpot(UUID.randomUUID(), lat1, lng1);
        ParkingSpot spot2 = createSpot(UUID.randomUUID(), lat2, lng2);
        List<ParkingSpot> spots = List.of(spot1, spot2);

        ParkingSpot result = matcher.findSpotByCoordinates(spots, lat1, lng1);

        assertEquals(spot1.getId(), result.getId());
    }

    @Test
    @DisplayName("Should handle null coordinates in spot list gracefully")
    void shouldHandleNullCoordinatesInSpots() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spotWithNulls = new ParkingSpot();
        spotWithNulls.setId(UUID.randomUUID());
        spotWithNulls.setLatitude(null);
        spotWithNulls.setLongitude(null);

        ParkingSpot validSpot = createSpot(UUID.randomUUID(), lat, lng);
        List<ParkingSpot> spots = List.of(spotWithNulls, validSpot);

        ParkingSpot result = matcher.findSpotByCoordinates(spots, lat, lng);

        assertEquals(validSpot.getId(), result.getId());
    }

    @Test
    @DisplayName("Should match spot with slightly different coordinates within tolerance")
    void shouldMatchSpotWithSlightDifference() {
        BigDecimal lat = new BigDecimal("-23.550520");
        BigDecimal lng = new BigDecimal("-46.633308");

        ParkingSpot spot = createSpot(UUID.randomUUID(), 
                new BigDecimal("-23.5505205"), 
                new BigDecimal("-46.6333085"));
        List<ParkingSpot> spots = List.of(spot);

        ParkingSpot result = matcher.findSpotByCoordinates(spots, lat, lng);

        assertEquals(spot.getId(), result.getId());
    }

    private ParkingSpot createSpot(UUID id, BigDecimal latitude, BigDecimal longitude) {
        ParkingSpot spot = new ParkingSpot();
        spot.setId(id);
        spot.setLatitude(latitude);
        spot.setLongitude(longitude);
        spot.setSectorId(UUID.randomUUID());
        spot.setIsOccupied(false);
        spot.setVersion(0);
        return spot;
    }
}
