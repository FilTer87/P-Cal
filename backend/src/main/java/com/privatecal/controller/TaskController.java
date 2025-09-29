package com.privatecal.controller;

import com.privatecal.dto.TaskRequest;
import com.privatecal.dto.TaskResponse;
import com.privatecal.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for task management endpoints
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management endpoints for creating, reading, updating and deleting calendar tasks")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;
    
    /**
     * Create a new task
     * POST /api/tasks
     */
    @Operation(
        summary = "Create Task", 
        description = "Create a new calendar task with title, description, start/end time, and optional reminders"
    )
    @ApiResponse(responseCode = "201", description = "Task created successfully", 
                content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid task data or time conflict")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Parameter(description = "Task details", required = true)
            @Valid @RequestBody TaskRequest taskRequest) {
        try {
            logger.debug("Creating new task: {}", taskRequest.getTitle());
            
            TaskResponse response = taskService.createTask(taskRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error creating task: {}", taskRequest.getTitle(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Get task by ID
     * GET /api/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long taskId) {
        try {
            TaskResponse response = taskService.getTaskById(taskId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting task ID: {}", taskId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all tasks for current user
     * GET /api/tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        try {
            List<TaskResponse> tasks = taskService.getAllUserTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error getting all tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get tasks in date range
     * GET /api/tasks/range?startDate=...&endDate=...
     * GET /api/tasks/date-range?startDate=...&endDate=...
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<TaskResponse>> getTasksInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate) {
        try {
            // Convert date strings to Instant (start of day and end of day in UTC)
            Instant startDateTime = LocalDate.parse(startDate).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            
            List<TaskResponse> tasks = taskService.getTasksInDateRange(startDateTime, endDateTime);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error getting tasks in range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get today's tasks
     * GET /api/tasks/today
     */
    @GetMapping("/today")
    public ResponseEntity<List<TaskResponse>> getTodayTasks() {
        try {
            List<TaskResponse> tasks = taskService.getTodayTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error getting today's tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Search tasks
     * GET /api/tasks/search?q=searchTerm
     */
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTasks(@RequestParam String q) {
        try {
            List<TaskResponse> tasks = taskService.searchTasks(q);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error searching tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update task
     * PUT /api/tasks/{taskId}
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId, 
                                                  @Valid @RequestBody TaskRequest taskRequest) {
        try {
            logger.debug("Updating task ID: {}", taskId);
            
            TaskResponse response = taskService.updateTask(taskId, taskRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Delete task
     * DELETE /api/tasks/{taskId}
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long taskId) {
        try {
            logger.debug("Deleting task ID: {}", taskId);
            
            taskService.deleteTask(taskId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Task deleted successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Clone/duplicate a task
     * POST /api/tasks/{taskId}/clone
     */
    @PostMapping("/{taskId}/clone")
    public ResponseEntity<TaskResponse> cloneTask(@PathVariable Long taskId, 
                                                 @RequestBody Map<String, String> request) {
        try {
            String newStartTimeStr = request.get("newStartTime");
            
            if (newStartTimeStr == null || newStartTimeStr.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Parse as ISO instant (UTC format)
            Instant newStartTime = Instant.parse(newStartTimeStr);
            
            logger.debug("Cloning task ID: {} with new start time: {}", taskId, newStartTime);
            
            TaskResponse response = taskService.cloneTask(taskId, newStartTime);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error cloning task ID: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Get task statistics
     * GET /api/tasks/statistics
     * GET /api/tasks/stats (alias)
     */
    @GetMapping("/stats")
    public ResponseEntity<TaskService.TaskStatistics> getTaskStatistics() {
        try {
            TaskService.TaskStatistics statistics = taskService.getTaskStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting task statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    
}