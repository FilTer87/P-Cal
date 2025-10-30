package com.privatecal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

/**
 * Exception handler specifically for CalDAV endpoints (/caldav/**)
 * Returns XML responses instead of JSON for WebDAV/CalDAV clients
 */
@ControllerAdvice(basePackages = "com.privatecal.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)  // Handle before GlobalExceptionHandler
public class CalDAVExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CalDAVExceptionHandler.class);

    /**
     * Handle HttpMediaTypeNotAcceptableException for CalDAV endpoints
     * This occurs when CalDAV clients request XML but Spring tries to return JSON
     * @throws HttpMediaTypeNotAcceptableException
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            jakarta.servlet.http.HttpServletRequest request) throws HttpMediaTypeNotAcceptableException {

        String requestUri = request.getRequestURI();

        logger.info("CalDAVExceptionHandler.handleMediaTypeNotAcceptable called for URI: {}", requestUri);

        // Only handle CalDAV endpoints
        if (!requestUri.startsWith("/caldav")) {
            logger.info("Not a CalDAV endpoint, re-throwing exception");
            // Let GlobalExceptionHandler handle it
            throw ex;
        }

        logger.warn("CalDAV MediaTypeNotAcceptable for {}: {}", requestUri, ex.getMessage());

        // Return XML error response for CalDAV clients
        String xmlError = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<error>\n" +
            "  <status>%d</status>\n" +
            "  <error>%s</error>\n" +
            "  <message>%s</message>\n" +
            "  <path>%s</path>\n" +
            "  <timestamp>%s</timestamp>\n" +
            "</error>",
            HttpStatus.NOT_ACCEPTABLE.value(),
            escapeXml(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase()),
            escapeXml("The requested resource is not available in the requested format"),
            escapeXml(requestUri),
            Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlError);
    }

    /**
     * Handle all other exceptions for CalDAV endpoints
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleCalDAVException(
            Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {

        String requestUri = request.getRequestURI();

        // Only handle CalDAV endpoints
        if (!requestUri.startsWith("/caldav")) {
            // Let GlobalExceptionHandler handle it
            throw new RuntimeException(ex);
        }

        logger.error("CalDAV error for {}: {}", requestUri, ex.getMessage(), ex);

        // Determine HTTP status
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        }

        // Return XML error response for CalDAV clients
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
            escapeXml(ex.getMessage() != null ? ex.getMessage() : "An error occurred"),
            escapeXml(requestUri),
            Instant.now().toString()
        );

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlError);
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
