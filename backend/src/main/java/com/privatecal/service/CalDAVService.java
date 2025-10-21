package com.privatecal.service;

import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Service for CalDAV integration: import/export iCalendar (.ics) format
 * Handles conversion between Task entities and iCalendar VEVENT/VTODO components
 */
@Service
public class CalDAVService {

    private static final Logger logger = LoggerFactory.getLogger(CalDAVService.class);
    private static final String PRODID = "-//PrivateCal//PrivateCal v0.11.0//EN";
    private static final int DEFAULT_TODO_DURATION_MINUTES = 30;

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
        for (Task task : tasks) {
            try {
                VEvent event = taskToVEvent(task);
                calendar.getComponents().add(event);
            } catch (Exception e) {
                logger.error("Error converting task {} to VEVENT: {}", task.getId(), e.getMessage(), e);
            }
        }

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

        // Create event with UID
        String uid = "task-" + task.getId() + "@privatecal.local";
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

        // Get SUMMARY (title)
        Summary summary = event.getSummary();
        taskRequest.setTitle(summary != null ? summary.getValue() : "Untitled Event");

        // Get DESCRIPTION
        Description description = event.getDescription();
        if (description != null) {
            taskRequest.setDescription(description.getValue());
        }

        // Get LOCATION
        net.fortuna.ical4j.model.property.Location location = event.getLocation();
        if (location != null) {
            taskRequest.setLocation(location.getValue());
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

        // Get SUMMARY (title) with [TODO] prefix
        Summary summary = todo.getSummary();
        String title = summary != null ? summary.getValue() : "Untitled Task";
        taskRequest.setTitle("[TODO] " + title);

        // Get DESCRIPTION
        Description description = todo.getDescription();
        if (description != null) {
            taskRequest.setDescription(description.getValue());
        }

        // Get LOCATION
        net.fortuna.ical4j.model.property.Location location = todo.getLocation();
        if (location != null) {
            taskRequest.setLocation(location.getValue());
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
}
