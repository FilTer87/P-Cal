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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Parse exception dates (EXDATE)
        Set<Instant> exceptions = parseExceptionDates(task.getRecurrenceExceptions());

        // Convert to TaskOccurrence list
        List<TaskOccurrence> occurrences = new ArrayList<>();
        for (Date date : dates) {
            Instant occStart = Instant.ofEpochMilli(date.getTime());
            Instant occEnd = occStart.plusMillis(durationMillis);

            // Skip if this occurrence is in the exception list
            if (exceptions.contains(occStart)) {
                logger.debug("Skipping exception date: {}", occStart);
                continue;
            }

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
     * Parse exception dates from comma-separated string
     * Format: "2025-10-17T10:00:00Z,2025-10-24T10:00:00Z"
     */
    private Set<Instant> parseExceptionDates(String exceptionsStr) {
        if (exceptionsStr == null || exceptionsStr.trim().isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(exceptionsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Instant::parse)
            .collect(Collectors.toSet());
    }

    /**
     * Add an exception date to the task (EXDATE)
     * @param task The task to modify
     * @param exceptionDate The occurrence date to exclude
     */
    public void addExceptionDate(Task task, Instant exceptionDate) {
        Set<Instant> exceptions = parseExceptionDates(task.getRecurrenceExceptions());
        exceptions.add(exceptionDate);
        task.setRecurrenceExceptions(formatExceptionDates(exceptions));
        logger.info("Added exception date {} to task {}", exceptionDate, task.getId());
    }

    /**
     * Format exception dates as comma-separated string
     */
    private String formatExceptionDates(Set<Instant> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return null;
        }

        return exceptions.stream()
            .sorted()
            .map(Instant::toString)
            .collect(Collectors.joining(","));
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
     * Get the next occurrence of a recurring task after a given time
     * Returns null if there are no more occurrences
     *
     * @param task The recurring task
     * @param afterTime Get the next occurrence after this time
     * @return The next TaskOccurrence or null if none exists
     */
    public TaskOccurrence getNextOccurrence(Task task, Instant afterTime) {
        logger.debug("getNextOccurrence for task {}: afterTime={}, recurrenceRule={}",
                    task.getId(), afterTime, task.getRecurrenceRule());

        if (task.getRecurrenceRule() == null || task.getRecurrenceRule().trim().isEmpty()) {
            // Non-recurring task - return the task itself if it's after the given time
            if (task.getStartDatetime().isAfter(afterTime)) {
                return new TaskOccurrence(task, task.getStartDatetime(), task.getEndDatetime());
            }
            return null;
        }

        try {
            // Parse RRULE
            RRule rrule = new RRule(task.getRecurrenceRule());
            Recur recur = rrule.getRecur();

            // Calculate task duration
            long durationMillis = ChronoUnit.MILLIS.between(task.getStartDatetime(), task.getEndDatetime());

            // Convert to ical4j DateTime
            DateTime startDate = toDateTime(task.getStartDatetime());
            // Add 1 second to afterTime to exclude the current occurrence
            DateTime searchFrom = toDateTime(afterTime.plusSeconds(1));

            // Calculate effective end date
            // Use recurrenceEnd if set, otherwise search up to 2 years from afterTime
            Instant effectiveEnd = task.getRecurrenceEnd() != null
                ? task.getRecurrenceEnd()
                : afterTime.plus(730, ChronoUnit.DAYS); // 2 years

            DateTime periodEnd = toDateTime(effectiveEnd);

            logger.debug("Searching for next occurrence: startDate={}, searchFrom={} (afterTime+1s), periodEnd={}",
                        startDate, searchFrom, periodEnd);

            // Generate occurrences starting from afterTime + 1 second
            DateList dates = recur.getDates(
                startDate,
                searchFrom,
                periodEnd,
                Value.DATE_TIME,
                1 // We only need the first occurrence
            );

            logger.debug("Found {} dates from recur.getDates()", dates.size());

            // Return the first occurrence after afterTime
            if (!dates.isEmpty()) {
                Date firstDate = dates.get(0);
                Instant occStart = Instant.ofEpochMilli(firstDate.getTime());
                Instant occEnd = occStart.plusMillis(durationMillis);

                logger.debug("First date found: {}, occStart={}, isAfter(afterTime)={}",
                            firstDate, occStart, occStart.isAfter(afterTime));

                // Make sure it's actually after the requested time
                if (occStart.isAfter(afterTime)) {
                    logger.info("Next occurrence found for task {}: {}", task.getId(), occStart);
                    return new TaskOccurrence(task, occStart, occEnd);
                } else {
                    logger.warn("Found occurrence {} is not after requested time {}", occStart, afterTime);
                }
            } else {
                logger.warn("No dates found for task {} with RRULE {} after {}",
                           task.getId(), task.getRecurrenceRule(), afterTime);
            }

            return null;
        } catch (ParseException e) {
            logger.error("Invalid RRULE for task {}: {}", task.getId(), task.getRecurrenceRule(), e);
            return null;
        } catch (Exception e) {
            logger.error("Error getting next occurrence for task {}: {}", task.getId(), e.getMessage(), e);
            return null;
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
