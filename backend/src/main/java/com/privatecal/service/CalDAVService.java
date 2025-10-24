package com.privatecal.service;

import com.privatecal.dto.DuplicateStrategy;
import com.privatecal.dto.ImportPreviewResponse;
import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for CalDAV integration: import/export iCalendar (.ics) format
 * Handles conversion between Task entities and iCalendar VEVENT/VTODO components
 */
@Service
public class CalDAVService {

    private static final Logger logger = LoggerFactory.getLogger(CalDAVService.class);
    private static final String PRODID = "-//PrivateCal//PrivateCal v0.11.0//EN";
    private static final int DEFAULT_TODO_DURATION_MINUTES = 30;
    private static final int MAX_DESCRIPTION_LENGTH = 2500;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_LOCATION_LENGTH = 200;

    @Autowired
    private TaskRepository taskRepository;

    /**
     * Export tasks to iCalendar format (.ics)
     *
     * @param tasks List of tasks to export
     * @param calendarName Name of the calendar
     * @return iCalendar data as byte array
     */
    public byte[] exportToICS(List<Task> tasks, String calendarName) throws IOException {
        logger.info("Exporting {} tasks to iCalendar format", tasks.size());

        // Create calendar
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId(PRODID));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(new XProperty("X-WR-CALNAME", calendarName));
        calendar.getProperties().add(CalScale.GREGORIAN);

        // Add tasks as VEVENTs
        int successCount = 0;
        int failedCount = 0;
        for (Task task : tasks) {
            try {
                logger.debug("Converting task {} to VEVENT: {}", task.getId(), task.getTitle());
                VEvent event = taskToVEvent(task);
                calendar.getComponents().add(event);
                successCount++;
                logger.debug("Successfully added task {} to calendar", task.getId());
            } catch (Exception e) {
                failedCount++;
                logger.error("Error converting task {} ('{}') to VEVENT: {}",
                    task.getId(), task.getTitle(), e.getMessage(), e);
            }
        }
        logger.info("Export conversion summary: {} succeeded, {} failed out of {} total tasks",
            successCount, failedCount, tasks.size());

