package com.estapar.parking.api.exception;

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
}
