package com.privatecal.controller;

import com.privatecal.entity.Calendar;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import com.privatecal.service.CalDAVService;
import com.privatecal.service.CalendarService;
import com.privatecal.service.UserService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * CalDAV Server Controller
 * Implements RFC 4791 (CalDAV) endpoints for calendar synchronization
 *
 * URL Structure: /caldav/{username}/{calendar-slug}/
 *
 * Supported Methods:
 * - GET    /caldav/{username}/{calendar}/{eventId}.ics  → Get single event
 * - PUT    /caldav/{username}/{calendar}/{eventId}.ics  → Create/Update event
 * - DELETE /caldav/{username}/{calendar}/{eventId}.ics  → Delete event
 * - PROPFIND /caldav/{username}/{calendar}/             → List events (WebDAV)
 * - OPTIONS  /caldav/{username}/{calendar}/             → Declare CalDAV support
 */
@RestController
@RequestMapping("/caldav")
@RequiredArgsConstructor
public class CalDAVServerController {

    private static final Logger logger = LoggerFactory.getLogger(CalDAVServerController.class);

    private final CalDAVService calDAVService;
    private final CalendarService calendarService;
    private final UserService userService;
    private final TaskRepository taskRepository;

    /**
     * GET /caldav/{username}/{calendar}/{eventId}.ics
     * Retrieve single event in iCalendar format
     */
    @GetMapping(value = "/{username}/{calendarSlug}/{eventId}.ics",
                produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> getEvent(
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @PathVariable Long eventId) {

        logger.info("CalDAV GET: /{}/{}/{}.ics", username, calendarSlug, eventId);

        try {
            // Validate current user matches username
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                logger.warn("Unauthorized CalDAV access attempt: user {} tried to access {}",
                    currentUser.getUsername(), username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists and belongs to user
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Verify task exists and belongs to this calendar
            Task task = taskRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            if (!task.getCalendar().getId().equals(calendar.getId())) {
                logger.warn("Event {} does not belong to calendar {}/{}", eventId, username, calendarSlug);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Export task as ICS
            String icsContent = calDAVService.exportTaskAsICS(eventId);
            String etag = calDAVService.getTaskETag(eventId);

            logger.info("CalDAV GET successful: event {} exported", eventId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                    .header("ETag", "\"" + etag + "\"")
                    .header("Content-Disposition", "inline; filename=\"event-" + eventId + ".ics\"")
                    .body(icsContent);

        } catch (Exception e) {
            logger.error("Error handling CalDAV GET: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * PUT /caldav/{username}/{calendar}/{eventId}.ics
     * Create or update event from iCalendar data
     *
     * Supports:
     * - Creating new events
     * - Updating existing events (matched by UID in ICS content)
     * - ETag-based conflict detection via If-Match header
     */
    @PutMapping(value = "/{username}/{calendarSlug}/{eventId}.ics",
                consumes = "text/calendar")
    public ResponseEntity<?> putEvent(
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @PathVariable Long eventId,
            @RequestBody String icsContent,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {

        logger.info("CalDAV PUT: /{}/{}/{}.ics", username, calendarSlug, eventId);

        try {
            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                logger.warn("Unauthorized CalDAV PUT attempt: user {} tried to access {}",
                    currentUser.getUsername(), username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Extract ETag from If-Match header (format: "etag-value")
            String expectedETag = null;
            if (ifMatch != null && !ifMatch.isEmpty()) {
                expectedETag = ifMatch.replaceAll("^\"|\"$", ""); // Remove surrounding quotes
                logger.debug("CalDAV PUT If-Match header: raw='{}', cleaned='{}'", ifMatch, expectedETag);
            }

            // Check if task with this ID already exists (CalDAV: URL identifies the resource)
            Optional<Task> existingTaskById = taskRepository.findByIdAndUser(eventId, currentUser);

            // If If-Match header is present and task exists at this URL, verify ETag
            if (expectedETag != null && existingTaskById.isPresent()) {
                String currentETag = calDAVService.getTaskETag(eventId);
                logger.debug("CalDAV PUT ETag comparison: expected='{}' (len={}), current='{}' (len={})",
                    expectedETag, expectedETag.length(), currentETag, currentETag.length());

                if (!currentETag.equals(expectedETag)) {
                    logger.warn("CalDAV PUT ETag mismatch: expected '{}' but current is '{}'", expectedETag, currentETag);
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .body("ETag mismatch - conflict detected");
                }
                logger.info("CalDAV PUT ETag match verified for task {}", eventId);
            }

            // Import/update event (matches by UID or creates new)
            Task task = calDAVService.importSingleEventFromICS(icsContent, calendar, currentUser, eventId, expectedETag);

            // Generate new ETag for response
            String newETag = calDAVService.getTaskETag(task.getId());

            logger.info("CalDAV PUT successful: task {} (UID: {})", task.getId(), task.getUid());

            // Return 201 Created for new events, 204 No Content for updates
            boolean isNewTask = task.getCreatedAt().equals(task.getUpdatedAt());
            if (isNewTask) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header("ETag", "\"" + newETag + "\"")
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .header("ETag", "\"" + newETag + "\"")
                        .build();
            }

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("ETag mismatch")) {
                logger.warn("CalDAV PUT conflict: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                        .body("ETag mismatch - conflict detected");
            }
            logger.error("Error handling CalDAV PUT: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error handling CalDAV PUT: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid ICS content: " + e.getMessage());
        }
    }

    /**
     * DELETE /caldav/{username}/{calendar}/{eventId}.ics
     * Delete event
     */
    @DeleteMapping("/{username}/{calendarSlug}/{eventId}.ics")
    public ResponseEntity<?> deleteEvent(
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @PathVariable Long eventId) {

        logger.info("CalDAV DELETE: /{}/{}/{}.ics", username, calendarSlug, eventId);

        try {
            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Verify task exists and belongs to calendar
            Task task = taskRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            if (!task.getCalendar().getId().equals(calendar.getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Delete task
            taskRepository.delete(task);

            logger.info("CalDAV DELETE successful: event {} deleted", eventId);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error handling CalDAV DELETE: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * OPTIONS /caldav/{username}/{calendar}/
     * Declare CalDAV support (RFC 4791)
     */
    @RequestMapping(value = "/{username}/{calendarSlug}",
                    method = RequestMethod.OPTIONS)
    public ResponseEntity<?> options() {
        logger.debug("CalDAV OPTIONS request");

        return ResponseEntity.ok()
                .header("DAV", "1, 3, calendar-access")  // CalDAV capability
                .header("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, PROPFIND")
                .build();
    }

    /**
     * PROPFIND /caldav/{username}/{calendar}/
     * WebDAV method to list calendar events
     *
     * Note: This is a simplified implementation. Full CalDAV clients expect
     * XML responses with WebDAV properties. For now, returns basic calendar info.
     *
     * Spring doesn't have PROPFIND as a standard method, so we handle it
     * via custom filter (WebDAVMethodFilter) and check method manually.
     */
    @RequestMapping(value = "/{username}/{calendarSlug}",
                    produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> propfind(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @RequestBody(required = false) String propfindXML,
            @RequestHeader(value = "Depth", defaultValue = "1") String depth) {

        // Only handle PROPFIND method
        if (!"PROPFIND".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        logger.info("CalDAV PROPFIND: /{}/{} (depth={})", username, calendarSlug, depth);

        try {
            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Get all tasks in this calendar
            List<Task> tasks = taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(calendar.getId());

            // Build simplified WebDAV multistatus response
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");

            // Calendar collection resource
            xml.append("  <D:response>\n");
            xml.append("    <D:href>/caldav/").append(username).append("/").append(calendarSlug).append("/</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:resourcetype><D:collection/><C:calendar/></D:resourcetype>\n");
            xml.append("        <D:displayname>").append(calendar.getName()).append("</D:displayname>\n");
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");

            // Individual events (if depth > 0)
            if (!"0".equals(depth)) {
                for (Task task : tasks) {
                    String etag = calDAVService.getTaskETag(task.getId());
                    xml.append("  <D:response>\n");
                    xml.append("    <D:href>/caldav/").append(username).append("/").append(calendarSlug)
                       .append("/").append(task.getId()).append(".ics</D:href>\n");
                    xml.append("    <D:propstat>\n");
                    xml.append("      <D:prop>\n");
                    xml.append("        <D:getetag>\"").append(etag).append("\"</D:getetag>\n");
                    xml.append("        <D:getcontenttype>text/calendar; component=VEVENT</D:getcontenttype>\n");
                    xml.append("      </D:prop>\n");
                    xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
                    xml.append("    </D:propstat>\n");
                    xml.append("  </D:response>\n");
                }
            }

            xml.append("</D:multistatus>");

            logger.info("CalDAV PROPFIND successful: {} events listed", tasks.size());

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml.toString());

        } catch (Exception e) {
            logger.error("Error handling CalDAV PROPFIND: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
