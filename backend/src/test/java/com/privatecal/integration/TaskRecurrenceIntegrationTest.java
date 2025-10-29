package com.privatecal.integration;

import com.privatecal.dto.TaskRequest;
import com.privatecal.dto.TaskResponse;
import com.privatecal.entity.Calendar;
import com.privatecal.entity.User;
import com.privatecal.repository.CalendarRepository;
import com.privatecal.repository.TaskRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.UserDetailsImpl;
import com.privatecal.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Task recurrence functionality
 * Tests the complete flow: TaskService → RecurrenceService → Database
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class TaskRecurrenceIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Calendar testCalendar;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("recurrence-test@example.com");
        testUser.setEmail("recurrence-test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser = userRepository.save(testUser);

        // Create default calendar for test user
        testCalendar = new Calendar();
        testCalendar.setUser(testUser);
        testCalendar.setName("Default Calendar");
        testCalendar.setSlug("default");
        testCalendar.setColor("#3788d8");
        testCalendar.setIsDefault(true);
        testCalendar.setIsVisible(true);
        testCalendar.setTimezone("UTC");
        testCalendar = calendarRepository.save(testCalendar);

        // Set up security context with UserDetailsImpl
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        taskRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateNonRecurringTask() {
        // Create non-recurring task
        TaskRequest request = createTaskRequest("Non-recurring meeting",
            2025, 10, 15, 10, 0,
            2025, 10, 15, 11, 0);

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response.getId());
        assertEquals("Non-recurring meeting", response.getTitle());
        assertNull(response.getRecurrenceRule());
        assertFalse(response.getIsRecurring());
    }

    @Test
    void testCreateRecurringTask() {
        // Create daily recurring task
        TaskRequest request = createTaskRequest("Daily standup", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        request.setRecurrenceRule("FREQ=DAILY;COUNT=7");

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response.getId());
        assertEquals("Daily standup", response.getTitle());
        assertEquals("FREQ=DAILY;COUNT=7", response.getRecurrenceRule());
        assertTrue(response.getIsRecurring());
    }

    @Test
    void testCreateRecurringTaskWithRecurrenceEnd() {
        // Create weekly task with end date
        TaskRequest request = createTaskRequest("Weekly review", 2025, 10, 1, 14, 0, 2025, 10, 1, 15, 0);
        request.setRecurrenceRule("FREQ=WEEKLY");
        request.setRecurrenceEnd(LocalDateTime.of(2025, 10, 31, 23, 59));

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response.getId());
        assertEquals("FREQ=WEEKLY", response.getRecurrenceRule());
        assertNotNull(response.getRecurrenceEnd());
        assertTrue(response.getIsRecurring());
    }

    @Test
    void testUpdateTaskToRecurring() {
        // Create non-recurring task
        TaskRequest createRequest = createTaskRequest("One-time meeting", 2025, 10, 15, 10, 0, 2025, 10, 15, 11, 0);
        TaskResponse created = taskService.createTask(createRequest);

        // Update to recurring
        TaskRequest updateRequest = createTaskRequest("Weekly meeting", 2025, 10, 15, 10, 0, 2025, 10, 15, 11, 0);
        updateRequest.setRecurrenceRule("FREQ=WEEKLY;COUNT=4");

        TaskResponse updated = taskService.updateTask(created.getId(), updateRequest);

        assertEquals("Weekly meeting", updated.getTitle());
        assertEquals("FREQ=WEEKLY;COUNT=4", updated.getRecurrenceRule());
        assertTrue(updated.getIsRecurring());
    }

    @Test
    void testUpdateRecurringTaskToNonRecurring() {
        // Create recurring task
        TaskRequest createRequest = createTaskRequest("Daily standup", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        createRequest.setRecurrenceRule("FREQ=DAILY;COUNT=5");
        TaskResponse created = taskService.createTask(createRequest);

        // Update to non-recurring
        TaskRequest updateRequest = createTaskRequest("One-time standup", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        updateRequest.setRecurrenceRule(null);

        TaskResponse updated = taskService.updateTask(created.getId(), updateRequest);

        assertEquals("One-time standup", updated.getTitle());
        assertNull(updated.getRecurrenceRule());
        assertFalse(updated.getIsRecurring());
    }

    @Test
    void testGetTasksInDateRangeExpandsRecurrences() {
        // Create daily recurring task (7 days)
        TaskRequest request = createTaskRequest("Daily task", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        request.setRecurrenceRule("FREQ=DAILY;COUNT=7");
        taskService.createTask(request);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return 7 expanded occurrences
        assertEquals(7, tasks.size());

        // Verify dates are consecutive days
        assertEquals(getLocalDateTime(2025, 10, 1, 9, 0), tasks.get(0).getStartDatetimeLocal());
        assertEquals(getLocalDateTime(2025, 10, 2, 9, 0), tasks.get(1).getStartDatetimeLocal());
        assertEquals(getLocalDateTime(2025, 10, 7, 9, 0), tasks.get(6).getStartDatetimeLocal());
    }

    @Test
    void testGetTasksInDateRangeWithMixedTasks() {
        // Create non-recurring task
        TaskRequest nonRecurring = createTaskRequest("One-time meeting", 2025, 10, 5, 14, 0, 2025, 10, 5, 15, 0);
        taskService.createTask(nonRecurring);

        // Create recurring task (3 occurrences)
        TaskRequest recurring = createTaskRequest("Daily standup", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        recurring.setRecurrenceRule("FREQ=DAILY;COUNT=3");
        taskService.createTask(recurring);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return 4 tasks total (1 non-recurring + 3 recurring occurrences)
        assertEquals(4, tasks.size());
    }

    @Test
    void testGetTasksInDateRangePartialRecurrence() {
        // Create daily task starting Oct 25 (7 days)
        TaskRequest request = createTaskRequest("Daily task", 2025, 10, 25, 10, 0, 2025, 10, 25, 10, 30);
        request.setRecurrenceRule("FREQ=DAILY;COUNT=10");
        taskService.createTask(request);

        // Query only for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return only October occurrences (Oct 25-31 = 7 days)
        assertEquals(7, tasks.size());
        assertEquals(getLocalDateTime(2025, 10, 25, 10, 0), tasks.get(0).getStartDatetimeLocal());
        assertEquals(getLocalDateTime(2025, 10, 31, 10, 0), tasks.get(6).getStartDatetimeLocal());
    }

    @Test
    void testGetTasksRecurrenceInFutureDateRange() {
        // Create weekly task starting Sept 25
        TaskRequest request = createTaskRequest("Weekly task (old)", 2025, 9, 25, 10, 0, 2025, 9, 25, 10, 30);
        request.setRecurrenceRule("FREQ=WEEKLY");
        taskService.createTask(request);

        // Query only for the second week of October
        Instant rangeStart = getInstant(2025, 10, 6, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 12, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return 1 occurrence
        assertEquals(1, tasks.size());
        assertEquals(getLocalDateTime(2025, 10, 9, 10, 0), tasks.get(0).getStartDatetimeLocal());
    }

    @Test
    void testInvalidRecurrenceRuleThrowsException() {
        TaskRequest request = createTaskRequest("Invalid recurring task", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        request.setRecurrenceRule("INVALID_RRULE");

        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(request);
        });
    }

    @Test
    void testRecurrenceEndBeforeStartThrowsException() {
        TaskRequest request = createTaskRequest("Invalid recurrence end", 2025, 10, 15, 9, 0, 2025, 10, 15, 9, 30);
        request.setRecurrenceRule("FREQ=DAILY");
        request.setRecurrenceEnd(LocalDateTime.of(2025, 10, 1, 0, 0)); // Before start

        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(request);
        });
    }

    @Test
    void testWeeklyRecurrenceExpansion() {
        // Create weekly task (Mondays only)
        TaskRequest request = createTaskRequest("Weekly meeting",
            2025, 10, 6, 14, 0,
            2025, 10, 6, 15, 0);
        request.setRecurrenceRule("FREQ=WEEKLY;COUNT=4");
        taskService.createTask(request);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return 4 Mondays
        assertEquals(4, tasks.size());
        assertEquals(getLocalDateTime(2025, 10, 6, 14, 0), tasks.get(0).getStartDatetimeLocal());
        assertEquals(getLocalDateTime(2025, 10, 13, 14, 0), tasks.get(1).getStartDatetimeLocal());
        assertEquals(getLocalDateTime(2025, 10, 20, 14, 0), tasks.get(2).getStartDatetimeLocal());
        assertEquals(getLocalDateTime(2025, 10, 27, 14, 0), tasks.get(3).getStartDatetimeLocal());
    }

    @Test
    void testRecurrencePreservesDuration() {
        // Create 2-hour recurring meeting
        TaskRequest request = createTaskRequest("Long meeting", 2025, 10, 1, 14, 0, 2025, 10, 1, 16, 0);
        request.setRecurrenceRule("FREQ=DAILY;COUNT=3");
        taskService.createTask(request);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Verify all occurrences have 2-hour duration
        for (TaskResponse task : tasks) {
            long durationMinutes = java.time.Duration.between(
                task.getStartDatetimeLocal(),
                task.getEndDatetimeLocal()
            ).toMinutes();
            assertEquals(120, durationMinutes); // 2 hours = 120 minutes
        }
    }

    @Test
    void testUpdateSingleOccurrenceCreatesExceptionAndNewTask() {
        // Create recurring task (3 daily occurrences)
        TaskRequest createRequest = createTaskRequest("Daily meeting", 2025, 10, 1, 10, 0, 2025, 10, 1, 11, 0);
        createRequest.setRecurrenceRule("FREQ=DAILY;COUNT=3");
        TaskResponse created = taskService.createTask(createRequest);

        // Get the second occurrence date
        LocalDateTime secondOccurrence = LocalDateTime.of(2025, 10, 2, 10, 0);

        // Update single occurrence with new data
        TaskRequest updateRequest = createTaskRequest("Modified meeting (one-time)",
            2025, 10, 2, 14, 0,
            2025, 10, 2, 15, 0);
        updateRequest.setDescription("This is a special one-time change");

        TaskResponse updatedSingle = taskService.updateSingleOccurrence(
            created.getId(),
            secondOccurrence,
            updateRequest
        );

        // Verify new task is created and is non-recurring
        assertNotNull(updatedSingle);
        assertNotEquals(created.getId(), updatedSingle.getId());
        assertEquals("Modified meeting (one-time)", updatedSingle.getTitle());
        assertEquals("This is a special one-time change", updatedSingle.getDescription());
        assertNull(updatedSingle.getRecurrenceRule());
        assertFalse(updatedSingle.getIsRecurring());
        assertEquals(getLocalDateTime(2025, 10, 2, 14, 0), updatedSingle.getStartDatetimeLocal());

        // Verify original task has exception date by accessing the entity directly
        com.privatecal.entity.Task originalTask = taskRepository.findById(created.getId())
            .orElseThrow(() -> new RuntimeException("Task not found"));
        assertNotNull(originalTask.getRecurrenceExceptions());
        assertTrue(originalTask.getRecurrenceExceptions().contains(secondOccurrence.toString()));
    }

    @Test
    void testUpdateSingleOccurrenceExcludesFromDateRange() {
        // Create recurring task (5 daily occurrences)
        TaskRequest createRequest = createTaskRequest("Daily task", 2025, 10, 1, 9, 0, 2025, 10, 1, 9, 30);
        createRequest.setRecurrenceRule("FREQ=DAILY;COUNT=5");
        TaskResponse created = taskService.createTask(createRequest);

        // Get range before update (should have 5 occurrences)
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 5, 23, 59);
        List<TaskResponse> tasksBefore = taskService.getTasksInDateRange(rangeStart, rangeEnd);
        assertEquals(5, tasksBefore.size());

        // Update the 3rd occurrence
        LocalDateTime thirdOccurrence = LocalDateTime.of(2025, 10, 3, 9, 0);
        TaskRequest updateRequest = createTaskRequest("Modified single task",
            2025, 10, 3, 15, 0,
            2025, 10, 3, 15, 30);

        taskService.updateSingleOccurrence(created.getId(), thirdOccurrence, updateRequest);

        // Get range after update (should still have 5 tasks total: 4 from series + 1 modified)
        List<TaskResponse> tasksAfter = taskService.getTasksInDateRange(rangeStart, rangeEnd);
        assertEquals(5, tasksAfter.size());

        // Verify the modified task exists with new data
        boolean foundModified = tasksAfter.stream()
            .anyMatch(t -> t.getTitle().equals("Modified single task")
                && t.getStartDatetimeLocal().equals(getLocalDateTime(2025, 10, 3, 15, 0)));
        assertTrue(foundModified, "Modified single occurrence should exist with new data");

        // Verify original occurrence is excluded (should not find task at 9:00)
        boolean foundOriginal = tasksAfter.stream()
            .anyMatch(t -> t.getStartDatetimeLocal().equals(getLocalDateTime(2025, 10, 3, 9, 0)));
        assertFalse(foundOriginal, "Original occurrence at 9:00 should be excluded");
    }

    @Test
    void testUpdateSingleOccurrencePreservesOtherOccurrences() {
        // Create weekly recurring task
        TaskRequest createRequest = createTaskRequest("Weekly standup", 2025, 10, 1, 10, 0, 2025, 10, 1, 10, 30);
        createRequest.setRecurrenceRule("FREQ=WEEKLY;COUNT=4");
        TaskResponse created = taskService.createTask(createRequest);

        // Update second occurrence
        LocalDateTime secondOccurrence = LocalDateTime.of(2025, 10, 8, 10, 0);
        TaskRequest updateRequest = createTaskRequest("Modified standup", 2025, 10, 8, 11, 0, 2025, 10, 8, 11, 30);

        taskService.updateSingleOccurrence(created.getId(), secondOccurrence, updateRequest);

        // Verify other occurrences remain unchanged
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);
        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should have 4 tasks total (3 from series + 1 modified)
        assertEquals(4, tasks.size());

        // First occurrence should be unchanged
        TaskResponse firstOccurrence = tasks.stream()
            .filter(t -> t.getStartDatetimeLocal().equals(getLocalDateTime(2025, 10, 1, 10, 0)))
            .findFirst()
            .orElse(null);
        assertNotNull(firstOccurrence);
        assertEquals("Weekly standup", firstOccurrence.getTitle());

        // Third and fourth occurrences should also be unchanged
        long unchangedCount = tasks.stream()
            .filter(t -> t.getTitle().equals("Weekly standup"))
            .count();
        assertEquals(3, unchangedCount);
    }

    // Helper methods

    private Instant getInstant(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime getLocalDateTime(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    private TaskRequest createTaskRequest(String title, int startYear, int startMonth, int startDay, int startHour, int startMinute,
                                         int endYear, int endMonth, int endDay, int endHour, int endMinute) {
        TaskRequest request = new TaskRequest();
        request.setTitle(title);
        request.setStartDatetimeLocal(getLocalDateTime(startYear, startMonth, startDay, startHour, startMinute));
        request.setEndDatetimeLocal(getLocalDateTime(endYear, endMonth, endDay, endHour, endMinute));
        request.setTimezone("UTC");
        return request;
    }
}
