package com.privatecal.service;

import com.privatecal.entity.Task;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling recurring tasks using RFC 5545 RRULE format
 */
@Service
public class RecurrenceService {

    private static final Logger logger = LoggerFactory.getLogger(RecurrenceService.class);
    private static final int MAX_OCCURRENCES = 1000; // Safety limit

    /**
     * Expand recurring task into concrete occurrences within a date range
     *
     * @param task Task with optional recurrenceRule
     * @param rangeStart Start of expansion period
     * @param rangeEnd End of expansion period
     * @return List of task occurrences (1 for non-recurring, N for recurring)
     */
    public List<TaskOccurrence> expandRecurrences(Task task, Instant rangeStart, Instant rangeEnd) {
        if (task.getRecurrenceRule() == null || task.getRecurrenceRule().trim().isEmpty()) {
            // Non-recurring task: return single occurrence if in range
            if (isInRange(task, rangeStart, rangeEnd)) {
                return List.of(new TaskOccurrence(task, task.getStartDatetime(), task.getEndDatetime()));
            }
            return List.of();
        }

        // Parse and expand recurring task
        try {
            return expandRecurringTask(task, rangeStart, rangeEnd);
        } catch (ParseException e) {
            logger.error("Invalid RRULE for task {}: {}", task.getId(), task.getRecurrenceRule(), e);
            return List.of();
        } catch (Exception e) {
            logger.error("Error expanding recurrences for task {}: {}", task.getId(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Expand recurring task using ical4j
     */
    private List<TaskOccurrence> expandRecurringTask(Task task, Instant rangeStart, Instant rangeEnd)
            throws ParseException {

        // Parse RRULE
        RRule rrule = new RRule(task.getRecurrenceRule());
        Recur recur = rrule.getRecur();

        // Calculate task duration
        long durationMillis = ChronoUnit.MILLIS.between(task.getStartDatetime(), task.getEndDatetime());

        // Convert Instant to ical4j DateTime
        DateTime startDate = toDateTime(task.getStartDatetime());

        // Calculate period end (use recurrenceEnd if set, otherwise rangeEnd)
        Instant effectiveEnd = task.getRecurrenceEnd() != null
            ? Instant.ofEpochMilli(Math.min(task.getRecurrenceEnd().toEpochMilli(), rangeEnd.toEpochMilli()))
            : rangeEnd;

        DateTime periodEnd = toDateTime(effectiveEnd);

        // Generate occurrences
        DateList dates = recur.getDates(
            startDate,
            toDateTime(rangeStart),
            periodEnd,
            Value.DATE_TIME,
            MAX_OCCURRENCES
        );

        // Convert to TaskOccurrence list
        List<TaskOccurrence> occurrences = new ArrayList<>();
        for (Date date : dates) {
            Instant occStart = Instant.ofEpochMilli(date.getTime());
            Instant occEnd = occStart.plusMillis(durationMillis);

            // Only include if within range
            if (isOccurrenceInRange(occStart, occEnd, rangeStart, rangeEnd)) {
                occurrences.add(new TaskOccurrence(task, occStart, occEnd));
            }

            // Safety check
            if (occurrences.size() >= MAX_OCCURRENCES) {
                logger.warn("Reached max occurrences ({}) for task {}", MAX_OCCURRENCES, task.getId());
                break;
            }
        }

        return occurrences;
    }

    /**
     * Validate RRULE syntax (RFC 5545)
     */
    public boolean isValidRecurrenceRule(String rrule) {
        if (rrule == null || rrule.trim().isEmpty()) {
            return true; // Null/empty is valid (non-recurring task)
        }

        try {
            new RRule(rrule);
            return true;
        } catch (ParseException | IllegalArgumentException e) {
            logger.debug("Invalid RRULE: {}", rrule, e);
            return false;
        }
    }

    /**
     * Check if non-recurring task is within range
     */
    private boolean isInRange(Task task, Instant rangeStart, Instant rangeEnd) {
        return task.getStartDatetime().isBefore(rangeEnd) &&
               task.getEndDatetime().isAfter(rangeStart);
    }

    /**
     * Check if occurrence is within range
     */
    private boolean isOccurrenceInRange(Instant occStart, Instant occEnd,
                                       Instant rangeStart, Instant rangeEnd) {
        return occStart.isBefore(rangeEnd) && occEnd.isAfter(rangeStart);
    }

    /**
     * Convert Java Instant to ical4j DateTime (UTC)
     */
    private DateTime toDateTime(Instant instant) {
        DateTime dateTime = new DateTime(instant.toEpochMilli());
        dateTime.setUtc(true); // Ensure UTC timezone
        return dateTime;
    }

    /**
     * Inner class representing a task occurrence
     */
    public static class TaskOccurrence {
        private final Task task;
        private final Instant occurrenceStart;
        private final Instant occurrenceEnd;

        public TaskOccurrence(Task task, Instant occurrenceStart, Instant occurrenceEnd) {
            this.task = task;
            this.occurrenceStart = occurrenceStart;
            this.occurrenceEnd = occurrenceEnd;
        }

        public Task getTask() {
            return task;
        }

        public Instant getOccurrenceStart() {
            return occurrenceStart;
        }

        public Instant getOccurrenceEnd() {
            return occurrenceEnd;
        }

        /**
         * Get task ID
         */
        public Long getTaskId() {
            return task.getId();
        }

        /**
         * Check if this is the original occurrence
         */
        public boolean isOriginalOccurrence() {
            return occurrenceStart.equals(task.getStartDatetime());
        }

        /**
         * Get occurrence date (local date in system timezone)
         */
        public java.time.LocalDate getOccurrenceDate() {
            return occurrenceStart.atZone(ZoneId.systemDefault()).toLocalDate();
        }

        @Override
        public String toString() {
            return "TaskOccurrence{" +
                    "taskId=" + task.getId() +
                    ", title='" + task.getTitle() + '\'' +
                    ", occurrenceStart=" + occurrenceStart +
                    ", occurrenceEnd=" + occurrenceEnd +
                    '}';
        }
    }
}
