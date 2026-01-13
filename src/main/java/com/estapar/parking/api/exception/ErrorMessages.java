package com.estapar.parking.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ErrorMessages {
    
    private ErrorMessages() {
        // Utility class
    }
    
    public static final String VEHICLE_ALREADY_HAS_ACTIVE_SESSION = "Vehicle %s already has an active parking session";
    public static final String GARAGE_FULL = "Garage is full - no available capacity";
    public static final String SPOT_ALREADY_OCCUPIED = "Spot %s is already occupied";
    public static final String NO_ACTIVE_SESSION = "No active parking session found for vehicle: %s";
    public static final String GARAGE_NOT_FOUND = "Garage not found: %s";
    public static final String NO_DEFAULT_GARAGE = "No default garage found. System must be initialized.";
    public static final String SECTOR_NOT_FOUND = "Sector not found: %s";
    public static final String PRICING_STRATEGY_NOT_FOUND = "No active pricing strategy found for occupancy percentage: %.2f";
    
    // Exception factory methods for common patterns
    public static ResponseStatusException notFound(String message, Object... args) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(message, args));
    }
    
    public static ResponseStatusException conflict(String message, Object... args) {
        return new ResponseStatusException(HttpStatus.CONFLICT, String.format(message, args));
    }
    
    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
