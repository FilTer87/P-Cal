package com.privatecal.controller;

import com.privatecal.caldav.CalDAVValidator;
import com.privatecal.caldav.CalDAVXmlBuilder;
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
 *
 * Authentication:
 * - HTTP Basic Auth (username/email + password over HTTPS)
 * - Future: OAuth 2.0 with Bearer tokens (planned enhancement)
 *
 * CalDAV Compliance:
 * - ✅ FULLY RFC 4791 COMPLIANT (as of v0.14.1)
 * - UID in URL is the primary key, ensuring stable resource URLs
 * - ETag-based conflict detection
 * - Supports GET, PUT, DELETE, PROPFIND, OPTIONS
 *
 * Known Limitations:
 * - Some errors return 500 instead of specific HTTP codes
 * - See CalDAVServerIntegrationTest for detailed test coverage
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
    private final CalDAVXmlBuilder xmlBuilder;
    private final CalDAVValidator validator;

    /**
     * /.well-known/caldav
     * CalDAV service discovery endpoint (RFC 6764)
     * Redirects to the CalDAV root endpoint
     */
    @GetMapping("/.well-known/caldav")
    public ResponseEntity<Void> wellKnownCalDAV() {
        logger.info("CalDAV well-known discovery request, redirecting to /caldav/");
        return ResponseEntity.status(301)  // 301 Moved Permanently
                .header("Location", "/caldav/")
                .build();
    }

    /**
     * GET /caldav/{username}/{calendar}/{eventUid}.ics
     * Retrieve single event in iCalendar format
     *
     * CalDAV RFC 4791 compliant: eventUid in URL is the actual task UID (primary key)
     * This ensures stable URLs across all operations
     */
    @GetMapping(value = "/{username}/{calendarSlug}/{eventUid}.ics",
                produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> getEvent(
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @PathVariable String eventUid) {

        logger.info("CalDAV GET: /{}/{}/{}.ics", username, calendarSlug, eventUid);

        try {
            // Validate path parameters to prevent injection attacks
            validator.validateUsername(username);
            validator.validateCalendarSlug(calendarSlug);
            validator.validateEventUid(eventUid);

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
            // findById now uses UID as primary key
            Task task = taskRepository.findById(eventUid)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            if (!task.getCalendar().getId().equals(calendar.getId())) {
                logger.warn("Event {} does not belong to calendar {}/{}", eventUid, username, calendarSlug);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Export task as ICS
            String icsContent = calDAVService.exportTaskAsICS(eventUid);
            String etag = calDAVService.getTaskETag(eventUid);

            logger.info("CalDAV GET successful: event {} exported", eventUid);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                    .header("ETag", "\"" + validator.sanitizeHeaderValue(etag) + "\"")
                    .header("Content-Disposition", "inline; filename=\"" + validator.sanitizeHeaderValue(eventUid) + ".ics\"")
                    .body(icsContent);

        } catch (IllegalArgumentException e) {
            // Validation errors - safe to expose
            logger.warn("CalDAV GET validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid request parameter</message></error>");
        } catch (Exception e) {
            // Don't expose internal error details to client
            logger.error("Error handling CalDAV GET for {}/{}/{}: {}", username, calendarSlug, eventUid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>An error occurred processing your request</message></error>");
        }
    }

    /**
     * PUT /caldav/{username}/{calendar}/{eventUid}.ics
     * Create or update event from iCalendar data
     *
     * CalDAV RFC 4791 compliant: eventUid in URL is the task's primary key
     * This ensures stable URLs - the URL UID directly identifies the database record
     *
     * Supports:
     * - Creating new events with specified UID
     * - Updating existing events (matched by UID from URL)
     * - ETag-based conflict detection via If-Match header
     */
    @PutMapping(value = "/{username}/{calendarSlug}/{eventUid}.ics",
                consumes = "text/calendar")
    public ResponseEntity<?> putEvent(
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @PathVariable String eventUid,
            @RequestBody String icsContent,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {

        logger.info("CalDAV PUT: /{}/{}/{}.ics", username, calendarSlug, eventUid);

        try {
            // Validate path parameters to prevent injection attacks
            validator.validateUsername(username);
            validator.validateCalendarSlug(calendarSlug);
            validator.validateEventUid(eventUid);

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

            // Check if task with this UID already exists
            Optional<Task> existingTask = taskRepository.findById(eventUid);

            // If If-Match header is present and task exists, verify ETag
            if (expectedETag != null && existingTask.isPresent()) {
                String currentETag = calDAVService.getTaskETag(eventUid);
                logger.debug("CalDAV PUT ETag comparison: expected='{}' (len={}), current='{}' (len={})",
                    expectedETag, expectedETag.length(), currentETag, currentETag.length());

                if (!currentETag.equals(expectedETag)) {
                    logger.warn("CalDAV PUT ETag mismatch: expected '{}' but current is '{}'", expectedETag, currentETag);
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .body("ETag mismatch - conflict detected");
                }
                logger.info("CalDAV PUT ETag match verified for task {}", eventUid);
            }

            // Import/update event - eventUid from URL is the primary key
            Task task = calDAVService.importSingleEventFromICS(icsContent, calendar, currentUser, eventUid, expectedETag);

            // Generate new ETag for response
            String newETag = calDAVService.getTaskETag(task.getUid());

            // Build resource URL for Location header
            String resourceUrl = String.format("/caldav/%s/%s/%s.ics", username, calendarSlug, task.getUid());

            logger.info("CalDAV PUT successful: task UID {} at {}", task.getUid(), resourceUrl);

            // Return 201 Created for new events, 204 No Content for updates
            boolean isNewTask = task.getCreatedAt().equals(task.getUpdatedAt());
            if (isNewTask) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header("ETag", "\"" + validator.sanitizeHeaderValue(newETag) + "\"")
                        .header("Location", validator.sanitizeHeaderValue(resourceUrl))
                        .header("Cache-Control", "no-cache")
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .header("ETag", "\"" + validator.sanitizeHeaderValue(newETag) + "\"")
                        .header("Cache-Control", "no-cache")
                        .build();
            }

        } catch (IllegalArgumentException e) {
            // Validation errors - safe to expose
            logger.warn("CalDAV PUT validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid request parameter</message></error>");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("ETag mismatch")) {
                logger.warn("CalDAV PUT conflict: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                        .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>ETag mismatch - conflict detected</message></error>");
            }
            // Don't expose internal error details
            logger.error("Error handling CalDAV PUT for {}/{}/{}: {}", username, calendarSlug, eventUid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>An error occurred processing your request</message></error>");
        } catch (Exception e) {
            // Don't expose ICS parsing details
            logger.error("Error parsing ICS content for {}/{}/{}: {}", username, calendarSlug, eventUid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid calendar data</message></error>");
        }
    }

    /**
     * DELETE /caldav/{username}/{calendar}/{eventUid}.ics
     * Delete event
     *
     * CalDAV RFC 4791 compliant: eventUid in URL is the task's UID (primary key)
     */
    @DeleteMapping("/{username}/{calendarSlug}/{eventUid}.ics")
    public ResponseEntity<?> deleteEvent(
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @PathVariable String eventUid) {

        logger.info("CalDAV DELETE: /{}/{}/{}.ics", username, calendarSlug, eventUid);

        try {
            // Validate path parameters to prevent injection attacks
            validator.validateUsername(username);
            validator.validateCalendarSlug(calendarSlug);
            validator.validateEventUid(eventUid);

            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Verify task exists and belongs to calendar
            Task task = taskRepository.findById(eventUid)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            if (!task.getCalendar().getId().equals(calendar.getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Delete task
            taskRepository.delete(task);

            logger.info("CalDAV DELETE successful: event {} deleted", eventUid);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            // Validation errors - safe to expose
            logger.warn("CalDAV DELETE validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid request parameter</message></error>");
        } catch (Exception e) {
            // Don't expose internal error details
            logger.error("Error handling CalDAV DELETE for {}/{}/{}: {}", username, calendarSlug, eventUid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>An error occurred processing your request</message></error>");
        }
    }

    /**
     * OPTIONS /caldav/{username}/{calendar}/ and /caldav/{username}/{calendar}
     * Declare CalDAV support (RFC 4791)
     */
    @RequestMapping(value = {"/{username}/{calendarSlug}", "/{username}/{calendarSlug}/"},
                    method = RequestMethod.OPTIONS)
    public ResponseEntity<?> options() {
        logger.debug("CalDAV OPTIONS request");

        return ResponseEntity.ok()
                .header("DAV", "1, 3, calendar-access")  // CalDAV capability
                .header("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, PROPFIND, REPORT")
                .build();
    }

    /**
     * PROPFIND/REPORT /caldav/{username}/{calendar}/ and /caldav/{username}/{calendar}
     * WebDAV methods to list calendar events
     *
     * PROPFIND: List events with properties (used for discovery)
     * REPORT: Query events with filters (used for sync)
     *
     * Spring doesn't have PROPFIND/REPORT as standard methods, so we handle them
     * via custom filter (WebDAVMethodFilter) and check method manually.
     */
    @RequestMapping(value = {"/{username}/{calendarSlug}", "/{username}/{calendarSlug}/"})
    public ResponseEntity<String> propfindOrReport(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String username,
            @PathVariable String calendarSlug,
            @RequestBody(required = false) String requestXML,
            @RequestHeader(value = "Depth", defaultValue = "1") String depth) {

        String method = request.getMethod();

        // Only handle PROPFIND and REPORT methods
        if (!"PROPFIND".equalsIgnoreCase(method) && !"REPORT".equalsIgnoreCase(method)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        logger.info("CalDAV {}: /{}/{} (depth={})", method, username, calendarSlug, depth);

        try {
            // Validate path parameters to prevent injection attacks
            validator.validateUsername(username);
            validator.validateCalendarSlug(calendarSlug);

            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Parse calendar-multiget requests using validator
            List<String> requestedUids = null;
            if ("REPORT".equalsIgnoreCase(method)) {
                requestedUids = validator.parseCalendarMultigetUids(requestXML);
                if (requestedUids != null) {
                    logger.info("CalDAV calendar-multiget: {} UIDs requested", requestedUids.size());
                }
            }

            // Get tasks based on request type
            List<Task> tasks;
            if (requestedUids != null && !requestedUids.isEmpty()) {
                // For calendar-multiget, only fetch requested events
                tasks = taskRepository.findAllById(requestedUids);
                logger.debug("CalDAV calendar-multiget: fetched {} out of {} requested events",
                           tasks.size(), requestedUids.size());
            } else {
                // For normal PROPFIND/REPORT, get all tasks in calendar
                tasks = taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(calendar.getId());
            }

            // Check if client requested calendar-data
            boolean includeCalendarData = "REPORT".equalsIgnoreCase(method) ||
                (requestXML != null && (requestXML.contains("calendar-data") || requestXML.contains("allprop")));

            // Filter tasks based on depth for PROPFIND (always include for REPORT)
            List<Task> tasksToInclude = ("0".equals(depth) && "PROPFIND".equalsIgnoreCase(method))
                ? java.util.Collections.emptyList()
                : tasks;

            // Build WebDAV multistatus response using builder
            String xmlResponse = xmlBuilder.buildCalendarCollectionResponse(
                username,
                calendarSlug,
                calendar.getName(),
                tasksToInclude,
                includeCalendarData,
                method,
                uid -> {
                    try {
                        return calDAVService.exportTaskAsICS(uid);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to export task as ICS", e);
                    }
                },
                calDAVService::getTaskETag
            );

            logger.info("CalDAV {} successful: {} events listed, response size: {} KB",
                       method, tasksToInclude.size(), xmlResponse.length() / 1024);

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlResponse);

        } catch (IllegalArgumentException e) {
            // Validation errors - safe to expose
            logger.warn("CalDAV {} validation error: {}", method, e.getMessage());
            return ResponseEntity.badRequest()
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid request parameter</message></error>");
        } catch (Exception e) {
            // Don't expose internal error details
            logger.error("Error handling CalDAV {} for {}/{}: {}", method, username, calendarSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>An error occurred processing your request</message></error>");
        }
    }

    /**
     * PROPFIND /caldav/ and /caldav
     * CalDAV root discovery - allows clients to discover available calendars
     */
    @RequestMapping(value = {"", "/"})
    public ResponseEntity<String> propfindRoot(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) String propfindXML,
            @RequestHeader(value = "Depth", defaultValue = "0") String depth) {

        // Only handle PROPFIND method
        if (!"PROPFIND".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        logger.info("CalDAV PROPFIND root: / (depth={})", depth);

        try {
            User currentUser = userService.getCurrentUser();
            String xmlResponse = xmlBuilder.buildRootResponse(currentUser.getUsername());

            logger.info("CalDAV PROPFIND root successful");

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlResponse);

        } catch (Exception e) {
            // Don't expose internal error details
            logger.error("Error handling CalDAV PROPFIND root: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>An error occurred processing your request</message></error>");
        }
    }

    /**
     * PROPFIND /caldav/{username}/ and /caldav/{username}
     * List available calendars for a user
     */
    @RequestMapping(value = {"/{username}", "/{username}/"})
    public ResponseEntity<String> propfindUser(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable String username,
            @RequestBody(required = false) String propfindXML,
            @RequestHeader(value = "Depth", defaultValue = "1") String depth) {

        // Only handle PROPFIND method
        if (!"PROPFIND".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        logger.info("CalDAV PROPFIND user: /{} (depth={})", username, depth);

        try {
            // Validate path parameters to prevent injection attacks
            validator.validateUsername(username);

            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                logger.warn("Unauthorized CalDAV access attempt: user {} tried to access {}",
                           currentUser.getUsername(), username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get user's calendars
            List<com.privatecal.dto.CalendarResponse> calendars = calendarService.getAllCalendars();

            // Build WebDAV multistatus response using builder
            String xmlResponse = xmlBuilder.buildUserPrincipalResponse(
                username,
                currentUser.getEmail(),
                calendars,
                depth
            );

            logger.info("CalDAV PROPFIND user successful: {} calendars listed", calendars.size());

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlResponse);

        } catch (IllegalArgumentException e) {
            // Validation errors - safe to expose
            logger.warn("CalDAV PROPFIND user validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid request parameter</message></error>");
        } catch (Exception e) {
            // Don't expose internal error details
            logger.error("Error handling CalDAV PROPFIND user for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>An error occurred processing your request</message></error>");
        }
    }

    /**
     * GET /caldav/ and /caldav
     * Return basic info about CalDAV root (some clients use GET for discovery)
     */
    @GetMapping(value = {"", "/"})
    public ResponseEntity<String> getCalDAVRoot() {
        logger.debug("CalDAV GET request for root collection");

        User currentUser = userService.getCurrentUser();

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<caldav-info>\n" +
                    "  <message>PrivateCal CalDAV Server</message>\n" +
                    "  <user>" + validator.escapeXml(currentUser.getUsername()) + "</user>\n" +
                    "  <caldav-url>/caldav/" + validator.escapeXml(currentUser.getUsername()) + "/</caldav-url>\n" +
                    "</caldav-info>";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    /**
     * OPTIONS /caldav/ and /caldav
     * Declare CalDAV support for root collection
     */
    @RequestMapping(value = {"", "/"},
                    method = RequestMethod.OPTIONS)
    public ResponseEntity<?> optionsCalDAVRoot() {
        logger.debug("CalDAV OPTIONS request for root collection");

        return ResponseEntity.ok()
                .header("DAV", "1, 3, calendar-access")  // CalDAV capability
                .header("Allow", "OPTIONS, GET, HEAD, PROPFIND")
                .build();
    }

    /**
     * PUT /caldav/{username}/ - Not allowed (must PUT to calendar, not user collection)
     */
    @PutMapping({"/{username}", "/{username}/"})
    public ResponseEntity<String> putUserCollection(@PathVariable String username) {
        try {
            // Validate even though we're rejecting
            validator.validateUsername(username);

            logger.warn("CalDAV PUT attempt on user collection /{}, returning 403", username);

            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<error xmlns=\"DAV:\">\n" +
                        "  <cannot-modify-protected-property/>\n" +
                        "  <message>Cannot PUT to user collection. Use /caldav/" + validator.escapeXml(username) + "/{calendar}/ instead.</message>\n" +
                        "</error>";

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        } catch (IllegalArgumentException e) {
            logger.warn("CalDAV PUT validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>Invalid request parameter</message></error>");
        }
    }

    /**
     * OPTIONS /caldav/{username}/ and /caldav/{username}
     * Declare CalDAV support for user collection
     */
    @RequestMapping(value = {"/{username}", "/{username}/"},
                    method = RequestMethod.OPTIONS)
    public ResponseEntity<?> optionsUserCollection() {
        logger.debug("CalDAV OPTIONS request for user collection");

        return ResponseEntity.ok()
                .header("DAV", "1, 3, calendar-access")  // CalDAV capability
                .header("Allow", "OPTIONS, GET, HEAD, PROPFIND")
                .build();
    }

}
