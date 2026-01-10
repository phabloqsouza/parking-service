package com.estapar.parking.api.exception;

import com.estapar.parking.exception.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(SectorFullException.class)
    public ResponseEntity<ErrorResponse> handleSectorFullException(SectorFullException ex) {
        logger.warn("Sector full exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "SECTOR_FULL",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(SpotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSpotNotFoundException(SpotNotFoundException ex) {
        logger.warn("Spot not found exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "SPOT_NOT_FOUND",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(AmbiguousSpotMatchException.class)
    public ResponseEntity<ErrorResponse> handleAmbiguousSpotMatchException(AmbiguousSpotMatchException ex) {
        logger.error("Ambiguous spot match exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "AMBIGUOUS_SPOT_MATCH",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(SpotAlreadyOccupiedException.class)
    public ResponseEntity<ErrorResponse> handleSpotAlreadyOccupiedException(SpotAlreadyOccupiedException ex) {
        logger.warn("Spot already occupied exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "SPOT_ALREADY_OCCUPIED",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(ParkingSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleParkingSessionNotFoundException(ParkingSessionNotFoundException ex) {
        logger.warn("Parking session not found exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "PARKING_SESSION_NOT_FOUND",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "CONCURRENT_MODIFICATION",
                "The resource was modified by another transaction. Please retry.",
                Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        logger.error("Illegal state exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "INVALID_STATE",
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected exception: ", ex);
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private Instant timestamp;
    }
}
