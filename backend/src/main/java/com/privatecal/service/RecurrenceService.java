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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
                Instant taskStart = task.getStartDatetimeAsInstant();
                Instant taskEnd = task.getEndDatetimeAsInstant();
                return List.of(new TaskOccurrence(task, taskStart, taskEnd));
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
     * Expand recurring task using ical4j with floating time (DST-safe).
     *
     * The key insight: recurring tasks maintain their LOCAL time across DST changes.
     * A task at "15:00" recurs at "15:00" even when DST changes the UTC offset.
     *
     * We use ical4j with the task's timezone so it handles DST transitions correctly.
     */
    private List<TaskOccurrence> expandRecurringTask(Task task, Instant rangeStart, Instant rangeEnd)
            throws ParseException {

        // Parse RRULE
        RRule rrule = new RRule(task.getRecurrenceRule());
        Recur recur = rrule.getRecur();

        // Calculate task duration in local time (duration stays constant across DST)
        long durationMillis = ChronoUnit.MILLIS.between(
            task.getStartDatetimeLocal(),
            task.getEndDatetimeLocal()
        );

        // Get task timezone for floating time calculations
        ZoneId taskZone = ZoneId.of(task.getTaskTimezone());

        // Convert local datetime to ical4j DateTime WITH timezone
        // This ensures ical4j respects DST transitions
        DateTime startDate = toDateTimeWithZone(task.getStartDatetimeLocal(), taskZone);

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
            Instant taskStart = task.getStartDatetimeAsInstant();
            if (taskStart.isAfter(afterTime)) {
                return new TaskOccurrence(task, taskStart, task.getEndDatetimeAsInstant());
            }
            return null;
        }

        try {
            // Parse RRULE
            RRule rrule = new RRule(task.getRecurrenceRule());
            Recur recur = rrule.getRecur();

            // Calculate task duration in local time
            long durationMillis = ChronoUnit.MILLIS.between(
                task.getStartDatetimeLocal(),
                task.getEndDatetimeLocal()
            );

            // Get task timezone
            ZoneId taskZone = ZoneId.of(task.getTaskTimezone());

            // Convert to ical4j DateTime WITH timezone (floating time)
            DateTime startDate = toDateTimeWithZone(task.getStartDatetimeLocal(), taskZone);
            // Add 1 second to afterTime to exclude the current occurrence
            DateTime searchFrom = toDateTime(afterTime.plusSeconds(1));

            // Calculate effective end date
            // Use recurrenceEnd if set, otherwise search up to 2 years from afterTime
            Instant effectiveEnd = task.getRecurrenceEnd() != null
                ? task.getRecurrenceEnd()
                : afterTime.plus(730, ChronoUnit.DAYS); // 2 years

            DateTime periodEnd = toDateTime(effectiveEnd);

            if (logger.isDebugEnabled()) {
                logger.debug("🔍 getNextOccurrence DEBUG:");
                logger.debug("  task.getStartDatetimeLocal()={}", task.getStartDatetimeLocal());
                logger.debug("  task.getTaskTimezone()={}", task.getTaskTimezone());
                logger.debug("  afterTime={}", afterTime);
                logger.debug("  afterTime+1s={}", afterTime.plusSeconds(1));
                logger.debug("  startDate (ical4j)={}", startDate);
                logger.debug("  searchFrom (ical4j)={}", searchFrom);
                logger.debug("  periodEnd (ical4j)={}", periodEnd);
                logger.debug("  RRULE={}", task.getRecurrenceRule());
            }

            // Generate occurrences starting from afterTime + 1 second
            DateList dates = recur.getDates(
                startDate,
                searchFrom,
                periodEnd,
                Value.DATE_TIME,
                1 // We only need the first occurrence
            );

            if (logger.isDebugEnabled()) {
                logger.debug("  Found {} dates from recur.getDates()", dates.size());
                if (!dates.isEmpty()) {
                    logger.debug("  First date: {}", dates.get(0));
                }
            }

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
        Instant taskStart = task.getStartDatetimeAsInstant();
        Instant taskEnd = task.getEndDatetimeAsInstant();
        return taskStart.isBefore(rangeEnd) && taskEnd.isAfter(rangeStart);
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
        // Create DateTime in UTC timezone to avoid timezone conversion issues
        DateTime dateTime = new DateTime(true); // true = UTC
        dateTime.setTime(instant.toEpochMilli());
        return dateTime;
    }

    /**
     * Convert LocalDateTime + ZoneId to ical4j DateTime with timezone.
     * This preserves the local time and allows ical4j to handle DST correctly.
     *
     * Example: 2025-10-20T15:00 + Europe/Rome
     * - ical4j will generate occurrences at "15:00 local" for each day
     * - DST transitions are handled automatically by ical4j
     */
    private DateTime toDateTimeWithZone(LocalDateTime localDateTime, ZoneId zoneId) {
        try {
            // Convert to ZonedDateTime
            ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);

            // Create ical4j TimeZone
            net.fortuna.ical4j.model.TimeZone ical4jTimeZone =
                net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance()
                    .createRegistry()
                    .getTimeZone(zoneId.getId());

            // Create DateTime with timezone
            DateTime dateTime = new DateTime(zonedDateTime.toInstant().toEpochMilli());
            dateTime.setTimeZone(ical4jTimeZone);

            return dateTime;
        } catch (Exception e) {
            logger.error("Error creating DateTime with zone {}: {}", zoneId, e.getMessage());
            // Fallback to UTC
            return toDateTime(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
        }
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
         * Get task UID
         */
        public String getTaskId() {
            return task.getId();
        }

        /**
         * Check if this is the original occurrence
         */
        public boolean isOriginalOccurrence() {
            return occurrenceStart.equals(task.getStartDatetimeAsInstant());
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
