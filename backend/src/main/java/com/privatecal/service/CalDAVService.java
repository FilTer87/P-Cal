package com.privatecal.service;

import com.privatecal.dto.DuplicateStrategy;
import com.privatecal.dto.ImportPreviewResponse;
import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.TaskRepository;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
// @RequiredArgsConstructor //test handling need no args constructor (using @Autowired)
public class CalDAVService {

    private static final Logger logger = LoggerFactory.getLogger(CalDAVService.class);
    private static final String PRODID = "-//PrivateCal//PrivateCal v0.11.0//EN";
    
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private RecurrenceService recurrenceService;

    @Autowired
    private com.privatecal.caldav.ICalConverter icalConverter;

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

        // Collect unique timezones from tasks to add VTIMEZONE components
        // This is CRITICAL for CalDAV clients to interpret TZID references correctly
        java.util.Set<String> timezones = new java.util.HashSet<>();
        for (Task task : tasks) {
            if (task.getTaskTimezone() != null && !Boolean.TRUE.equals(task.getIsAllDay())) {
                timezones.add(task.getTaskTimezone());
            }
        }

        // Add VTIMEZONE components for all referenced timezones
        net.fortuna.ical4j.model.TimeZoneRegistry registry =
            net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry();
        for (String tzId : timezones) {
            try {
                net.fortuna.ical4j.model.TimeZone tz = registry.getTimeZone(tzId);
                if (tz != null) {
                    calendar.getComponents().add(tz.getVTimeZone());
                    logger.debug("Added VTIMEZONE for {}", tzId);
                } else {
                    logger.warn("Timezone not found in registry: {}", tzId);
                }
            } catch (Exception e) {
                logger.warn("Error adding VTIMEZONE for {}: {}", tzId, e.getMessage());
            }
        }

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
        return icalConverter.taskToVEvent(task);
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
        return icalConverter.veventToTaskRequest(event);
    }

    /**
     * Convert VTODO to TaskRequest with hybrid logic:
     * - If DUE has time component → 30-minute timed task
     * - If DUE is date-only or absent → all-day task
     */
    private TaskRequest vtodoToTaskRequest(VToDo todo) {
        return icalConverter.vtodoToTaskRequest(todo);
    }

    /**
     * Generate calendar name for a user
     */
    public String generateCalendarName(User user) {
        return user.getUsername() + "'s Calendar";
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
                    dupInfo.setExistingDate(dateFormatter.format(existingTask.getStartDatetimeAsInstant()));
                    // Convert TaskRequest local time to Instant for display
                    Instant newStartInstant = taskRequest.getStartDatetimeLocal()
                        .atZone(java.time.ZoneId.of(taskRequest.getTimezone()))
                        .toInstant();
                    dupInfo.setNewDate(dateFormatter.format(newStartInstant));
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
        boolean startChanged = !safeEquals(existingTask.getStartDatetimeLocal(), taskRequest.getStartDatetimeLocal());
        boolean endChanged = !safeEquals(existingTask.getEndDatetimeLocal(), taskRequest.getEndDatetimeLocal());
        boolean timezoneChanged = !safeEquals(existingTask.getTaskTimezone(), taskRequest.getTimezone());

        return titleChanged || descChanged || locationChanged || startChanged || endChanged || timezoneChanged;
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

        // Create calendar wrapper for single event
        net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
        calendar.getProperties().add(new ProdId(PRODID));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        // Add VTIMEZONE component if task has timezone (CRITICAL for CalDAV clients)
        // Without VTIMEZONE, clients like Thunderbird cannot interpret TZID references
        if (task.getTaskTimezone() != null && !Boolean.TRUE.equals(task.getIsAllDay())) {
            try {
                net.fortuna.ical4j.model.TimeZoneRegistry registry =
                    net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry();
                net.fortuna.ical4j.model.TimeZone tz = registry.getTimeZone(task.getTaskTimezone());
                if (tz != null) {
                    calendar.getComponents().add(tz.getVTimeZone());
                    logger.debug("Added VTIMEZONE for {} to single task export", task.getTaskTimezone());
                } else {
                    logger.warn("Timezone not found in registry: {}", task.getTaskTimezone());
                }
            } catch (Exception e) {
                logger.warn("Error adding VTIMEZONE for {}: {}", task.getTaskTimezone(), e.getMessage());
            }
        }

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
            // Log ICS content for debugging
            logger.debug("CalDAV PUT received ICS content:\n{}", icsContent);

            // Parse ICS content
            CalendarBuilder builder = new CalendarBuilder();
            net.fortuna.ical4j.model.Calendar icalCalendar = builder.build(
                new java.io.ByteArrayInputStream(icsContent.getBytes(StandardCharsets.UTF_8))
            );

            // Extract VEVENTs
            // CalDAV clients (like Thunderbird) send multiple VEVENTs when modifying single occurrences:
            // 1. Master event with EXDATE for cancelled/modified occurrences
            // 2. Override events with RECURRENCE-ID for modified single occurrences
            var components = icalCalendar.getComponents(Component.VEVENT);
            if (components.isEmpty()) {
                throw new IOException("No VEVENT found in ICS content");
            }

            // Separate master event from override events
            VEvent masterEvent = null;
            List<VEvent> overrideEvents = new ArrayList<>();

            for (Object comp : components) {
                VEvent vevent = (VEvent) comp;
                // Check if this is an override (has RECURRENCE-ID)
                if (vevent.getProperty(Property.RECURRENCE_ID) != null) {
                    overrideEvents.add(vevent);
                    logger.debug("Found override event with RECURRENCE-ID");
                } else {
                    if (masterEvent != null) {
                        logger.warn("Multiple master VEVENTs found (no RECURRENCE-ID), using first one");
                    } else {
                        masterEvent = vevent;
                        logger.debug("Found master event (no RECURRENCE-ID)");
                    }
                }
            }

            if (masterEvent == null) {
                throw new IOException("No master VEVENT found (all have RECURRENCE-ID)");
            }

            TaskRequest taskRequest = veventToTaskRequest(masterEvent);
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
            task.setStartDatetimeLocal(taskRequest.getStartDatetimeLocal());
            task.setEndDatetimeLocal(taskRequest.getEndDatetimeLocal());
            task.setTaskTimezone(taskRequest.getTimezone());
            task.setIsAllDay(taskRequest.getIsAllDay());
            task.setRecurrenceRule(taskRequest.getRecurrenceRule());
            task.setRecurrenceExceptions(taskRequest.getRecurrenceExceptions());
            task.setColor(taskRequest.getColor());
            task.setUpdatedAt(Instant.now());

            logger.debug("CalDAV PUT: Saving recurrenceExceptions = '{}'", taskRequest.getRecurrenceExceptions());

            Task savedTask = taskRepository.save(task);

            // Sync reminders from ICS (if present in TaskRequest)
            if (taskRequest.getReminders() != null) {
                // Get existing reminders
                List<Reminder> existingReminders = reminderRepository.findByTask_UidOrderByReminderTimeAsc(eventUid);

                // Delete existing reminders
                if (!existingReminders.isEmpty()) {
                    reminderRepository.deleteAll(existingReminders);
                    reminderRepository.flush(); // Force immediate execution
                }

                // Add new reminders from VALARM
                for (com.privatecal.dto.ReminderRequest reminderRequest : taskRequest.getReminders()) {
                    reminderService.createReminderForTask(eventUid, reminderRequest);
                }

                // Force flush of inserts
                reminderRepository.flush();

                logger.info("CalDAV PUT: Synced {} reminder(s) for task {}", taskRequest.getReminders().size(), eventUid);
            }

            // Process override events (single occurrence modifications with RECURRENCE-ID)
            // These are saved as separate tasks linked to the master via EXDATE
            if (!overrideEvents.isEmpty()) {
                logger.info("CalDAV PUT: Processing {} override event(s) with RECURRENCE-ID", overrideEvents.size());

                for (VEvent overrideEvent : overrideEvents) {
                    try {
                        processOverrideEvent(overrideEvent, savedTask, targetCalendar, currentUser);
                    } catch (Exception e) {
                        logger.error("Error processing override event: {}", e.getMessage(), e);
                        // Continue with other overrides even if one fails
                    }
                }
            }

            logger.info("CalDAV PUT successful: task {} (UID: {})", savedTask.getId(), savedTask.getUid());
            return savedTask;

        } catch (Exception e) {
            logger.error("Error importing single event from ICS: {}", e.getMessage(), e);
            throw new IOException("Failed to import event", e);
        }
    }

    /**
     * Process override event (VEVENT with RECURRENCE-ID) from CalDAV PUT.
     * This represents a modified single occurrence of a recurring event.
     *
     * Per RFC 5545, the override event:
     * - Has the same UID as the master event
     * - Has a RECURRENCE-ID property indicating which occurrence it overrides
     * - Contains modified properties for that specific occurrence
     *
     * We save this as a separate task (like updateSingleOccurrence does).
     */
    private void processOverrideEvent(VEvent overrideEvent, Task masterTask,
                                     com.privatecal.entity.Calendar targetCalendar, User currentUser) {
        // Extract RECURRENCE-ID to determine which occurrence this overrides
        net.fortuna.ical4j.model.property.RecurrenceId recurrenceId =
            (net.fortuna.ical4j.model.property.RecurrenceId) overrideEvent.getProperty(Property.RECURRENCE_ID);

        if (recurrenceId == null) {
            logger.warn("Override event has no RECURRENCE-ID, skipping");
            return;
        }

        // Parse the override event as a TaskRequest
        TaskRequest overrideRequest = veventToTaskRequest(overrideEvent);

        // Get the RECURRENCE-ID date (this is the original occurrence time being overridden)
        java.util.Date recurrenceDate = recurrenceId.getDate();
        Instant recurrenceInstant = Instant.ofEpochMilli(recurrenceDate.getTime());

        // Convert to LocalDateTime in master task's timezone
        ZoneId masterZone = ZoneId.of(masterTask.getTaskTimezone());
        LocalDateTime occurrenceLocalDateTime = recurrenceInstant.atZone(masterZone).toLocalDateTime();

        logger.info("CalDAV PUT: Processing override for occurrence at {} (RECURRENCE-ID: {})",
                   occurrenceLocalDateTime, recurrenceInstant);

        // Check if an override task already exists for this occurrence
        // We identify override tasks by checking for tasks that:
        // 1. Belong to same user and calendar
        // 2. Start at the same time as the overridden occurrence
        // 3. Are not recurring (recurrenceRule is null)
        // 4. Are not the master task itself

        Optional<Task> existingOverride = taskRepository.findAll().stream()
            .filter(t -> t.getUser().getId().equals(currentUser.getId()))
            .filter(t -> t.getCalendar().getId().equals(targetCalendar.getId()))
            .filter(t -> t.getRecurrenceRule() == null || t.getRecurrenceRule().trim().isEmpty())
            .filter(t -> !t.getUid().equals(masterTask.getUid()))
            .filter(t -> t.getStartDatetimeLocal().equals(overrideRequest.getStartDatetimeLocal()))
            .findFirst();

        Task overrideTask;

        if (existingOverride.isPresent()) {
            // Update existing override
            overrideTask = existingOverride.get();
            logger.info("CalDAV PUT: Updating existing override task {}", overrideTask.getUid());
        } else {
            // Create new override task
            overrideTask = new Task();
            overrideTask.setUid(java.util.UUID.randomUUID().toString()); // New UID for override
            overrideTask.setUser(currentUser);
            overrideTask.setCalendar(targetCalendar);
            overrideTask.setCreatedAt(Instant.now());
            logger.info("CalDAV PUT: Creating new override task for occurrence at {}", occurrenceLocalDateTime);
        }

        // Set all properties from override event
        overrideTask.setTitle(overrideRequest.getTitle());
        overrideTask.setDescription(overrideRequest.getDescription());
        overrideTask.setLocation(overrideRequest.getLocation());
        overrideTask.setStartDatetimeLocal(overrideRequest.getStartDatetimeLocal());
        overrideTask.setEndDatetimeLocal(overrideRequest.getEndDatetimeLocal());
        overrideTask.setTaskTimezone(overrideRequest.getTimezone());
        overrideTask.setIsAllDay(overrideRequest.getIsAllDay());
        overrideTask.setColor(overrideRequest.getColor() != null ? overrideRequest.getColor() : masterTask.getColor());
        overrideTask.setRecurrenceRule(null); // Override is NOT recurring
        overrideTask.setRecurrenceExceptions(null);
        overrideTask.setUpdatedAt(Instant.now());

        taskRepository.save(overrideTask);

        logger.info("CalDAV PUT: Saved override task {} for occurrence {}",
                   overrideTask.getUid(), occurrenceLocalDateTime);

        // CRITICAL: Add this occurrence to master task's EXDATE
        // When an occurrence is modified (RECURRENCE-ID), it should NOT appear in the
        // master's recurring series anymore - only the override task should be visible.
        // Thunderbird doesn't add EXDATE for modified occurrences, only for deleted ones,
        // so we must add it ourselves to avoid duplicate occurrences.

        // We need to use RecurrenceService.addExceptionDate which finds the correct Instant
        recurrenceService.addExceptionDate(masterTask, occurrenceLocalDateTime);
        taskRepository.save(masterTask);

        logger.info("CalDAV PUT: Added EXDATE {} to master task {} for override occurrence",
                   occurrenceLocalDateTime, masterTask.getUid());
    }
}
