package com.privatecal.service;

import com.privatecal.dto.NotificationType;
import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Calendar;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CalDAVService
 * Tests import/export of iCalendar (.ics) format
 */
class CalDAVServiceTest {

    private CalDAVService calDAVService;
    private User testUser;
    private Calendar testCalendar;

    @BeforeEach
    void setUp() {
        calDAVService = new CalDAVService();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testCalendar = new Calendar();
        testCalendar.setId(1L);
        testCalendar.setUser(testUser);
        testCalendar.setName("Test Calendar");
        testCalendar.setSlug("test");
        testCalendar.setIsDefault(true);
    }

    // ==================== EXPORT TESTS ====================

    @Test
    void testExportSingleTask() throws IOException {
        // Create a simple task
        Task task = createTask(
            "Meeting with team",
            "Discuss project roadmap",
            getInstant(2025, 10, 21, 14, 0),
            getInstant(2025, 10, 21, 15, 0),
            false
        );

        List<Task> tasks = List.of(task);

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");

        // Verify result
        assertNotNull(icsData);
        assertTrue(icsData.length > 0);

        // Parse the exported ICS
        String icsString = new String(icsData);
        assertTrue(icsString.contains("BEGIN:VCALENDAR"));
        assertTrue(icsString.contains("BEGIN:VEVENT"));
        assertTrue(icsString.contains("SUMMARY:Meeting with team"));
        assertTrue(icsString.contains("DESCRIPTION:Discuss project roadmap"));
        assertTrue(icsString.contains("END:VEVENT"));
        assertTrue(icsString.contains("END:VCALENDAR"));
    }

    @Test
    void testExportAllDayTask() throws IOException {
        // Create an all-day task
        LocalDate date = LocalDate.of(2025, 10, 21);
        Task task = createTask(
            "Birthday",
            "John's birthday",
            date.atStartOfDay(ZoneOffset.UTC).toInstant(),
            date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            true // all-day
        );

        List<Task> tasks = List.of(task);

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");
        String icsString = new String(icsData);

        // Verify all-day event uses DATE format (not DATE-TIME)
        // Should be DTSTART:20251021 (without time) or DTSTART;VALUE=DATE:20251021
        assertTrue(icsString.contains("DTSTART:20251021") || icsString.contains("DTSTART;VALUE=DATE:20251021"),
                "DTSTART should be in DATE format (YYYYMMDD) without time. Actual output:\n" + icsString);
        assertFalse(icsString.contains("DTSTART:20251021T"), "DTSTART should not contain time component");
        assertTrue(icsString.contains("SUMMARY:Birthday"));
    }

    @Test
    void testExportRecurringTask() throws IOException {
        // Create recurring task
        Task task = createTask(
            "Weekly standup",
            "Team sync meeting",
            getInstant(2025, 10, 21, 9, 0),
            getInstant(2025, 10, 21, 9, 30),
            false
        );
        task.setRecurrenceRule("FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10");

        List<Task> tasks = List.of(task);

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");
        String icsString = new String(icsData);

        // Verify RRULE is present (check components separately as ical4j may reorder)
        assertTrue(icsString.contains("RRULE:"), "RRULE property not found");
        assertTrue(icsString.contains("FREQ=WEEKLY"), "FREQ=WEEKLY not found in RRULE");
        assertTrue(icsString.contains("BYDAY=MO,WE,FR"), "BYDAY=MO,WE,FR not found in RRULE");
        assertTrue(icsString.contains("COUNT=10"), "COUNT=10 not found in RRULE");
    }

    @Test
    void testExportTaskWithReminder() throws IOException {
        // Create task with reminder
        Task task = createTask(
            "Important meeting",
            "Don't forget!",
            getInstant(2025, 10, 21, 14, 0),
            getInstant(2025, 10, 21, 15, 0),
            false
        );

        // Add reminder (15 minutes before)
        Reminder reminder = new Reminder();
        reminder.setTask(task);
        reminder.setReminderOffsetMinutes(15);
        reminder.setNotificationType(NotificationType.PUSH);
        task.getReminders().add(reminder);

        List<Task> tasks = List.of(task);

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");
        String icsString = new String(icsData);

        // Verify VALARM is present
        assertTrue(icsString.contains("BEGIN:VALARM"));
        assertTrue(icsString.contains("ACTION:DISPLAY"));
        assertTrue(icsString.contains("END:VALARM"));
    }

    @Test
    void testExportMultipleTasks() throws IOException {
        // Create multiple tasks
        List<Task> tasks = new ArrayList<>();

        tasks.add(createTask("Task 1", "Description 1",
            getInstant(2025, 10, 21, 10, 0),
            getInstant(2025, 10, 21, 11, 0), false));

        tasks.add(createTask("Task 2", "Description 2",
            getInstant(2025, 10, 22, 14, 0),
            getInstant(2025, 10, 22, 15, 0), false));

        tasks.add(createTask("Task 3", "Description 3",
            getInstant(2025, 10, 23, 9, 0),
            getInstant(2025, 10, 23, 10, 0), false));

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");
        String icsString = new String(icsData);

        // Count VEVENT occurrences
        int eventCount = icsString.split("BEGIN:VEVENT").length - 1;
        assertEquals(3, eventCount);
    }

