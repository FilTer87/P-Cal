package com.privatecal.controller;

import com.privatecal.dto.CalendarRequest;
import com.privatecal.dto.CalendarResponse;
import com.privatecal.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for calendar management endpoints
 */
@RestController
@RequestMapping("/api/calendars")
@Tag(name = "Calendars", description = "Calendar management endpoints for creating, reading, updating and deleting user calendars")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class CalendarController {

    private static final Logger logger = LoggerFactory.getLogger(CalendarController.class);

    private final CalendarService calendarService;

    /**
     * Get all calendars for current user
     * GET /api/calendars
     */
    @Operation(
        summary = "Get All Calendars",
        description = "Retrieve all calendars for the authenticated user, ordered by default status and creation date"
    )
    @ApiResponse(responseCode = "200", description = "Calendars retrieved successfully",
                content = @Content(schema = @Schema(implementation = CalendarResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping
    public ResponseEntity<List<CalendarResponse>> getAllCalendars() {
        logger.debug("Fetching all calendars for current user");
        List<CalendarResponse> calendars = calendarService.getAllCalendars();
        return ResponseEntity.ok(calendars);
    }

    /**
     * Get calendar by ID
     * GET /api/calendars/{id}
     */
    @Operation(
        summary = "Get Calendar by ID",
        description = "Retrieve a specific calendar by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Calendar retrieved successfully",
                content = @Content(schema = @Schema(implementation = CalendarResponse.class)))
    @ApiResponse(responseCode = "404", description = "Calendar not found")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping("/{id}")
    public ResponseEntity<CalendarResponse> getCalendarById(
            @Parameter(description = "Calendar ID", required = true)
            @PathVariable Long id) {
        try {
            logger.debug("Fetching calendar with ID: {}", id);
            CalendarResponse calendar = calendarService.getCalendarById(id);
            return ResponseEntity.ok(calendar);
        } catch (RuntimeException e) {
            logger.error("Calendar not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get default calendar for current user
     * GET /api/calendars/default
     */
    @Operation(
        summary = "Get Default Calendar",
        description = "Retrieve the default calendar for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Default calendar retrieved successfully",
                content = @Content(schema = @Schema(implementation = CalendarResponse.class)))
    @ApiResponse(responseCode = "404", description = "Default calendar not found")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping("/default")
    public ResponseEntity<CalendarResponse> getDefaultCalendar() {
        try {
            logger.debug("Fetching default calendar for current user");
            CalendarResponse calendar = calendarService.getDefaultCalendar();
            return ResponseEntity.ok(calendar);
        } catch (RuntimeException e) {
            logger.error("Default calendar not found");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new calendar
     * POST /api/calendars
     */
    @Operation(
        summary = "Create Calendar",
        description = "Create a new calendar for the authenticated user"
    )
    @ApiResponse(responseCode = "201", description = "Calendar created successfully",
                content = @Content(schema = @Schema(implementation = CalendarResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid calendar data or slug already exists")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @PostMapping
    public ResponseEntity<?> createCalendar(
            @Parameter(description = "Calendar details", required = true)
            @Valid @RequestBody CalendarRequest calendarRequest) {
        try {
            logger.debug("Creating new calendar: {}", calendarRequest.getName());
            CalendarResponse response = calendarService.createCalendar(calendarRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error creating calendar: {}", calendarRequest.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update an existing calendar
     * PUT /api/calendars/{id}
     */
    @Operation(
        summary = "Update Calendar",
        description = "Update an existing calendar"
    )
    @ApiResponse(responseCode = "200", description = "Calendar updated successfully",
                content = @Content(schema = @Schema(implementation = CalendarResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid calendar data or slug conflict")
    @ApiResponse(responseCode = "404", description = "Calendar not found")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCalendar(
            @Parameter(description = "Calendar ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated calendar details", required = true)
            @Valid @RequestBody CalendarRequest calendarRequest) {
        try {
            logger.debug("Updating calendar with ID: {}", id);
            CalendarResponse response = calendarService.updateCalendar(id, calendarRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating calendar with ID: {}", id, e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete a calendar
     * DELETE /api/calendars/{id}
     */
    @Operation(
        summary = "Delete Calendar",
        description = "Delete a calendar. Tasks will be reassigned to the default calendar."
    )
    @ApiResponse(responseCode = "204", description = "Calendar deleted successfully")
    @ApiResponse(responseCode = "400", description = "Cannot delete the only calendar or default calendar with tasks")
    @ApiResponse(responseCode = "404", description = "Calendar not found")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCalendar(
            @Parameter(description = "Calendar ID", required = true)
            @PathVariable Long id) {
        try {
            logger.debug("Deleting calendar with ID: {}", id);
            calendarService.deleteCalendar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting calendar with ID: {}", id, e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Simple error response DTO for better error handling
     */
    private record ErrorResponse(String message) {}
}
