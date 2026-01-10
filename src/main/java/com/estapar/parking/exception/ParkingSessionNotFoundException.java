package com.estapar.parking.exception;

public class ParkingSessionNotFoundException extends RuntimeException {
    
    public ParkingSessionNotFoundException(String message) {
        super(message);
    }
    
    public ParkingSessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