    // ==================== IMPORT TESTS ====================

    @Test
    void testImportSimpleVEvent() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            UID:test-event-1@example.com
            DTSTART:20251021T140000Z
            DTEND:20251021T150000Z
            SUMMARY:Test Meeting
            DESCRIPTION:This is a test event
            LOCATION:Conference Room
            END:VEVENT
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertEquals("Test Meeting", task.getTitle());
        assertEquals("This is a test event", task.getDescription());
        assertEquals("Conference Room", task.getLocation());
        assertEquals(false, task.getIsAllDay());
        assertNotNull(task.getStartDatetime());
        assertNotNull(task.getEndDatetime());
    }

    @Test
    void testImportAllDayVEvent() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            UID:test-event-2@example.com
            DTSTART;VALUE=DATE:20251021
            DTEND;VALUE=DATE:20251022
            SUMMARY:All Day Event
            END:VEVENT
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertEquals("All Day Event", task.getTitle());
        assertEquals(true, task.getIsAllDay());
    }

    @Test
    void testImportRecurringVEvent() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            UID:test-recurring@example.com
            DTSTART:20251021T090000Z
            DTEND:20251021T093000Z
            SUMMARY:Daily Standup
            RRULE:FREQ=DAILY;COUNT=5
            END:VEVENT
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertEquals("Daily Standup", task.getTitle());
        assertEquals("FREQ=DAILY;COUNT=5", task.getRecurrenceRule());
    }

    @Test
    void testImportVTodoWithTime() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:test-todo-1@example.com
            DUE:20251021T170000Z
            SUMMARY:Complete documentation
            DESCRIPTION:Write user manual
            END:VTODO
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertTrue(task.getTitle().startsWith("[TODO]"));
        assertTrue(task.getTitle().contains("Complete documentation"));
        assertEquals("Write user manual", task.getDescription());
        assertEquals(false, task.getIsAllDay()); // Has time → timed task
        assertNotNull(task.getStartDatetime());
        assertNotNull(task.getEndDatetime());

        // Verify 30-minute duration
        long durationMinutes = java.time.Duration.between(
            task.getStartDatetime(), task.getEndDatetime()).toMinutes();
        assertEquals(30, durationMinutes);
    }

    @Test
    void testImportVTodoDateOnly() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:test-todo-2@example.com
            DUE;VALUE=DATE:20251021
            SUMMARY:Buy groceries
            END:VTODO
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertTrue(task.getTitle().contains("Buy groceries"));
        assertEquals(true, task.getIsAllDay()); // Date only → all-day
    }

    @Test
    void testImportVTodoNoDue() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:test-todo-3@example.com
            SUMMARY:Call John
            END:VTODO
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertTrue(task.getTitle().contains("Call John"));
        assertEquals(true, task.getIsAllDay()); // No DUE → all-day
    }

    @Test
    void testImportMixedVEventAndVTodo() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            UID:event-1@example.com
            DTSTART:20251021T140000Z
            DTEND:20251021T150000Z
            SUMMARY:Meeting
            END:VEVENT
            BEGIN:VTODO
            UID:todo-1@example.com
            DUE:20251022T120000Z
            SUMMARY:Submit report
            END:VTODO
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(2, taskRequests.size());

        // Verify event
        TaskRequest event = taskRequests.stream()
            .filter(t -> t.getTitle().equals("Meeting"))
            .findFirst()
            .orElseThrow();
        assertEquals(false, event.getIsAllDay());

        // Verify todo
        TaskRequest todo = taskRequests.stream()
            .filter(t -> t.getTitle().contains("[TODO]"))
            .findFirst()
            .orElseThrow();
        assertEquals(false, todo.getIsAllDay());
    }

    @Test
    void testImportEmptyCalendar() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(0, taskRequests.size());
    }

    @Test
    void testGenerateCalendarName() {
        String calendarName = calDAVService.generateCalendarName(testUser);
        assertEquals("testuser's Calendar", calendarName);
    }

    // ==================== UID PRESERVATION TESTS ====================

    @Test
    void testImportVEventPreservesExistingUid() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            UID:existing-uid-12345@example.com
            DTSTART:20251021T140000Z
            DTEND:20251021T150000Z
            SUMMARY:Meeting with UID
            DESCRIPTION:This event has a UID that should be preserved
            END:VEVENT
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertEquals("Meeting with UID", task.getTitle());
        assertEquals("existing-uid-12345@example.com", task.getUid(),
            "UID from VEVENT should be preserved exactly as provided");
    }

    @Test
    void testImportVEventGeneratesDeterministicUidWhenMissing() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            DTSTART:20251021T140000Z
            DTEND:20251021T150000Z
            SUMMARY:Meeting without UID
            DESCRIPTION:This event has no UID
            END:VEVENT
            END:VCALENDAR
            """;

        // Import twice to verify deterministic generation
        List<TaskRequest> taskRequests1 = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);
        List<TaskRequest> taskRequests2 = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests1.size());
        assertEquals(1, taskRequests2.size());

        TaskRequest task1 = taskRequests1.get(0);
        TaskRequest task2 = taskRequests2.get(0);

        assertNotNull(task1.getUid(), "UID should be generated when missing");
        assertNotNull(task2.getUid(), "UID should be generated when missing");
        assertTrue(task1.getUid().startsWith("privatecal-generated-"),
            "Generated UID should have correct prefix");
        assertEquals(task1.getUid(), task2.getUid(),
            "Generated UID should be deterministic (same input → same UID)");
    }

    @Test
    void testImportVTodoPreservesExistingUid() throws IOException {
        String ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:todo-uid-67890@example.com
            DUE:20251021T170000Z
            SUMMARY:Task with UID
            DESCRIPTION:This todo has a UID
            END:VTODO
            END:VCALENDAR
            """;

        List<TaskRequest> taskRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(ics.getBytes()), testUser);

        assertEquals(1, taskRequests.size());

        TaskRequest task = taskRequests.get(0);
        assertTrue(task.getTitle().contains("Task with UID"));
        assertEquals("todo-uid-67890@example.com", task.getUid(),
            "UID from VTODO should be preserved exactly as provided");
    }

    @Test
    void testExportTaskUsesSavedUid() throws IOException {
        // Create a task with a specific UID
        Task task = createTask(
            "Task with saved UID",
            "This task has a UID from import",
            getInstant(2025, 10, 21, 14, 0),
            getInstant(2025, 10, 21, 15, 0),
            false
        );
        task.setUid("imported-uid-12345@external.com");

        List<Task> tasks = List.of(task);

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");
        String icsString = new String(icsData);

        // Verify the saved UID is used in export
        assertTrue(icsString.contains("UID:imported-uid-12345@external.com"),
            "Export should use the saved UID from the task. Actual output:\n" + icsString);
    }

    @Test
    void testExportTaskWithoutUidGeneratesFallback() throws IOException {
        // Create a task without UID (legacy task)
        Task task = createTask(
            "Legacy task without UID",
            "This task has no UID",
            getInstant(2025, 10, 21, 14, 0),
            getInstant(2025, 10, 21, 15, 0),
            false
        );
        // Don't set UID (null)

        List<Task> tasks = List.of(task);

        // Export to ICS
        byte[] icsData = calDAVService.exportToICS(tasks, "Test Calendar");
        String icsString = new String(icsData);

        // Verify a fallback UID is generated
        assertTrue(icsString.contains("UID:test-uid-1"),
            "Export should generate fallback UID for legacy tasks. Actual output:\n" + icsString);
    }

    @Test
    void testRoundTripPreservesUid() throws IOException {
        // Step 1: Import event with UID
        String originalIcs = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VEVENT
            UID:roundtrip-test-uid@example.com
            DTSTART:20251021T140000Z
            DTEND:20251021T150000Z
            SUMMARY:Round Trip Test
            DESCRIPTION:Testing UID preservation through import/export cycle
            END:VEVENT
            END:VCALENDAR
            """;

        List<TaskRequest> importedRequests = calDAVService.importFromICS(
            new ByteArrayInputStream(originalIcs.getBytes()), testUser);

        assertEquals(1, importedRequests.size());
        TaskRequest importedRequest = importedRequests.get(0);
        assertEquals("roundtrip-test-uid@example.com", importedRequest.getUid());

        // Step 2: Create Task entity from TaskRequest (simulating save)
        Task task = createTask(
            importedRequest.getTitle(),
            importedRequest.getDescription(),
            importedRequest.getStartDatetime(),
            importedRequest.getEndDatetime(),
            importedRequest.getIsAllDay()
        );
        task.setUid(importedRequest.getUid()); // Preserve UID when saving

        // Step 3: Export the task back to ICS
        byte[] exportedIcs = calDAVService.exportToICS(List.of(task), "Test Calendar");
        String exportedIcsString = new String(exportedIcs);

        // Step 4: Verify UID is preserved in the export
        assertTrue(exportedIcsString.contains("UID:roundtrip-test-uid@example.com"),
            "UID should be preserved through complete import/export cycle");
    }

    // ==================== HELPER METHODS ====================

    private Task createTask(String title, String description,
                          Instant start, Instant end, boolean isAllDay) {
        Task task = new Task();
        task.setUid("test-uid-1");
        task.setUser(testUser);
        task.setCalendar(testCalendar);
        task.setTitle(title);
        task.setDescription(description);
        task.setStartDatetime(start);
        task.setEndDatetime(end);
        task.setIsAllDay(isAllDay);
        task.setColor("#3788d8");
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        return task;
    }

    private Instant getInstant(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .toInstant(ZoneOffset.UTC);
    }
}
