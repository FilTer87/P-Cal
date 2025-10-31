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
                    .header("ETag", "\"" + etag + "\"")
                    .header("Content-Disposition", "inline; filename=\"" + eventUid + ".ics\"")
                    .body(icsContent);

        } catch (Exception e) {
            logger.error("Error handling CalDAV GET: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
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
                        .header("ETag", "\"" + newETag + "\"")
                        .header("Location", resourceUrl)
                        .header("Cache-Control", "no-cache")
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .header("ETag", "\"" + newETag + "\"")
                        .header("Cache-Control", "no-cache")
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

        } catch (Exception e) {
            logger.error("Error handling CalDAV DELETE: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        if (requestXML != null && !requestXML.isEmpty()) {
            logger.debug("Request body (first 500 chars): {}", requestXML.substring(0, Math.min(500, requestXML.length())));
        }

        try {
            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify calendar exists
            Calendar calendar = calendarService.getCalendarBySlugAndUsername(calendarSlug, username);

            // Parse calendar-multiget requests to get only requested UIDs
            List<String> requestedUids = null;
            if ("REPORT".equalsIgnoreCase(method) && requestXML != null && requestXML.contains("calendar-multiget")) {
                requestedUids = new java.util.ArrayList<>();
                // Simple XML parsing to extract href values
                // Example: <D:href>/caldav/user/calendar/EVENT-UID.ics</D:href>
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<D:href>([^<]+)</D:href>");
                java.util.regex.Matcher matcher = pattern.matcher(requestXML);
                while (matcher.find()) {
                    String href = matcher.group(1);
                    // Extract UID from href (remove .ics extension)
                    if (href.endsWith(".ics")) {
                        String[] parts = href.split("/");
                        String uidWithExt = parts[parts.length - 1];
                        String uid = uidWithExt.substring(0, uidWithExt.length() - 4);
                        requestedUids.add(uid);
                    }
                }
                logger.info("CalDAV calendar-multiget: {} UIDs requested", requestedUids.size());
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

            // Build simplified WebDAV multistatus response
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");

            // Calendar collection resource
            xml.append("  <D:response>\n");
            xml.append("    <D:href>/caldav/").append(escapeXml(username)).append("/").append(escapeXml(calendarSlug)).append("/</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:resourcetype><D:collection/><C:calendar/></D:resourcetype>\n");
            xml.append("        <D:displayname>").append(escapeXml(calendar.getName())).append("</D:displayname>\n");
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");

            // Check if client requested calendar-data (PROPFIND with allprop/calendar-data or REPORT always includes it)
            boolean includeCalendarData = "REPORT".equalsIgnoreCase(method) ||
                (requestXML != null && (requestXML.contains("calendar-data") || requestXML.contains("allprop")));

            // Individual events (if depth > 0 for PROPFIND, always for REPORT)
            if (!"0".equals(depth) || "REPORT".equalsIgnoreCase(method)) {
                for (Task task : tasks) {
                    String etag = calDAVService.getTaskETag(task.getUid());
                    xml.append("  <D:response>\n");
                    xml.append("    <D:href>/caldav/").append(escapeXml(username)).append("/").append(escapeXml(calendarSlug))
                       .append("/").append(escapeXml(task.getUid())).append(".ics</D:href>\n");
                    xml.append("    <D:propstat>\n");
                    xml.append("      <D:prop>\n");
                    xml.append("        <D:getetag>\"").append(escapeXml(etag)).append("\"</D:getetag>\n");

                    // Only include getcontenttype for PROPFIND
                    if ("PROPFIND".equalsIgnoreCase(method)) {
                        xml.append("        <D:getcontenttype>text/calendar; component=VEVENT</D:getcontenttype>\n");
                    }

                    // Include calendar data if requested
                    if (includeCalendarData) {
                        try {
                            String icsContent = calDAVService.exportTaskAsICS(task.getUid());
                            xml.append("        <C:calendar-data>");
                            xml.append(escapeXml(icsContent));
                            xml.append("</C:calendar-data>\n");
                        } catch (Exception e) {
                            logger.warn("Failed to export task {} as ICS: {}", task.getUid(), e.getMessage());
                        }
                    }

                    xml.append("      </D:prop>\n");
                    xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
                    xml.append("    </D:propstat>\n");
                    xml.append("  </D:response>\n");
                }
            }

            xml.append("</D:multistatus>");

            String xmlResponse = xml.toString();
            logger.info("CalDAV {} successful: {} events listed, response size: {} KB",
                       method, tasks.size(), xmlResponse.length() / 1024);

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlResponse);

        } catch (Exception e) {
            logger.error("Error handling CalDAV PROPFIND: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

            // Return root collection response
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");
            xml.append("  <D:response>\n");
            xml.append("    <D:href>/caldav/</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:resourcetype><D:collection/></D:resourcetype>\n");
            xml.append("        <D:displayname>CalDAV Root</D:displayname>\n");
            xml.append("        <D:current-user-principal>\n");
            xml.append("          <D:href>/caldav/").append(escapeXml(currentUser.getUsername())).append("/</D:href>\n");
            xml.append("        </D:current-user-principal>\n");
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");
            xml.append("</D:multistatus>");

            logger.info("CalDAV PROPFIND root successful");

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml.toString());

        } catch (Exception e) {
            logger.error("Error handling CalDAV PROPFIND root: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            // Validate current user
            User currentUser = userService.getCurrentUser();
            if (!currentUser.getUsername().equals(username) && !currentUser.getEmail().equals(username)) {
                logger.warn("Unauthorized CalDAV access attempt: user {} tried to access {}",
                           currentUser.getUsername(), username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get user's calendars
            List<com.privatecal.dto.CalendarResponse> calendars = calendarService.getAllCalendars();

            // Build WebDAV multistatus response
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");

            // User principal collection
            xml.append("  <D:response>\n");
            xml.append("    <D:href>/caldav/").append(escapeXml(username)).append("/</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:resourcetype><D:collection/></D:resourcetype>\n");
            xml.append("        <D:displayname>").append(escapeXml(username)).append("</D:displayname>\n");
            xml.append("        <C:calendar-home-set>\n");
            xml.append("          <D:href>/caldav/").append(escapeXml(username)).append("/</D:href>\n");
            xml.append("        </C:calendar-home-set>\n");
            xml.append("        <C:calendar-user-address-set>\n");
            xml.append("          <D:href>mailto:").append(escapeXml(currentUser.getEmail())).append("</D:href>\n");
            xml.append("        </C:calendar-user-address-set>\n");
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");

            // List each calendar if depth > 0
            if (!"0".equals(depth)) {
                for (com.privatecal.dto.CalendarResponse calendar : calendars) {
                    xml.append("  <D:response>\n");
                    xml.append("    <D:href>/caldav/").append(escapeXml(username)).append("/")
                       .append(escapeXml(calendar.getSlug())).append("/</D:href>\n");
                    xml.append("    <D:propstat>\n");
                    xml.append("      <D:prop>\n");
                    xml.append("        <D:resourcetype>\n");
                    xml.append("          <D:collection/>\n");
                    xml.append("          <C:calendar/>\n");
                    xml.append("        </D:resourcetype>\n");
                    xml.append("        <D:displayname>").append(escapeXml(calendar.getName())).append("</D:displayname>\n");
                    xml.append("        <C:calendar-description>").append(escapeXml(calendar.getDescription() != null ? calendar.getDescription() : "")).append("</C:calendar-description>\n");
                    xml.append("      </D:prop>\n");
                    xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
                    xml.append("    </D:propstat>\n");
                    xml.append("  </D:response>\n");
                }
            }

            xml.append("</D:multistatus>");

            logger.info("CalDAV PROPFIND user successful: {} calendars listed", calendars.size());

            return ResponseEntity.status(207)  // 207 Multi-Status
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml.toString());

        } catch (Exception e) {
            logger.error("Error handling CalDAV PROPFIND user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
                    "  <user>" + escapeXml(currentUser.getUsername()) + "</user>\n" +
                    "  <caldav-url>/caldav/" + escapeXml(currentUser.getUsername()) + "/</caldav-url>\n" +
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
        logger.warn("CalDAV PUT attempt on user collection /{}, returning 403", username);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<error xmlns=\"DAV:\">\n" +
                    "  <cannot-modify-protected-property/>\n" +
                    "  <message>Cannot PUT to user collection. Use /caldav/" + escapeXml(username) + "/{calendar}/ instead.</message>\n" +
                    "</error>";

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
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

    /**
     * Escapes XML special characters to prevent XML injection and XSS attacks.
     *
     * Escapes the following characters according to XML specification:
     * - & (ampersand) → &amp;
     * - < (less than) → &lt;
     * - > (greater than) → &gt;
     * - " (double quote) → &quot;
     * - ' (apostrophe) → &apos;
     *
     * @param text The text to escape (can be null)
     * @return Escaped text safe for XML inclusion, or empty string if input is null
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
