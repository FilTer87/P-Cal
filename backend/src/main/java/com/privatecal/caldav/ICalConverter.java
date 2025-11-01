package com.privatecal.caldav;

import com.privatecal.dto.ReminderRequest;
import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Converter between Task entities and iCalendar components (VEVENT/VTODO)
 * Handles bidirectional conversion for CalDAV import/export
 */
@Component
public class ICalConverter {

    private static final Logger logger = LoggerFactory.getLogger(ICalConverter.class);
    private static final String PRODID = "-//PrivateCal//PrivateCal v0.11.0//EN";
    private static final int DEFAULT_TODO_DURATION_MINUTES = 30;
    private static final int MAX_DESCRIPTION_LENGTH = 2500;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_LOCATION_LENGTH = 200;

    /**
     * Convert Task entity to VEvent component
     */
    public VEvent taskToVEvent(Task task) {
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

        // Add SUMMARY (title)
        if (task.getTitle() != null) {
            event.getProperties().add(new Summary(task.getTitle()));
        }

        // Add DESCRIPTION
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            event.getProperties().add(new Description(task.getDescription()));
        }

        // Add LOCATION
        if (task.getLocation() != null && !task.getLocation().isEmpty()) {
            event.getProperties().add(new net.fortuna.ical4j.model.property.Location(task.getLocation()));
        }

