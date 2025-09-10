package com.privatecal.controller;

import com.privatecal.dto.TaskRequest;
import com.privatecal.dto.TaskResponse;
import com.privatecal.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for task management endpoints
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    
    @Autowired
    private TaskService taskService;
    
    /**
     * Create a new task
     * POST /api/tasks
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest taskRequest) {
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
    @GetMapping({"/range", "/date-range"})
    public ResponseEntity<List<TaskResponse>> getTasksInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate) {
        try {
            // Convert date strings to LocalDateTime (start of day and end of day)
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            
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
     * Get upcoming tasks
     * GET /api/tasks/upcoming?limit=10
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<TaskResponse>> getUpcomingTasks(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TaskResponse> tasks = taskService.getUpcomingTasks(limit);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error getting upcoming tasks", e);
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
     * Delete multiple tasks
     * DELETE /api/tasks
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteTasks(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> taskIds = request.get("taskIds");
            
            if (taskIds == null || taskIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Task IDs are required"));
            }
            
            logger.debug("Deleting {} tasks", taskIds.size());
            
            taskService.deleteTasks(taskIds);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tasks deleted successfully",
                "deletedCount", taskIds.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting multiple tasks", e);
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
            
            LocalDateTime newStartTime = LocalDateTime.parse(newStartTimeStr);
            
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
    @GetMapping({"/statistics", "/stats"})
    public ResponseEntity<TaskService.TaskStatistics> getTaskStatistics() {
        try {
            TaskService.TaskStatistics statistics = taskService.getTaskStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting task statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get overdue tasks
     * GET /api/tasks/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks() {
        try {
            List<TaskResponse> tasks = taskService.getOverdueTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error getting overdue tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get task count
     * GET /api/tasks/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getTaskCount() {
        try {
            long count = taskService.getTaskCountForUser();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            logger.error("Error getting task count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error"));
        }
    }
    
    /**
     * Check for time conflicts
     * POST /api/tasks/check-conflict
     */
    @PostMapping("/check-conflict")
    public ResponseEntity<Map<String, Object>> checkTimeConflict(@RequestBody Map<String, Object> request) {
        try {
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");
            Long excludeTaskId = request.containsKey("excludeTaskId") ? 
                    Long.valueOf(request.get("excludeTaskId").toString()) : null;
            
            if (startTimeStr == null || endTimeStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Start time and end time are required"));
            }
            
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            boolean hasConflict = taskService.hasTasksInDateRange(startTime, endTime);
            
            return ResponseEntity.ok(Map.of(
                "hasConflict", hasConflict,
                "message", hasConflict ? "Time conflict detected" : "No conflicts found"
            ));
            
        } catch (Exception e) {
            logger.error("Error checking time conflict", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error"));
        }
    }
}