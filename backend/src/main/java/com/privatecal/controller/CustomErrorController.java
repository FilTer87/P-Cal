package com.privatecal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public ResponseEntity<?> handleError(HttpServletRequest request) {
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

        // Check if client expects XML (for CalDAV/WebDAV clients like Thunderbird)
        String acceptHeader = request.getHeader("Accept");
        boolean expectsXml = acceptHeader != null &&
            (acceptHeader.contains("application/xml") ||
             acceptHeader.contains("text/xml") ||
             acceptHeader.contains("application/davmount+xml"));

        // Return XML for CalDAV clients, JSON otherwise
        if (expectsXml) {
            String xmlError = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<error>\n" +
                "  <status>%d</status>\n" +
                "  <error>%s</error>\n" +
                "  <message>%s</message>\n" +
                "  <path>%s</path>\n" +
                "  <timestamp>%s</timestamp>\n" +
                "</error>",
                status.value(),
                escapeXml(status.getReasonPhrase()),
                escapeXml(errorMessage != null ? errorMessage : "An error occurred"),
                escapeXml(requestUri != null ? requestUri : request.getRequestURI()),
                Instant.now().toString()
            );
            return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlError);
        }

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Escape XML special characters to prevent XML injection
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}