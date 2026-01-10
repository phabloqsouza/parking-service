package com.estapar.parking.service;

import com.estapar.parking.infrastructure.persistence.entity.ParkingSpot;
import com.estapar.parking.exception.AmbiguousSpotMatchException;
import com.estapar.parking.exception.SpotNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class SpotLocationMatcher {
    
    private final BigDecimal tolerance;
    
    public SpotLocationMatcher(BigDecimal tolerance) {
        this.tolerance = tolerance;
    }
    
    public ParkingSpot findSpotByCoordinates(List<ParkingSpot> spots, BigDecimal latitude, BigDecimal longitude) {
        List<ParkingSpot> matchingSpots = spots.stream()
                .filter(spot -> isWithinTolerance(spot.getLatitude(), latitude) 
                        && isWithinTolerance(spot.getLongitude(), longitude))
                .collect(Collectors.toList());
        
        if (matchingSpots.isEmpty()) {
            throw new SpotNotFoundException(
                String.format("No parking spot found within tolerance for coordinates (%.8f, %.8f)", 
                             latitude, longitude));
        }
        
        if (matchingSpots.size() > 1) {
            throw new AmbiguousSpotMatchException(
                String.format("Multiple parking spots (%d) found within tolerance for coordinates (%.8f, %.8f). " +
                             "Ambiguous match requires investigation or stricter tolerance.", 
                             matchingSpots.size(), latitude, longitude));
        }
        
        return matchingSpots.get(0);
    }
    
    private boolean isWithinTolerance(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null) {
            return false;
        }
        BigDecimal difference = value1.subtract(value2).abs();
        return difference.compareTo(tolerance) <= 0;
    }
}
