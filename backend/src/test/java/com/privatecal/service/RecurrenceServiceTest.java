package com.privatecal.service;

import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecurrenceService
 */
class RecurrenceServiceTest {

    private RecurrenceService recurrenceService;
    private User testUser;

    @BeforeEach
    void setUp() {
        recurrenceService = new RecurrenceService();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test@example.com");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testNonRecurringTask() {
        // Create non-recurring task
        Task task = createTask(
            "Non-recurring task",
            getInstant(2025, 10, 15, 10, 0),
            getInstant(2025, 10, 15, 11, 0),
            null
        );

        // Expand in range that includes task
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return single occurrence
        assertEquals(1, occurrences.size());
        assertEquals(task.getStartDatetime(), occurrences.get(0).getOccurrenceStart());
        assertEquals(task.getEndDatetime(), occurrences.get(0).getOccurrenceEnd());
    }

    @Test
    void testNonRecurringTaskOutsideRange() {
        // Create task in October
        Task task = createTask(
            "Task in October",
            getInstant(2025, 10, 15, 10, 0),
            getInstant(2025, 10, 15, 11, 0),
            null
        );

        // Query for November (outside range)
        Instant rangeStart = getInstant(2025, 11, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 11, 30, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return empty list
        assertEquals(0, occurrences.size());
    }

    @Test
    void testDailyRecurrence() {
        // Create daily recurring task (every day for 7 days)
        Task task = createTask(
            "Daily standup",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30),
            "FREQ=DAILY;COUNT=7"
        );

        // Expand for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return 7 occurrences
        assertEquals(7, occurrences.size());

        // Verify first occurrence
        assertEquals(getInstant(2025, 10, 1, 9, 0), occurrences.get(0).getOccurrenceStart());
        assertEquals(getInstant(2025, 10, 1, 9, 30), occurrences.get(0).getOccurrenceEnd());

        // Verify last occurrence (day 7)
        assertEquals(getInstant(2025, 10, 7, 9, 0), occurrences.get(6).getOccurrenceStart());
        assertEquals(getInstant(2025, 10, 7, 9, 30), occurrences.get(6).getOccurrenceEnd());
    }

    @Test
    void testWeeklyRecurrence() {
        // Create weekly recurring task (every Monday and Wednesday)
        Task task = createTask(
            "Weekly meeting",
            getInstant(2025, 10, 6, 14, 0), // Monday Oct 6
            getInstant(2025, 10, 6, 15, 0),
            "FREQ=WEEKLY;BYDAY=MO,WE;COUNT=6"
        );

        // Expand for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return 6 occurrences (3 weeks × 2 days)
        assertEquals(6, occurrences.size());

        // Verify pattern: Mon, Wed, Mon, Wed, Mon, Wed
        assertEquals(getInstant(2025, 10, 6, 14, 0), occurrences.get(0).getOccurrenceStart());  // Mon
        assertEquals(getInstant(2025, 10, 8, 14, 0), occurrences.get(1).getOccurrenceStart());  // Wed
        assertEquals(getInstant(2025, 10, 13, 14, 0), occurrences.get(2).getOccurrenceStart()); // Mon
        assertEquals(getInstant(2025, 10, 15, 14, 0), occurrences.get(3).getOccurrenceStart()); // Wed
    }

    @Test
    void testMonthlyRecurrence() {
        // Create monthly recurring task (1st of each month, for 3 months)
        Task task = createTask(
            "Monthly report",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 10, 0),
            "FREQ=MONTHLY;COUNT=3"
        );

        // Expand for Oct-Dec
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 12, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return 3 occurrences (Oct, Nov, Dec)
        assertEquals(3, occurrences.size());