        // Output to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        try {
            outputter.output(calendar, out);
            logger.info("Successfully exported {} events to ICS", calendar.getComponents().size());
            return out.toByteArray();
        } catch (Exception e) {
            logger.error("Error outputting calendar: {}", e.getMessage(), e);
            throw new IOException("Failed to export calendar", e);
        }
    }

    /**
     * Convert Task entity to iCalendar VEVENT
     */
    private VEvent taskToVEvent(Task task) {
        logger.debug("Converting task {} to VEVENT", task.getId());

        // Create event with UID (use saved UID or fallback to generated)
        String uid = task.getUid();
        if (uid == null || uid.trim().isEmpty()) {
            // Fallback for legacy tasks without UID
            uid = "privatecal-" + task.getUser().getId() + "-" + task.getId() + "@privatecal.local";
            logger.warn("Task {} missing UID, using fallback: {}", task.getId(), uid);
        }
        VEvent event = new VEvent();
        event.getProperties().add(new Uid(uid));

        // Set title (SUMMARY)
        event.getProperties().add(new Summary(task.getTitle()));

        // Set description if present
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            event.getProperties().add(new Description(task.getDescription()));
        }

        // Set location if present
        if (task.getLocation() != null && !task.getLocation().trim().isEmpty()) {
            event.getProperties().add(new Location(task.getLocation()));
        }

        // Set dates (DTSTART and DTEND)
        if (Boolean.TRUE.equals(task.getIsAllDay())) {
            // All-day event: use DATE format (no time component)
            LocalDate startDate = task.getStartDatetime().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate endDate = task.getEndDatetime().atZone(ZoneOffset.UTC).toLocalDate();

            // Create ical4j Date objects (not DateTime) for all-day events
            // Format: YYYYMMDD (e.g., 20251021)
            try {
                String startDateStr = String.format("%04d%02d%02d",
                    startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth());
                String endDateStr = String.format("%04d%02d%02d",
                    endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth());

                event.getProperties().add(new DtStart(new Date(startDateStr)));
                event.getProperties().add(new DtEnd(new Date(endDateStr)));
            } catch (Exception e) {
                logger.error("Error creating all-day dates for task {}: {}", task.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create all-day event dates", e);
            }
        } else {
            // Timed event: use DATE-TIME format (UTC)
            DateTime dtStart = new DateTime(true); // true = UTC
            dtStart.setTime(task.getStartDatetime().toEpochMilli());
            event.getProperties().add(new DtStart(dtStart));

            DateTime dtEnd = new DateTime(true);
            dtEnd.setTime(task.getEndDatetime().toEpochMilli());
            event.getProperties().add(new DtEnd(dtEnd));
        }

        // Set timestamps
        event.getProperties().add(new Created(new DateTime(task.getCreatedAt().toEpochMilli())));
        event.getProperties().add(new LastModified(new DateTime(task.getUpdatedAt() != null ?
            task.getUpdatedAt().toEpochMilli() : task.getCreatedAt().toEpochMilli())));

        // Set recurrence rule if present
        if (task.getRecurrenceRule() != null && !task.getRecurrenceRule().trim().isEmpty()) {
            try {
                event.getProperties().add(new RRule(task.getRecurrenceRule()));
            } catch (Exception e) {
                logger.warn("Invalid RRULE for task {}: {}", task.getId(), e.getMessage());
            }
        }

        // Set color (Apple extension)
        if (task.getColor() != null) {
            event.getProperties().add(new XProperty("X-APPLE-CALENDAR-COLOR", task.getColor()));
        }

        // Add reminders as VALARMs
        if (task.getReminders() != null && !task.getReminders().isEmpty()) {
            task.getReminders().forEach(reminder -> {
                try {
                    VAlarm alarm = new VAlarm();
                    // Set trigger as duration before event start (negative minutes)
                    Trigger trigger = new Trigger(java.time.Duration.ofMinutes(-reminder.getReminderOffsetMinutes()));
                    alarm.getProperties().add(trigger);
                    alarm.getProperties().add(Action.DISPLAY);
                    alarm.getProperties().add(new Description("Reminder: " + task.getTitle()));
                    event.getAlarms().add(alarm);
                } catch (Exception e) {
                    logger.warn("Error adding reminder for task {}: {}", task.getId(), e.getMessage());
                }
            });
        }

        return event;
    }

    /**
     * Import iCalendar file (.ics) and convert to TaskRequest list
     *
     * @param inputStream ICS file input stream
     * @param user User importing the calendar
     * @return List of TaskRequest objects ready for creation
     */
    public List<TaskRequest> importFromICS(InputStream inputStream, User user) throws IOException {
        logger.info("Importing calendar for user: {}", user.getUsername());

        List<TaskRequest> taskRequests = new ArrayList<>();

        try {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(inputStream);

            // Process VEVENTs
            calendar.getComponents(Component.VEVENT).forEach(component -> {
                try {
                    VEvent event = (VEvent) component;
                    TaskRequest taskRequest = veventToTaskRequest(event);
                    taskRequests.add(taskRequest);
                } catch (Exception e) {
                    logger.error("Error converting VEVENT to TaskRequest: {}", e.getMessage(), e);
                }
            });

            // Process VTODOs
            calendar.getComponents(Component.VTODO).forEach(component -> {
                try {
                    VToDo todo = (VToDo) component;
                    TaskRequest taskRequest = vtodoToTaskRequest(todo);
                    taskRequests.add(taskRequest);
                } catch (Exception e) {
                    logger.error("Error converting VTODO to TaskRequest: {}", e.getMessage(), e);
                }
            });

            logger.info("Successfully imported {} tasks from ICS", taskRequests.size());
            return taskRequests;

        } catch (Exception e) {
            logger.error("Error parsing ICS file: {}", e.getMessage(), e);
            throw new IOException("Failed to import calendar", e);
        }
    }

    /**
     * Convert VEVENT to TaskRequest
     */
    private TaskRequest veventToTaskRequest(VEvent event) {
        TaskRequest taskRequest = new TaskRequest();

        // Get UID (preserve original or generate deterministic)
        Uid eventUid = event.getUid();
        if (eventUid != null && eventUid.getValue() != null) {
            taskRequest.setUid(eventUid.getValue());
        } else {
            // Generate deterministic UID if missing
            Summary summary = event.getSummary();
            DtStart dtStart = event.getStartDate();
            String generatedUid = generateDeterministicUid(
                summary != null ? summary.getValue() : null,
                dtStart != null ? dtStart.getDate() : null
            );
            taskRequest.setUid(generatedUid);
            logger.warn("VEVENT missing UID, generated: {}", generatedUid);
        }

        // Get SUMMARY (title)
        Summary summary = event.getSummary();
        String title = summary != null ? summary.getValue() : "Untitled Event";
        String eventId = taskRequest.getUid(); // Use UID for logging
        taskRequest.setTitle(truncateField(title, MAX_TITLE_LENGTH, "title", "event " + eventId));

        // Get DESCRIPTION
        Description description = event.getDescription();
        if (description != null) {
            String descValue = truncateField(description.getValue(), MAX_DESCRIPTION_LENGTH,
                "description", "event " + eventId);
            taskRequest.setDescription(descValue);
        }

        // Get LOCATION
        net.fortuna.ical4j.model.property.Location location = event.getLocation();
        if (location != null) {
            String locValue = truncateField(location.getValue(), MAX_LOCATION_LENGTH,
                "location", "event " + eventId);
            taskRequest.setLocation(locValue);
        }

        // Get dates (DTSTART and DTEND)
        DtStart dtStart = event.getStartDate();
        DtEnd dtEnd = event.getEndDate();

        if (dtStart != null && dtEnd != null) {
            Date startDate = dtStart.getDate();
            Date endDate = dtEnd.getDate();

            // Check if it's an all-day event (DATE vs DATE-TIME)
            boolean isAllDay = dtStart.getParameter(Value.VALUE) == Value.DATE;
            taskRequest.setIsAllDay(isAllDay);

            taskRequest.setStartDatetime(Instant.ofEpochMilli(startDate.getTime()));
            taskRequest.setEndDatetime(Instant.ofEpochMilli(endDate.getTime()));
        } else {
            // Fallback: use current time + 1 hour
            Instant now = Instant.now();
            taskRequest.setStartDatetime(now);
            taskRequest.setEndDatetime(now.plus(1, ChronoUnit.HOURS));
            taskRequest.setIsAllDay(false);
        }

        // Get recurrence rule
        RRule rrule = event.getProperty(Property.RRULE);
        if (rrule != null) {
            taskRequest.setRecurrenceRule(rrule.getValue());
        }

        // Get color (Apple extension)
        Property colorProp = event.getProperty("X-APPLE-CALENDAR-COLOR");
        if (colorProp != null) {
            taskRequest.setColor(colorProp.getValue());
        }

        return taskRequest;
    }

    /**
     * Convert VTODO to TaskRequest with hybrid logic:
     * - If DUE has time component → 30-minute timed task
     * - If DUE is date-only or absent → all-day task
     */
    private TaskRequest vtodoToTaskRequest(VToDo todo) {
        TaskRequest taskRequest = new TaskRequest();

        // Get UID (preserve original or generate deterministic)
        Uid todoUid = todo.getUid();
        if (todoUid != null && todoUid.getValue() != null) {
            taskRequest.setUid(todoUid.getValue());
        } else {
            // Generate deterministic UID if missing
            Summary summary = todo.getSummary();
            Due due = todo.getProperty(Property.DUE);
            String generatedUid = generateDeterministicUid(
                summary != null ? summary.getValue() : null,
                due != null ? due.getDate() : null
            );
            taskRequest.setUid(generatedUid);
            logger.warn("VTODO missing UID, generated: {}", generatedUid);
        }

        // Get SUMMARY (title) with [TODO] prefix
        Summary summary = todo.getSummary();
        String title = summary != null ? summary.getValue() : "Untitled Task";
        String todoId = taskRequest.getUid(); // Use UID for logging
        // Add [TODO] prefix and truncate if necessary (accounting for prefix length)
        String prefixedTitle = "[TODO] " + title;
        taskRequest.setTitle(truncateField(prefixedTitle, MAX_TITLE_LENGTH, "title", "todo " + todoId));

        // Get DESCRIPTION
        Description description = todo.getDescription();
        if (description != null) {
            String descValue = truncateField(description.getValue(), MAX_DESCRIPTION_LENGTH,
                "description", "todo " + todoId);
            taskRequest.setDescription(descValue);
        }

        // Get LOCATION
        net.fortuna.ical4j.model.property.Location location = todo.getLocation();
        if (location != null) {
            String locValue = truncateField(location.getValue(), MAX_LOCATION_LENGTH,
                "location", "todo " + todoId);
            taskRequest.setLocation(locValue);
        }

        // Get DUE date/time
        Due due = todo.getProperty(Property.DUE);

        if (due != null) {
            Date dueDate = due.getDate();

            // Check if DUE has time component (DATE-TIME vs DATE)
            boolean hasTimeComponent = dueDate instanceof DateTime;

            if (hasTimeComponent) {
                // CASE 1: DUE with time → 30-minute timed task
                Instant dueInstant = Instant.ofEpochMilli(dueDate.getTime());
                taskRequest.setStartDatetime(dueInstant);
                taskRequest.setEndDatetime(dueInstant.plus(DEFAULT_TODO_DURATION_MINUTES, ChronoUnit.MINUTES));
                taskRequest.setIsAllDay(false);
                logger.debug("VTODO with time component: {}", dueInstant);
            } else {
                // CASE 2: DUE date-only → all-day task
                LocalDate localDate = Instant.ofEpochMilli(dueDate.getTime())
                    .atZone(ZoneOffset.UTC).toLocalDate();
                taskRequest.setStartDatetime(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
                taskRequest.setEndDatetime(localDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
                taskRequest.setIsAllDay(true);
                logger.debug("VTODO date-only: {}", localDate);
            }
        } else {
            // CASE 3: No DUE → all-day task for today
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            taskRequest.setStartDatetime(today.atStartOfDay(ZoneOffset.UTC).toInstant());
            taskRequest.setEndDatetime(today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
            taskRequest.setIsAllDay(true);
            logger.debug("VTODO without DUE: using today");
        }

        return taskRequest;
    }

    /**
     * Generate calendar name for a user
     */
    public String generateCalendarName(User user) {
        return user.getUsername() + "'s Calendar";
    }

    /**
     * Truncate a string to a maximum length, logging if truncation occurs
     */
    private String truncateField(String value, int maxLength, String fieldName, String itemContext) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        logger.warn("Truncating {} from {} to {} characters for {}",
            fieldName, value.length(), maxLength, itemContext);
        return value.substring(0, maxLength);
    }

    /**
     * Generate a deterministic UID for an event/todo when none is present
     * Format: privatecal-generated-[SHA256-hash]
     *
     * @param summary Event/Todo summary (title)
     * @param dtStart Event/Todo start date
     * @return Generated UID string
     */
    private String generateDeterministicUid(String summary, Date dtStart) {
        try {
            // Create hash from summary + start date
            String input = (summary != null ? summary : "untitled") +
                          (dtStart != null ? dtStart.toString() : "no-date");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string (first 16 characters for brevity)
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return "privatecal-generated-" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to timestamp-based UID
            logger.error("SHA-256 not available, using timestamp UID", e);
            return "privatecal-generated-" + System.currentTimeMillis();
        }
    }

    /**
     * Analyze ICS file for import preview - detects duplicates without importing
     *
     * @param file ICS file to analyze
     * @param user User for duplicate checking
     * @return Preview information with duplicate detection
     */
    public ImportPreviewResponse analyzeIcsFile(MultipartFile file, User user) throws IOException {
        logger.info("Analyzing ICS file for user: {}", user.getUsername());

        ImportPreviewResponse response = new ImportPreviewResponse();
        List<ImportPreviewResponse.DuplicateEventInfo> duplicates = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(inputStream);

            // Parse all events/todos
            List<TaskRequest> parsedRequests = new ArrayList<>();
            List<String> parseErrors = new ArrayList<>();

            // Process VEVENTs
            calendar.getComponents(Component.VEVENT).forEach(component -> {
                try {
                    VEvent event = (VEvent) component;
                    TaskRequest taskRequest = veventToTaskRequest(event);
                    parsedRequests.add(taskRequest);
                } catch (Exception e) {
                    parseErrors.add("VEVENT: " + e.getMessage());
                    logger.warn("Failed to parse VEVENT: {}", e.getMessage());
                }
            });

            // Process VTODOs
            calendar.getComponents(Component.VTODO).forEach(component -> {
                try {
                    VToDo todo = (VToDo) component;
                    TaskRequest taskRequest = vtodoToTaskRequest(todo);
                    parsedRequests.add(taskRequest);
                } catch (Exception e) {
                    parseErrors.add("VTODO: " + e.getMessage());
                    logger.warn("Failed to parse VTODO: {}", e.getMessage());
                }
            });

            response.setTotalEvents(parsedRequests.size() + parseErrors.size());
            response.setErrorEvents(parseErrors.size());

            // Check for duplicates using UID
            List<String> uids = parsedRequests.stream()
                .map(TaskRequest::getUid)
                .filter(uid -> uid != null && !uid.isEmpty())
                .collect(Collectors.toList());

            // Batch query to find existing tasks by UID
            Map<String, Task> existingTasksByUid = new HashMap<>();
            if (!uids.isEmpty()) {
                List<Task> existingTasks = taskRepository.findByUserAndUidIn(user, uids);
                existingTasksByUid = existingTasks.stream()
                    .collect(Collectors.toMap(Task::getUid, task -> task));
            }

            // Categorize events as new or duplicate
            int newCount = 0;
            int duplicateCount = 0;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneOffset.UTC);

            for (TaskRequest taskRequest : parsedRequests) {
                String uid = taskRequest.getUid();
                if (uid != null && existingTasksByUid.containsKey(uid)) {
                    // Duplicate found
                    duplicateCount++;
                    Task existingTask = existingTasksByUid.get(uid);

                    // Check if content changed
                    boolean contentChanged = hasContentChanged(existingTask, taskRequest);

                    ImportPreviewResponse.DuplicateEventInfo dupInfo =
                        new ImportPreviewResponse.DuplicateEventInfo();
                    dupInfo.setUid(uid);
                    dupInfo.setTitle(taskRequest.getTitle());
                    dupInfo.setExistingDate(dateFormatter.format(existingTask.getStartDatetime()));
                    dupInfo.setNewDate(dateFormatter.format(taskRequest.getStartDatetime()));
                    dupInfo.setContentChanged(contentChanged);

                    duplicates.add(dupInfo);
                } else {
                    // New event
                    newCount++;
                }
            }

            response.setNewEvents(newCount);
            response.setDuplicateEvents(duplicateCount);
            response.setDuplicates(duplicates);

            logger.info("Analysis complete: {} total, {} new, {} duplicates, {} errors",
                response.getTotalEvents(), newCount, duplicateCount, parseErrors.size());

            return response;
        } catch (Exception e) {
            logger.error("Error analyzing ICS file: {}", e.getMessage(), e);
            throw new IOException("Failed to analyze ICS file", e);
        }
    }

    /**
     * Import ICS file with duplicate handling strategy
     *
     * @param file ICS file to import
     * @param user User to import for
     * @param strategy How to handle duplicates
     * @return List of imported task requests
     */
    public List<TaskRequest> importWithDuplicateHandling(
            MultipartFile file,
            User user,
            DuplicateStrategy strategy) throws IOException {

        logger.info("Importing ICS file with strategy {} for user: {}", strategy, user.getUsername());

        try (InputStream inputStream = file.getInputStream()) {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(inputStream);

            List<TaskRequest> parsedRequests = new ArrayList<>();

            // Process VEVENTs
            calendar.getComponents(Component.VEVENT).forEach(component -> {
                try {
                    VEvent event = (VEvent) component;
                    TaskRequest taskRequest = veventToTaskRequest(event);
                    parsedRequests.add(taskRequest);
                } catch (Exception e) {
                    logger.warn("Failed to parse VEVENT: {}", e.getMessage());
                }
            });

            // Process VTODOs
            calendar.getComponents(Component.VTODO).forEach(component -> {
                try {
                    VToDo todo = (VToDo) component;
                    TaskRequest taskRequest = vtodoToTaskRequest(todo);
                    parsedRequests.add(taskRequest);
                } catch (Exception e) {
                    logger.warn("Failed to parse VTODO: {}", e.getMessage());
                }
            });

            // Apply duplicate strategy
            List<TaskRequest> resultRequests = new ArrayList<>();

            for (TaskRequest taskRequest : parsedRequests) {
                String uid = taskRequest.getUid();
                if (uid == null || uid.isEmpty()) {
                    // No UID, always import
                    resultRequests.add(taskRequest);
                    continue;
                }

                Optional<Task> existingTask = taskRepository.findByUserAndUid(user, uid);

                if (existingTask.isPresent()) {
                    // Duplicate found - apply strategy
                    switch (strategy) {
                        case SKIP:
                            logger.debug("Skipping duplicate: {}", uid);
                            // Don't add to result
                            break;

                        case UPDATE:
                            logger.debug("Updating existing task: {}", uid);
                            // Keep the existing task ID to trigger update
                            taskRequest.setId(existingTask.get().getId());
                            resultRequests.add(taskRequest);
                            break;

                        case CREATE_ANYWAY:
                            logger.debug("Creating duplicate anyway: {}", uid);
                            // Generate new UID to avoid constraint violation
                            String newUid = "privatecal-dup-" + System.currentTimeMillis() + "-" +
                                Math.abs(uid.hashCode());
                            taskRequest.setUid(newUid);
                            resultRequests.add(taskRequest);
                            break;
                    }
                } else {
                    // New event, always import
                    resultRequests.add(taskRequest);
                }
            }

            logger.info("Import complete: {} tasks to be imported (strategy: {})",
                resultRequests.size(), strategy);

            return resultRequests;
        } catch (Exception e) {
            logger.error("Error importing ICS file: {}", e.getMessage(), e);
            throw new IOException("Failed to import ICS file", e);
        }
    }

    /**
     * Check if content has changed between existing task and imported request
     */
    private boolean hasContentChanged(Task existingTask, TaskRequest taskRequest) {
        // Compare key fields
        boolean titleChanged = !safeEquals(existingTask.getTitle(), taskRequest.getTitle());
        boolean descChanged = !safeEquals(existingTask.getDescription(), taskRequest.getDescription());
        boolean locationChanged = !safeEquals(existingTask.getLocation(), taskRequest.getLocation());
        boolean startChanged = !safeEquals(existingTask.getStartDatetime(), taskRequest.getStartDatetime());
        boolean endChanged = !safeEquals(existingTask.getEndDatetime(), taskRequest.getEndDatetime());

        return titleChanged || descChanged || locationChanged || startChanged || endChanged;
    }

    /**
     * Safe equality check handling nulls
     */
    private boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Export tasks from a specific calendar to iCalendar format
     * Used by CalDAV server to export calendar-specific events
     *
     * @param calendar Calendar entity
     * @return iCalendar data as byte array
     */
    public byte[] exportCalendarToICS(com.privatecal.entity.Calendar calendar) throws IOException {
        logger.info("Exporting calendar '{}' (id={}) to iCalendar format", calendar.getName(), calendar.getId());

        List<Task> tasks = taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(calendar.getId());
        return exportToICS(tasks, calendar.getName());
    }

    /**
     * Export task as ICS by UID (CalDAV compliant)
     * @param taskUid Task UID (primary key)
     * @return ICS formatted string
     */
    @Transactional(readOnly = true)
    public String exportTaskAsICS(String taskUid) throws IOException {
        Task task = taskRepository.findById(taskUid)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskUid));

        // Create minimal calendar wrapper for single event
        net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
        calendar.getProperties().add(new ProdId(PRODID));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        VEvent event = taskToVEvent(task);
        calendar.getComponents().add(event);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(calendar, out);

        return out.toString("UTF-8");
    }

    /**
     * Get ETag for a task (for CalDAV conflict detection)
     * ETag is based on task's updatedAt timestamp
     *
     * @param taskUid Task UID (primary key)
     * @return ETag value (timestamp in millis)
     */
    @Transactional(readOnly = true)
    public String getTaskETag(String taskUid) {
        Task task = taskRepository.findById(taskUid)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskUid));

        Instant updated = task.getUpdatedAt() != null ? task.getUpdatedAt() : task.getCreatedAt();
        return String.valueOf(updated.toEpochMilli());
    }

    /**
     * Import/update single event from ICS string
     * Used by CalDAV PUT endpoint
     *
     * CalDAV RFC 4791 compliant: The URL UID now directly identifies the database resource
     * - UID in URL is the primary key, ensuring stable URLs
     * - If task with UID exists → Update it
     * - Otherwise → Create new task with this UID
     *
     * @param icsContent ICS content as string
     * @param targetCalendar Target calendar
     * @param currentUser Current authenticated user
     * @param eventUid Event UID from URL (primary key, CalDAV resource identifier)
     * @param expectedETag Expected ETag for conflict detection (optional, can be null)
     * @return Created or updated Task
     * @throws IOException if parsing fails
     * @throws RuntimeException if ETag mismatch (conflict)
     */
    @Transactional
    public Task importSingleEventFromICS(String icsContent, com.privatecal.entity.Calendar targetCalendar, User currentUser, String eventUid, String expectedETag) throws IOException {
        try {
            // Parse ICS content
            CalendarBuilder builder = new CalendarBuilder();
            net.fortuna.ical4j.model.Calendar icalCalendar = builder.build(
                new java.io.ByteArrayInputStream(icsContent.getBytes(StandardCharsets.UTF_8))
            );

            // Extract VEVENT (should be only one for CalDAV PUT)
            var components = icalCalendar.getComponents(Component.VEVENT);
            if (components.isEmpty()) {
                throw new IOException("No VEVENT found in ICS content");
            }
            if (components.size() > 1) {
                logger.warn("Multiple VEVENTs found in CalDAV PUT, using first one");
            }

            VEvent vevent = (VEvent) components.get(0);
            TaskRequest taskRequest = veventToTaskRequest(vevent);
            String uidFromICS = taskRequest.getUid();

            // CalDAV RFC 4791 compliant: URL UID is the primary key
            // This ensures stable URLs - the URL UID IS the database UID
            // Priority: Use eventUid from URL as the definitive identifier

            Optional<Task> existingTask = taskRepository.findById(eventUid);

            Task task;

            if (existingTask.isPresent()) {
                // UPDATE: Task exists at this URL
                task = existingTask.get();
                logger.info("CalDAV PUT: Updating task at UID {} (ICS contains UID: {})",
                    eventUid, uidFromICS);

                // Verify user owns this task
                if (!task.getUser().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Unauthorized: Task belongs to different user");
                }

            } else {
                // CREATE: New task with UID from URL
                task = new Task();
                task.setUid(eventUid);  // Use UID from URL as primary key
                task.setUser(currentUser);
                task.setCalendar(targetCalendar);
                task.setCreatedAt(Instant.now());
                logger.info("CalDAV PUT: Creating new task with UID {} (ICS contains UID: {})",
                    eventUid, uidFromICS);
            }

            // Verify/update calendar
            if (!task.getCalendar().getId().equals(targetCalendar.getId())) {
                logger.warn("Task {} exists in different calendar, moving to {}", eventUid, targetCalendar.getSlug());
                task.setCalendar(targetCalendar);
            }

            // Update all fields from ICS
            task.setTitle(taskRequest.getTitle());
            task.setDescription(taskRequest.getDescription());
            task.setLocation(taskRequest.getLocation());
            task.setStartDatetime(taskRequest.getStartDatetime());
            task.setEndDatetime(taskRequest.getEndDatetime());
            task.setIsAllDay(taskRequest.getIsAllDay());
            task.setRecurrenceRule(taskRequest.getRecurrenceRule());
            task.setColor(taskRequest.getColor());
            task.setUpdatedAt(Instant.now());

            Task savedTask = taskRepository.save(task);
            logger.info("CalDAV PUT successful: task {} (UID: {})", savedTask.getId(), savedTask.getUid());
            return savedTask;

        } catch (Exception e) {
            logger.error("Error importing single event from ICS: {}", e.getMessage(), e);
            throw new IOException("Failed to import event", e);
        }
    }
}
