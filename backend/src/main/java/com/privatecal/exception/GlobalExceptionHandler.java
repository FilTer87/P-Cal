package com.privatecal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API errors
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Validation Failed",
            "message", "Invalid input data",
            "status", HttpStatus.BAD_REQUEST.value(),
            "path", request.getDescription(false).substring(4), // Remove "uri=" prefix
            "timestamp", Instant.now().toString(),
            "validationErrors", fieldErrors
        );
        
        logger.warn("Validation failed for request {}: {}", request.getDescription(false), fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Bad Request",
            "message", ex.getMessage(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "path", request.getDescription(false).substring(4),
            "timestamp", Instant.now().toString()
        );
        
        logger.warn("Bad request for {}: {}", request.getDescription(false), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle runtime exceptions (business logic errors)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        // Check if it's an authentication/authorization error
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        if (message != null) {
            if (message.toLowerCase().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (message.toLowerCase().contains("unauthorized") || 
                       message.toLowerCase().contains("access denied")) {
                status = HttpStatus.FORBIDDEN;
            } else if (message.toLowerCase().contains("validation") ||
                       message.toLowerCase().contains("invalid") ||
                       message.toLowerCase().contains("required") ||
                       message.toLowerCase().contains("conflict")) {
                status = HttpStatus.BAD_REQUEST;
            }
        }
        
        Map<String, Object> errorResponse = Map.of(
            "error", status.getReasonPhrase(),
            "message", message != null ? message : "An error occurred",
            "status", status.value(),
            "path", request.getDescription(false).substring(4),
            "timestamp", Instant.now().toString()
        );
        
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            logger.error("Internal server error for {}: {}", request.getDescription(false), ex.getMessage(), ex);
        } else {
            logger.warn("Business logic error for {}: {} - {}", request.getDescription(false), status.value(), ex.getMessage());
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Internal Server Error",
            "message", "An unexpected error occurred",
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "path", request.getDescription(false).substring(4),
            "timestamp", Instant.now().toString()
        );
        
        logger.error("Unexpected error for {}: {}", request.getDescription(false), ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}