        // Set dates (DTSTART and DTEND) - RFC 5545 compliant with TZID
        if (Boolean.TRUE.equals(task.getIsAllDay())) {
            // All-day event: use DATE format (no time component)
            LocalDate startDate = task.getStartDatetimeLocal().toLocalDate();
            LocalDate endDate = task.getEndDatetimeLocal().toLocalDate();

            // Create ical4j Date objects (not DateTime) for all-day events
            // Format: YYYYMMDD (e.g., 20251021)
            try {
                String startDateStr = String.format("%04d%02d%02d",
                    startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth());
                String endDateStr = String.format("%04d%02d%02d",
                    endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth());

                event.getProperties().add(new DtStart(new net.fortuna.ical4j.model.Date(startDateStr)));
                event.getProperties().add(new DtEnd(new net.fortuna.ical4j.model.Date(endDateStr)));
            } catch (Exception e) {
                logger.error("Error creating all-day dates for task {}: {}", task.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create all-day event dates", e);
            }
        } else {
            // Timed event: use DATE-TIME format WITH TZID (RFC 5545 floating time)
            // This preserves local time across DST changes
            try {
                ZoneId taskZone = ZoneId.of(task.getTaskTimezone());

                // Get ical4j timezone registry
                net.fortuna.ical4j.model.TimeZoneRegistry registry =
                    net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry();
                net.fortuna.ical4j.model.TimeZone ical4jTimeZone = registry.getTimeZone(task.getTaskTimezone());

                // Create DateTime with timezone (floating time)
                ZonedDateTime zonedStart = task.getStartDatetimeLocal().atZone(taskZone);
                DateTime dtStart = new DateTime(zonedStart.toInstant().toEpochMilli());
                dtStart.setTimeZone(ical4jTimeZone);
                event.getProperties().add(new DtStart(dtStart));

                ZonedDateTime zonedEnd = task.getEndDatetimeLocal().atZone(taskZone);
                DateTime dtEnd = new DateTime(zonedEnd.toInstant().toEpochMilli());
                dtEnd.setTimeZone(ical4jTimeZone);
                event.getProperties().add(new DtEnd(dtEnd));

                logger.debug("Exported task {} with TZID: {}", task.getId(), task.getTaskTimezone());
            } catch (Exception e) {
                logger.error("Error creating datetime with timezone for task {}: {}", task.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create event dates with timezone", e);
            }
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

        // Set recurrence exceptions (EXDATE) if present
        // CRITICAL: EXDATE must match DTSTART format exactly (same timezone representation)
        if (task.getRecurrenceExceptions() != null && !task.getRecurrenceExceptions().trim().isEmpty()) {
            try {
                // Parse comma-separated Instant strings and create single EXDATE with DateList
                // Expected format: "2025-12-25T10:00:00Z,2026-01-01T10:00:00Z"
                String[] exceptionDates = task.getRecurrenceExceptions().split(",");

                // Create a single DateList to hold all exception dates
                net.fortuna.ical4j.model.DateList exDateList = new net.fortuna.ical4j.model.DateList();

                ZoneId taskZone = ZoneId.of(task.getTaskTimezone() != null ? task.getTaskTimezone() : "UTC");

                for (String exDateStr : exceptionDates) {
                    exDateStr = exDateStr.trim();
                    if (!exDateStr.isEmpty()) {
                        // Parse Instant (ISO-8601 with Z)
                        Instant exInstant = Instant.parse(exDateStr);

                        // Convert to LocalDateTime in task's timezone (matching DTSTART format)
                        LocalDateTime exLocalDateTime = exInstant.atZone(taskZone).toLocalDateTime();

                        // Create ical4j Date WITHOUT timezone info (will be added via TZID parameter)
                        // This creates a "floating time" that will be interpreted in the TZID context
                        String dateStr = String.format("%04d%02d%02dT%02d%02d%02d",
                            exLocalDateTime.getYear(),
                            exLocalDateTime.getMonthValue(),
                            exLocalDateTime.getDayOfMonth(),
                            exLocalDateTime.getHour(),
                            exLocalDateTime.getMinute(),
                            exLocalDateTime.getSecond());

                        logger.debug("Converting exception: {} (UTC) -> {} (local in {}) -> EXDATE:{}",
                            exInstant, exLocalDateTime, taskZone, dateStr);

                        DateTime ical4jExDate = new DateTime(dateStr);
                        exDateList.add(ical4jExDate);
                    }
                }

                // Add single EXDATE property with all exception dates
                if (!exDateList.isEmpty()) {
                    ExDate exDate = new ExDate(exDateList);

                    // Add TZID parameter to match DTSTART format
                    if (!Boolean.TRUE.equals(task.getIsAllDay()) && task.getTaskTimezone() != null) {
                        exDate.getParameters().add(new net.fortuna.ical4j.model.parameter.TzId(task.getTaskTimezone()));
                    }

                    event.getProperties().add(exDate);
                    logger.debug("Exported {} EXDATE(s) with TZID={} for task {}",
                        exDateList.size(), task.getTaskTimezone(), task.getId());
                }
            } catch (Exception e) {
                logger.warn("Error parsing recurrence exceptions for task {}: {}", task.getId(), e.getMessage());
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
     * Convert VEvent to TaskRequest DTO
     */
    public TaskRequest veventToTaskRequest(VEvent event) {
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

            // Extract timezone from DTSTART (RFC 5545 TZID parameter)
            String timezone = "UTC"; // Default
            if (startDate instanceof DateTime) {
                DateTime dateTime = (DateTime) startDate;
                if (dateTime.getTimeZone() != null) {
                    timezone = dateTime.getTimeZone().getID();
                    logger.debug("Imported event with TZID: {}", timezone);
                } else if (!dateTime.isUtc()) {
                    // Floating time without explicit timezone - use UTC
                    logger.warn("Event has floating time without TZID, defaulting to UTC");
                }
            }

            // Convert to LocalDateTime + timezone (floating time)
            Instant startInstant = Instant.ofEpochMilli(startDate.getTime());
            Instant endInstant = Instant.ofEpochMilli(endDate.getTime());

            ZoneId zoneId = ZoneId.of(timezone);
            taskRequest.setStartDatetimeLocal(startInstant.atZone(zoneId).toLocalDateTime());
            taskRequest.setEndDatetimeLocal(endInstant.atZone(zoneId).toLocalDateTime());
            taskRequest.setTimezone(timezone);

            logger.debug("Imported task: start={} end={} timezone={}",
                taskRequest.getStartDatetimeLocal(), taskRequest.getEndDatetimeLocal(), timezone);
        } else {
            // Fallback: use current time + 1 hour in UTC
            Instant now = Instant.now();
            taskRequest.setStartDatetimeLocal(now.atZone(ZoneId.of("UTC")).toLocalDateTime());
            taskRequest.setEndDatetimeLocal(now.plus(1, java.time.temporal.ChronoUnit.HOURS).atZone(ZoneId.of("UTC")).toLocalDateTime());
            taskRequest.setTimezone("UTC");
            taskRequest.setIsAllDay(false);
        }

        // Get recurrence rule
        RRule rrule = event.getProperty(Property.RRULE);
        if (rrule != null) {
            taskRequest.setRecurrenceRule(rrule.getValue());
        }

        // Get recurrence exceptions (EXDATE)
        var exDates = event.getProperties(Property.EXDATE);
        if (exDates != null && !exDates.isEmpty()) {
            List<String> exceptionInstantStrings = new ArrayList<>();

            for (Object exDateObj : exDates) {
                if (exDateObj instanceof ExDate) {
                    ExDate exDate = (ExDate) exDateObj;
                    net.fortuna.ical4j.model.DateList dates = exDate.getDates();
                    if (dates != null) {
                        for (Object dateObj : dates) {
                            try {
                                Instant instant;
                                if (dateObj instanceof DateTime) {
                                    DateTime dt = (DateTime) dateObj;
                                    instant = Instant.ofEpochMilli(dt.getTime());
                                } else if (dateObj instanceof net.fortuna.ical4j.model.Date) {
                                    // Date only (all-day)
                                    net.fortuna.ical4j.model.Date d = (net.fortuna.ical4j.model.Date) dateObj;
                                    instant = Instant.ofEpochMilli(d.getTime());
                                } else {
                                    continue; // Skip unknown types
                                }
                                // Store as Instant (ISO-8601 with Z)
                                exceptionInstantStrings.add(instant.toString());
                            } catch (Exception e) {
                                logger.warn("Error parsing EXDATE for event {}: {}", eventId, e.getMessage());
                            }
                        }
                    }
                }
            }

            if (!exceptionInstantStrings.isEmpty()) {
                taskRequest.setRecurrenceExceptions(String.join(",", exceptionInstantStrings));
                logger.debug("Imported {} EXDATE(s) for event {}", exceptionInstantStrings.size(), eventId);
            }
        }

        // Get color (Apple extension)
        Property colorProp = event.getProperty("X-APPLE-CALENDAR-COLOR");
        if (colorProp != null) {
            taskRequest.setColor(colorProp.getValue());
        }

        // Get VALARM components (reminders)
        // CalDAV clients like Thunderbird send reminders as VALARM with TRIGGER property
        var alarms = event.getAlarms();
        if (alarms != null && !alarms.isEmpty()) {
            List<ReminderRequest> reminderRequests = new ArrayList<>();

            for (VAlarm alarm : alarms) {
                try {
                    // Extract TRIGGER property (RFC 5545)
                    // Only handle relative triggers (e.g., TRIGGER:-PT15M = 15 minutes before)
                    // Ignore absolute triggers and triggers relative to END
                    net.fortuna.ical4j.model.property.Trigger trigger = alarm.getTrigger();
                    if (trigger != null && trigger.getDuration() != null) {
                        // Convert ical4j Duration to minutes
                        // Examples: -PT15M → 15 minutes before
                        //          -PT1H → 60 minutes before
                        //          -P1D → 1440 minutes before (1 day)
                        java.time.temporal.TemporalAmount duration = trigger.getDuration();
                        // Convert to java.time.Duration by parsing string representation
                        java.time.Duration javaDuration = java.time.Duration.parse(duration.toString());
                        long minutes = Math.abs(javaDuration.toMinutes());

                        // Only add if offset is positive (before event) and reasonable (max 1 month)
                        if (minutes > 0 && minutes <= (31*24*60)) { // Max 1 month
                            // Use EMAIL as default notification type regardless of ACTION value
                            // (CalDAV ACTION types: DISPLAY, EMAIL, AUDIO don't match with our notification system)
                            ReminderRequest reminderRequest =
                                new ReminderRequest((int) minutes, com.privatecal.dto.NotificationType.EMAIL);
                            reminderRequests.add(reminderRequest);
                            logger.debug("Imported VALARM for event {}: {} minutes before", eventId, minutes);
                        } else if (minutes > (31*24*60)) {
                            logger.warn("Skipping VALARM for event {}: offset too large ({} minutes, max {})", eventId, minutes, (31*24*60));
                        }
                    } else {
                        logger.debug("Skipping VALARM for event {}: absolute or END-relative trigger not supported", eventId);
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing VALARM for event {}: {}", eventId, e.getMessage());
                }
            }

            if (!reminderRequests.isEmpty()) {
                taskRequest.setReminders(reminderRequests);
                logger.info("Imported {} reminder(s) for event {}", reminderRequests.size(), eventId);
            }
        }

        return taskRequest;
    }

    /**
     * Convert VTODO to TaskRequest with hybrid logic:
     * - If DUE has time component → 30-minute timed task
     * - If DUE is date-only or absent → all-day task
     */
    public TaskRequest vtodoToTaskRequest(VToDo todo) {
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

        // Get DUE date (treat as end time)
        Due due = todo.getProperty(Property.DUE);

        if (due != null && due.getDate() != null) {
            Date dueDate = due.getDate();

            // Check if DUE has time component
            boolean hasTimeComponent = dueDate instanceof DateTime;
            taskRequest.setIsAllDay(!hasTimeComponent);

            // Extract timezone
            String timezone = "UTC"; // Default
            if (hasTimeComponent) {
                DateTime dateTime = (DateTime) dueDate;
                if (dateTime.getTimeZone() != null) {
                    timezone = dateTime.getTimeZone().getID();
                }
            }

            // Convert DUE to LocalDateTime
            Instant dueInstant = Instant.ofEpochMilli(dueDate.getTime());
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime dueDatetime = dueInstant.atZone(zoneId).toLocalDateTime();

            if (hasTimeComponent) {
                // Timed task: set start = due - 30 minutes, end = due
                taskRequest.setStartDatetimeLocal(dueDatetime.minusMinutes(DEFAULT_TODO_DURATION_MINUTES));
                taskRequest.setEndDatetimeLocal(dueDatetime);
            } else {
                // All-day task: set to full day
                LocalDateTime dayStart = dueDatetime.toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);
                taskRequest.setStartDatetimeLocal(dayStart);
                taskRequest.setEndDatetimeLocal(dayEnd);
            }

            taskRequest.setTimezone(timezone);

            logger.debug("Imported VTODO: due={} timezone={} isAllDay={}",
                dueDatetime, timezone, taskRequest.getIsAllDay());
        } else {
            // No DUE → all-day task for today
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            taskRequest.setStartDatetimeLocal(today.atStartOfDay());
            taskRequest.setEndDatetimeLocal(today.plusDays(1).atStartOfDay());
            taskRequest.setTimezone("UTC");
            taskRequest.setIsAllDay(true);
            logger.debug("VTODO without DUE: using today");
        }

        // Get recurrence rule
        RRule rrule = todo.getProperty(Property.RRULE);
        if (rrule != null) {
            taskRequest.setRecurrenceRule(rrule.getValue());
        }

        // Get color (Apple extension)
        Property colorProp = todo.getProperty("X-APPLE-CALENDAR-COLOR");
        if (colorProp != null) {
            taskRequest.setColor(colorProp.getValue());
        }

        return taskRequest;
    }

    /**
     * Truncate field to max length and log warning
     */
    private String truncateField(String value, int maxLength, String fieldName, String itemContext) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;

        logger.warn("Truncating {} for {}: {} chars → {} chars",
            fieldName, itemContext, value.length(), maxLength);
        return value.substring(0, maxLength);
    }

    /**
     * Generate deterministic UID for events/todos without UID
     */
    private String generateDeterministicUid(String summary, Date dtStart) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            String input = (summary != null ? summary : "untitled") +
                          (dtStart != null ? dtStart.toString() : "");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hexString.append(String.format("%02x", hash[i]));
            }
            return "privatecal-generated-" + hexString.toString();
        } catch (Exception e) {
            return "privatecal-generated-" + java.util.UUID.randomUUID().toString();
        }
    }
}
