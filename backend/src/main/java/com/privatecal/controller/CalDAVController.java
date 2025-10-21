package com.privatecal.controller;

import com.privatecal.dto.TaskRequest;
import com.privatecal.dto.TaskResponse;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import com.privatecal.service.CalDAVService;
import com.privatecal.service.TaskService;
import com.privatecal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for CalDAV calendar import/export operations
 * Handles iCalendar (.ics) format for external calendar integration
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalDAVController {

    private static final Logger logger = LoggerFactory.getLogger(CalDAVController.class);

    private final CalDAVService calDAVService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final UserService userService;

    /**
     * Export user's calendar to iCalendar format (.ics)
     * GET /api/calendar/export
     *
     * @return iCalendar file as downloadable attachment
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCalendar() {
        logger.info("Calendar export requested");

        try {
            // Get current user
            User currentUser = userService.getCurrentUser();

            // Get all tasks for user
            List<Task> tasks = taskRepository.findByUserOrderByStartDatetimeAsc(currentUser);
            logger.info("Exporting {} tasks for user: {}", tasks.size(), currentUser.getUsername());

            // Generate calendar name
            String calendarName = calDAVService.generateCalendarName(currentUser);

            // Export to ICS
            byte[] icsData = calDAVService.exportToICS(tasks, calendarName);

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("privatecal_%s_%s.ics",
                currentUser.getUsername(), timestamp);

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/calendar"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            logger.info("Calendar export successful: {} bytes", icsData.length);
            return ResponseEntity.ok()
                .headers(headers)
                .body(icsData);

        } catch (Exception e) {
            logger.error("Error exporting calendar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Import calendar from iCalendar file (.ics)
     * POST /api/calendar/import
     *
     * @param file ICS file to import
     * @return Import summary with statistics
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCalendar(
            @RequestParam("file") MultipartFile file) {

        logger.info("Calendar import requested: {}", file.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file extension
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".ics") && !filename.endsWith(".ical"))) {
                response.put("success", false);
                response.put("error", "Invalid file format. Only .ics or .ical files are supported");
                return ResponseEntity.badRequest().body(response);
            }

            // Get current user
            User currentUser = userService.getCurrentUser();

            // Parse ICS file
            List<TaskRequest> taskRequests;
            try {
                taskRequests = calDAVService.importFromICS(file.getInputStream(), currentUser);
                logger.info("Parsed {} tasks from ICS file", taskRequests.size());
            } catch (IOException e) {
                logger.error("Error parsing ICS file: {}", e.getMessage(), e);
                response.put("success", false);
                response.put("error", "Failed to parse calendar file: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            // Import tasks
            int successCount = 0;
            int failedCount = 0;
            List<TaskResponse> importedTasks = new ArrayList<>();

            for (int i = 0; i < taskRequests.size(); i++) {
                TaskRequest taskRequest = taskRequests.get(i);
                try {
                    // Validate and clean task request
                    taskRequest.clean();

                    // Create task
                    TaskResponse taskResponse = taskService.createTask(taskRequest);
                    importedTasks.add(taskResponse);
                    successCount++;

                    logger.debug("Imported task {}/{}: {}", i + 1, taskRequests.size(), taskRequest.getTitle());

                } catch (Exception e) {
                    failedCount++;
                    String errorMsg = String.format("Task '%s': %s",
                        taskRequest.getTitle(), e.getMessage());
                    errors.add(errorMsg);
                    logger.warn("Failed to import task: {}", errorMsg);
                }
            }

            // Prepare response
            response.put("success", true);
            response.put("totalParsed", taskRequests.size());
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("importedTasks", importedTasks);

            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            if (!warnings.isEmpty()) {
                response.put("warnings", warnings);
            }

            logger.info("Calendar import completed: {} success, {} failed",
                successCount, failedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error importing calendar: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Import failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get import/export statistics
     * GET /api/calendar/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCalendarStats() {
        try {
            User currentUser = userService.getCurrentUser();
            long taskCount = taskRepository.countByUser(currentUser);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", taskCount);
            stats.put("calendarName", calDAVService.generateCalendarName(currentUser));
            stats.put("exportAvailable", true);
            stats.put("importAvailable", true);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting calendar stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
