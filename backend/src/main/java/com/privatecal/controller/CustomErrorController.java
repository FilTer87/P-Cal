package com.privatecal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

/**
 * Custom error controller to handle errors properly without Spring Security interference
 */
@RestController
public class CustomErrorController implements ErrorController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    
    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        // Get the original error status using correct attribute names
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String errorMessage = (String) request.getAttribute("jakarta.servlet.error.message");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        
        // Fallback to legacy names if jakarta attributes are not found
        if (statusCode == null) {
            statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        }
        if (errorMessage == null) {
            errorMessage = (String) request.getAttribute("javax.servlet.error.message");
        }
        if (requestUri == null) {
            requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
        }
        if (exception == null) {
            exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
        }
        
        // Default to 500 if no status code is found
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (statusCode != null) {
            try {
                status = HttpStatus.valueOf(statusCode);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid HTTP status code: {}", statusCode);
            }
        }
        
        // Build error response
        Map<String, Object> errorResponse = Map.of(
            "error", status.getReasonPhrase(),
            "message", errorMessage != null ? errorMessage : "An error occurred",
            "status", status.value(),
            "path", requestUri != null ? requestUri : request.getRequestURI(),
            "timestamp", Instant.now().toString()
        );
        
        // Log the error for debugging
        if (exception != null) {
            logger.error("Error handling request to {}: {}", requestUri, exception.getMessage(), exception);
        } else {
            logger.warn("Error handling request to {}: {} - {}", requestUri, status.value(), errorMessage);
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }
}