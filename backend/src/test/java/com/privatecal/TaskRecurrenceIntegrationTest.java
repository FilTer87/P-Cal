package com.privatecal;

import com.privatecal.dto.TaskRequest;
import com.privatecal.dto.TaskResponse;
import com.privatecal.entity.User;
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
    private PasswordEncoder passwordEncoder;

    private User testUser;

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
        userRepository.deleteAll();
    }

    @Test
    void testCreateNonRecurringTask() {
        // Create non-recurring task
        TaskRequest request = new TaskRequest(
            "Non-recurring meeting",
            getInstant(2025, 10, 15, 10, 0),
            getInstant(2025, 10, 15, 11, 0)
        );

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response.getId());
        assertEquals("Non-recurring meeting", response.getTitle());
        assertNull(response.getRecurrenceRule());
        assertFalse(response.getIsRecurring());
    }

    @Test
    void testCreateRecurringTask() {
        // Create daily recurring task
        TaskRequest request = new TaskRequest(
            "Daily standup",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30)
        );
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
        TaskRequest request = new TaskRequest(
            "Weekly review",
            getInstant(2025, 10, 1, 14, 0),
            getInstant(2025, 10, 1, 15, 0)
        );
        request.setRecurrenceRule("FREQ=WEEKLY");
        request.setRecurrenceEnd(getInstant(2025, 10, 31, 23, 59));

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response.getId());
        assertEquals("FREQ=WEEKLY", response.getRecurrenceRule());
        assertNotNull(response.getRecurrenceEnd());
        assertTrue(response.getIsRecurring());
    }

    @Test
    void testUpdateTaskToRecurring() {
        // Create non-recurring task
        TaskRequest createRequest = new TaskRequest(
            "One-time meeting",
            getInstant(2025, 10, 15, 10, 0),
            getInstant(2025, 10, 15, 11, 0)
        );
        TaskResponse created = taskService.createTask(createRequest);

        // Update to recurring
        TaskRequest updateRequest = new TaskRequest(
            "Weekly meeting",
            getInstant(2025, 10, 15, 10, 0),
            getInstant(2025, 10, 15, 11, 0)
        );
        updateRequest.setRecurrenceRule("FREQ=WEEKLY;COUNT=4");

        TaskResponse updated = taskService.updateTask(created.getId(), updateRequest);

        assertEquals("Weekly meeting", updated.getTitle());
        assertEquals("FREQ=WEEKLY;COUNT=4", updated.getRecurrenceRule());
        assertTrue(updated.getIsRecurring());
    }

    @Test
    void testUpdateRecurringTaskToNonRecurring() {
        // Create recurring task
        TaskRequest createRequest = new TaskRequest(
            "Daily standup",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30)
        );
        createRequest.setRecurrenceRule("FREQ=DAILY;COUNT=5");
        TaskResponse created = taskService.createTask(createRequest);

        // Update to non-recurring
        TaskRequest updateRequest = new TaskRequest(
            "One-time standup",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30)
        );
        updateRequest.setRecurrenceRule(null);

        TaskResponse updated = taskService.updateTask(created.getId(), updateRequest);

        assertEquals("One-time standup", updated.getTitle());
        assertNull(updated.getRecurrenceRule());
        assertFalse(updated.getIsRecurring());
    }

    @Test
    void testGetTasksInDateRangeExpandsRecurrences() {
        // Create daily recurring task (7 days)
        TaskRequest request = new TaskRequest(
            "Daily task",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30)
        );
        request.setRecurrenceRule("FREQ=DAILY;COUNT=7");
        taskService.createTask(request);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return 7 expanded occurrences
        assertEquals(7, tasks.size());

        // Verify dates are consecutive days
        assertEquals(getInstant(2025, 10, 1, 9, 0), tasks.get(0).getStartDatetime());
        assertEquals(getInstant(2025, 10, 2, 9, 0), tasks.get(1).getStartDatetime());
        assertEquals(getInstant(2025, 10, 7, 9, 0), tasks.get(6).getStartDatetime());
    }

    @Test
    void testGetTasksInDateRangeWithMixedTasks() {
        // Create non-recurring task
        TaskRequest nonRecurring = new TaskRequest(
            "One-time meeting",
            getInstant(2025, 10, 5, 14, 0),
            getInstant(2025, 10, 5, 15, 0)
        );
        taskService.createTask(nonRecurring);

        // Create recurring task (3 occurrences)
        TaskRequest recurring = new TaskRequest(
            "Daily standup",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30)
        );
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
        TaskRequest request = new TaskRequest(
            "Daily task",
            getInstant(2025, 10, 25, 10, 0),
            getInstant(2025, 10, 25, 10, 30)
        );
        request.setRecurrenceRule("FREQ=DAILY;COUNT=10");
        taskService.createTask(request);

        // Query only for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return only October occurrences (Oct 25-31 = 7 days)
        assertEquals(7, tasks.size());
        assertEquals(getInstant(2025, 10, 25, 10, 0), tasks.get(0).getStartDatetime());
        assertEquals(getInstant(2025, 10, 31, 10, 0), tasks.get(6).getStartDatetime());
    }

    @Test
    void testInvalidRecurrenceRuleThrowsException() {
        TaskRequest request = new TaskRequest(
            "Invalid recurring task",
            getInstant(2025, 10, 1, 9, 0),
            getInstant(2025, 10, 1, 9, 30)
        );
        request.setRecurrenceRule("INVALID_RRULE");

        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(request);
        });
    }

    @Test
    void testRecurrenceEndBeforeStartThrowsException() {
        TaskRequest request = new TaskRequest(
            "Invalid recurrence end",
            getInstant(2025, 10, 15, 9, 0),
            getInstant(2025, 10, 15, 9, 30)
        );
        request.setRecurrenceRule("FREQ=DAILY");
        request.setRecurrenceEnd(getInstant(2025, 10, 1, 0, 0)); // Before start

        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(request);
        });
    }

    @Test
    void testWeeklyRecurrenceExpansion() {
        // Create weekly task (Mondays only)
        TaskRequest request = new TaskRequest(
            "Weekly meeting",
            getInstant(2025, 10, 6, 14, 0), // Monday Oct 6
            getInstant(2025, 10, 6, 15, 0)
        );
        request.setRecurrenceRule("FREQ=WEEKLY;COUNT=4");
        taskService.createTask(request);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Should return 4 Mondays
        assertEquals(4, tasks.size());
        assertEquals(getInstant(2025, 10, 6, 14, 0), tasks.get(0).getStartDatetime());
        assertEquals(getInstant(2025, 10, 13, 14, 0), tasks.get(1).getStartDatetime());
        assertEquals(getInstant(2025, 10, 20, 14, 0), tasks.get(2).getStartDatetime());
        assertEquals(getInstant(2025, 10, 27, 14, 0), tasks.get(3).getStartDatetime());
    }

    @Test
    void testRecurrencePreservesDuration() {
        // Create 2-hour recurring meeting
        TaskRequest request = new TaskRequest(
            "Long meeting",
            getInstant(2025, 10, 1, 14, 0),
            getInstant(2025, 10, 1, 16, 0) // 2 hours
        );
        request.setRecurrenceRule("FREQ=DAILY;COUNT=3");
        taskService.createTask(request);

        // Query for October
        Instant rangeStart = getInstant(2025, 10, 1, 0, 0);
        Instant rangeEnd = getInstant(2025, 10, 31, 23, 59);

        List<TaskResponse> tasks = taskService.getTasksInDateRange(rangeStart, rangeEnd);

        // Verify all occurrences have 2-hour duration
        for (TaskResponse task : tasks) {
            long durationMinutes = java.time.Duration.between(
                task.getStartDatetime(),
                task.getEndDatetime()
            ).toMinutes();
            assertEquals(120, durationMinutes); // 2 hours = 120 minutes
        }
    }

    // Helper methods

    private Instant getInstant(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .toInstant(ZoneOffset.UTC);
    }
}
