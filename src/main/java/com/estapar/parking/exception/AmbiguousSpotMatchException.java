package com.estapar.parking.exception;

public class AmbiguousSpotMatchException extends RuntimeException {
    
    public AmbiguousSpotMatchException(String message) {
        super(message);
    }
    
    public AmbiguousSpotMatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