        assertEquals(getInstant(2025, 10, 1, 9, 0), occurrences.get(0).getOccurrenceStart());
        assertEquals(getInstant(2025, 11, 1, 9, 0), occurrences.get(1).getOccurrenceStart());
        assertEquals(getInstant(2025, 12, 1, 9, 0), occurrences.get(2).getOccurrenceStart());
    }

    @Test
    void testRecurrenceWithUntilDate() {
        // Create daily task with UNTIL date
        Task task = createTask(
            "Daily task",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30),
            "FREQ=DAILY;UNTIL=20251005T235959Z"
        );

        // Expand for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return 5 occurrences (Oct 1-5)
        assertEquals(5, occurrences.size());
        assertEquals(getInstant(2025, 10, 5, 9, 0), occurrences.get(4).getOccurrenceStart());
    }

    @Test
    void testRecurrenceWithRecurrenceEnd() {
        // Create daily task with recurrenceEnd field
        Task task = createTask(
            "Daily task",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30),
            "FREQ=DAILY"
        );
        task.setRecurrenceEnd(getInstant(2025, 10, 10, 23, 59));

        // Expand for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return 10 occurrences (Oct 1-10)
        assertEquals(10, occurrences.size());
        assertEquals(getInstant(2025, 10, 10, 9, 0), occurrences.get(9).getOccurrenceStart());
    }

    @Test
    void testRecurrencePartiallyInRange() {
        // Create weekly task starting in September
        Task task = createTask(
            "Weekly task",
            getInstant(2025, 9, 29, 10, 0),
            getInstant(2025, 9, 29, 11, 0),
            "FREQ=WEEKLY;COUNT=5"
        );

        // Query only October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Should return only October occurrences (4 weeks in October)
        assertEquals(4, occurrences.size());
        assertEquals(getInstant(2025, 10, 6, 10, 0), occurrences.get(0).getOccurrenceStart());
        assertEquals(getInstant(2025, 10, 27, 10, 0), occurrences.get(3).getOccurrenceStart());
    }

    @Test
    void testValidRecurrenceRule() {
        assertTrue(recurrenceService.isValidRecurrenceRule(null));
        assertTrue(recurrenceService.isValidRecurrenceRule(""));
        assertTrue(recurrenceService.isValidRecurrenceRule("FREQ=DAILY;COUNT=10"));
        assertTrue(recurrenceService.isValidRecurrenceRule("FREQ=WEEKLY;BYDAY=MO,WE,FR"));
        assertTrue(recurrenceService.isValidRecurrenceRule("FREQ=MONTHLY;BYMONTHDAY=1"));
    }

    @Test
    void testInvalidRecurrenceRule() {
        assertFalse(recurrenceService.isValidRecurrenceRule("FREQ=INVALIDFREQ"));
        assertFalse(recurrenceService.isValidRecurrenceRule("FREQ=DAILY;INVALIDPARAM=VALUE"));
        assertFalse(recurrenceService.isValidRecurrenceRule("NOTANRRULE"));
    }

    @Test
    void testTaskDurationPreserved() {
        // Create 2-hour task
        Task task = createTask(
            "Long meeting",
            getInstant(2025, 10, 1, 14, 0),
            getInstant(2025, 10, 1, 16, 0), // 2 hours
            "FREQ=DAILY;COUNT=3"
        );

        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<RecurrenceService.TaskOccurrence> occurrences =
            recurrenceService.expandRecurrences(task, rangeStart, rangeEnd);

        // Verify all occurrences maintain 2-hour duration
        for (RecurrenceService.TaskOccurrence occ : occurrences) {
            long durationHours = ChronoUnit.HOURS.between(
                occ.getOccurrenceStart(),
                occ.getOccurrenceEnd()
            );
            assertEquals(2, durationHours);
        }
    }

    // Helper methods

    private Task createTask(String title, Instant start, Instant end, String rrule) {
        Task task = new Task();
        task.setId(1L);
        task.setUser(testUser);
        task.setTitle(title);
        task.setStartDatetime(start);
        task.setEndDatetime(end);
        task.setRecurrenceRule(rrule);
        task.setColor("#3788d8");
        return task;
    }

    private Instant getInstant(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .toInstant(ZoneOffset.UTC);
    }
}
