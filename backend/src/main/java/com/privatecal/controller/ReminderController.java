package com.privatecal.controller;

import com.privatecal.dto.ReminderRequest;
import com.privatecal.dto.ReminderResponse;
import com.privatecal.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for reminder management endpoints
 */
@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Reminders", description = "Reminder management endpoints for creating and managing task notifications")
@SecurityRequirement(name = "Bearer Authentication")
public class ReminderController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReminderController.class);
    
    @Autowired
    private ReminderService reminderService;
    
    /**
     * Get all reminders for current user
     * GET /api/reminders
     */
    @GetMapping
    public ResponseEntity<List<ReminderResponse>> getAllReminders() {
        try {
            List<ReminderResponse> reminders = reminderService.getAllUserReminders();
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            logger.error("Error getting all reminders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create reminder for a task
     * POST /api/reminders/task/{taskId}
     */
    @Operation(
        summary = "Create Task Reminder", 
        description = "Create a notification reminder for a specific task. The reminder will be triggered at the specified time before the task starts."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reminder created successfully", 
                    content = @Content(schema = @Schema(implementation = ReminderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid reminder data"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/task/{taskId}")
    public ResponseEntity<ReminderResponse> createReminderForTask(
            @Parameter(description = "Task ID", required = true) @PathVariable Long taskId, 
            @Parameter(description = "Reminder details", required = true) @Valid @RequestBody ReminderRequest reminderRequest) {
        try {
            logger.debug("Creating reminder for task ID: {}", taskId);
            
            ReminderResponse response = reminderService.createReminderForTask(taskId, reminderRequest);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error creating reminder for task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    
    /**
     * Get all reminders for a task
     * GET /api/reminders/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<ReminderResponse>> getRemindersForTask(@PathVariable Long taskId) {
        try {
            List<ReminderResponse> reminders = reminderService.getRemindersForTask(taskId);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            logger.error("Error getting reminders for task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get upcoming reminders for current user
     * GET /api/reminders/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<ReminderResponse>> getUpcomingReminders() {
        try {
            List<ReminderResponse> reminders = reminderService.getUpcomingReminders();
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            logger.error("Error getting upcoming reminders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get reminders in date range
     * GET /api/reminders/range?startDate=...&endDate=...
     */
    @GetMapping("/range")
    public ResponseEntity<List<ReminderResponse>> getRemindersInRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // Parse ISO instant strings to Instant
            Instant startInstant = Instant.parse(startDate);
            Instant endInstant = Instant.parse(endDate);
            List<ReminderResponse> reminders = reminderService.getRemindersInDateRange(startInstant, endInstant);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            logger.error("Error getting reminders in range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Update reminder
     * PUT /api/reminders/{reminderId}
     */
    @PutMapping("/{reminderId}")
    public ResponseEntity<ReminderResponse> updateReminder(@PathVariable Long reminderId, 
                                                          @Valid @RequestBody ReminderRequest reminderRequest) {
        try {
            logger.debug("Updating reminder ID: {}", reminderId);
            
            ReminderResponse response = reminderService.updateReminder(reminderId, reminderRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating reminder ID: {}", reminderId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Delete reminder
     * DELETE /api/reminders/{reminderId}
     */
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Map<String, Object>> deleteReminder(@PathVariable Long reminderId) {
        try {
            logger.debug("Deleting reminder ID: {}", reminderId);
            
            reminderService.deleteReminder(reminderId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Reminder deleted successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting reminder ID: {}", reminderId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Delete all reminders for a task
     * DELETE /api/reminders/task/{taskId}
     */
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteRemindersForTask(@PathVariable Long taskId) {
        try {
            logger.debug("Deleting all reminders for task ID: {}", taskId);
            
            reminderService.deleteRemindersForTask(taskId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All reminders deleted for task"
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting reminders for task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    
    
    /**
     * Create quick reminder presets
     * POST /api/reminders/presets/task/{taskId}
     */
    @PostMapping("/presets/task/{taskId}")
    public ResponseEntity<List<ReminderResponse>> createReminderPresets(@PathVariable Long taskId, 
                                                                       @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> presets = (List<String>) request.get("presets");
            
            if (presets == null || presets.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            logger.debug("Creating reminder presets for task ID: {}", taskId);
            
            List<ReminderResponse> createdReminders = presets.stream()
                    .map(preset -> createReminderFromPreset(taskId, preset))
                    .filter(response -> response != null)
                    .toList();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReminders);
            
        } catch (Exception e) {
            logger.error("Error creating reminder presets for task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Get available reminder presets
     * GET /api/reminders/presets
     */
    @GetMapping("/presets")
    public ResponseEntity<Map<String, Object>> getReminderPresets() {
        try {
            Map<String, Object> presets = Map.of(
                "common", List.of(
                    Map.of("name", "5 minutes before", "value", "5min", "minutes", 5),
                    Map.of("name", "10 minutes before", "value", "10min", "minutes", 10),
                    Map.of("name", "15 minutes before", "value", "15min", "minutes", 15),
                    Map.of("name", "30 minutes before", "value", "30min", "minutes", 30),
                    Map.of("name", "1 hour before", "value", "1hour", "minutes", 60),
                    Map.of("name", "2 hours before", "value", "2hours", "minutes", 120),
                    Map.of("name", "1 day before", "value", "1day", "minutes", 24 * 60),
                    Map.of("name", "1 week before", "value", "1week", "minutes", 7 * 24 * 60)
                ),
                "quick", List.of("5min", "15min", "1hour", "1day"),
                "description", "Common reminder time presets"
            );
            
            return ResponseEntity.ok(presets);
            
        } catch (Exception e) {
            logger.error("Error getting reminder presets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Helper method to create reminder from preset
     */
    private ReminderResponse createReminderFromPreset(Long taskId, String preset) {
        try {
            int minutes = switch (preset.toLowerCase()) {
                case "5min" -> 5;
                case "10min" -> 10;
                case "15min" -> 15;
                case "30min" -> 30;
                case "1hour" -> 60;
                case "2hours" -> 120;
                case "1day" -> 24 * 60;
                case "1week" -> 7 * 24 * 60;
                default -> -1;
            };
            
            if (minutes == -1) {
                return null;
            }
            
            ReminderRequest reminderRequest = new ReminderRequest(minutes);
            return reminderService.createReminderForTask(taskId, reminderRequest);
            
        } catch (Exception e) {
            logger.warn("Failed to create reminder preset {} for task {}", preset, taskId);
            return null;
        }
    }
}