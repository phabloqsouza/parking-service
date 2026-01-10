package com.estapar.parking.exception;

public class SpotAlreadyOccupiedException extends RuntimeException {
    
    public SpotAlreadyOccupiedException(String message) {
        super(message);
    }
    
    public SpotAlreadyOccupiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
