package com.privatecal;

import com.privatecal.dto.*;
import com.privatecal.entity.Calendar;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.User;
import com.privatecal.repository.CalendarRepository;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.UserDetailsImpl;
import com.privatecal.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the reminder duplication fix with explicit flush operations.
 * This test works with H2 but simulates the fix behavior for PostgreSQL.
 *
 * The main purpose is to verify that our fix logic (explicit deleteAll + flush)
 * works correctly without requiring Docker/Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskReminderUpdateWithFlushTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReminderRepository reminderRepository;

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
        // Create a test user
        testUser = new User();
        testUser.setUsername("flushtest@example.com");
        testUser.setEmail("flushtest@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setFirstName("Flush");
        testUser.setLastName("Test");
        testUser.setTimezone("UTC");
        testUser.setCreatedAt(LocalDateTime.now());
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

        // Set up security context
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testReminderUpdateWithFlushLogic() {
        // This test verifies that our flush-based fix works correctly

        // Create task with initial reminders
        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(15, NotificationType.PUSH));
        initialReminders.add(new ReminderRequest(30, NotificationType.EMAIL));
        taskRequest.setReminders(initialReminders);

        TaskResponse createdTask = taskService.createTask(taskRequest);
        String taskUid = createdTask.getId();

        // Verify initial state
        List<Reminder> remindersAfterCreate = reminderRepository.findByTask_UidOrderByReminderTimeAsc(taskUid);
        assertEquals(2, remindersAfterCreate.size());

        // Capture original reminder IDs
        List<Long> originalReminderIds = remindersAfterCreate.stream()
                .map(Reminder::getId)
                .sorted()
                .toList();

        // Update task with different reminders
        TaskRequest updateRequest = createTaskRequest();
        updateRequest.setTitle("Updated Task");
        List<ReminderRequest> updatedReminders = new ArrayList<>();
        updatedReminders.add(new ReminderRequest(10, NotificationType.EMAIL));
        updatedReminders.add(new ReminderRequest(25, NotificationType.PUSH));
        updatedReminders.add(new ReminderRequest(45, NotificationType.EMAIL));
        updateRequest.setReminders(updatedReminders);

        // Update the task
        taskService.updateTask(taskUid, updateRequest);

        // Verify post-update state
        List<Reminder> remindersAfterUpdate = reminderRepository.findByTask_UidOrderByReminderTimeAsc(taskUid);
        assertEquals(3, remindersAfterUpdate.size(), "Should have exactly 3 reminders after update");

        // Verify that reminder IDs are different (old ones were deleted, new ones created)
        List<Long> newReminderIds = remindersAfterUpdate.stream()
                .map(Reminder::getId)
                .sorted()
                .toList();

        // All IDs should be different (no overlap between old and new)
        boolean hasOverlap = originalReminderIds.stream().anyMatch(newReminderIds::contains);
        assertFalse(hasOverlap, "No original reminder IDs should exist after update (they should be deleted)");

        // Verify the correct reminder values exist
        long count10Email = remindersAfterUpdate.stream()
                .filter(r -> r.getReminderOffsetMinutes() == 10 && r.getNotificationType() == NotificationType.EMAIL)
                .count();
        long count25Push = remindersAfterUpdate.stream()
                .filter(r -> r.getReminderOffsetMinutes() == 25 && r.getNotificationType() == NotificationType.PUSH)
                .count();
        long count45Email = remindersAfterUpdate.stream()
                .filter(r -> r.getReminderOffsetMinutes() == 45 && r.getNotificationType() == NotificationType.EMAIL)
                .count();

        assertEquals(1, count10Email, "Should have exactly 1 reminder: 10 min EMAIL");
        assertEquals(1, count25Push, "Should have exactly 1 reminder: 25 min PUSH");
        assertEquals(1, count45Email, "Should have exactly 1 reminder: 45 min EMAIL");

        // Verify database consistency - total count for task
        long totalCount = reminderRepository.countByTask_Uid(taskUid);
        assertEquals(3, totalCount, "Database should show exactly 3 reminders for this task");
    }

    @Test
    void testEmptyReminderListDeletesAll() {
        // Create task with reminders
        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(20, NotificationType.PUSH));
        initialReminders.add(new ReminderRequest(60, NotificationType.EMAIL));
        taskRequest.setReminders(initialReminders);

        TaskResponse createdTask = taskService.createTask(taskRequest);
        String taskUid = createdTask.getId();

        // Verify initial state
        assertEquals(2, reminderRepository.countByTask_Uid(taskUid));

        // Update with empty reminders
        TaskRequest updateRequest = createTaskRequest();
        updateRequest.setReminders(new ArrayList<>());

        taskService.updateTask(taskUid, updateRequest);

        // Verify all reminders are deleted
        assertEquals(0, reminderRepository.countByTask_Uid(taskUid),
                    "All reminders should be deleted when updating with empty list");
    }

    @Test
    void testStressTestMultipleUpdates() {
        // Create task
        TaskRequest taskRequest = createTaskRequest();
        taskRequest.setReminders(List.of(new ReminderRequest(30, NotificationType.PUSH)));

        TaskResponse createdTask = taskService.createTask(taskRequest);
        String taskUid = createdTask.getId();

        // Perform many updates to stress test the delete/insert logic
        for (int i = 1; i <= 10; i++) {
            TaskRequest updateRequest = createTaskRequest();
            updateRequest.setTitle("Update " + i);

            List<ReminderRequest> reminders = new ArrayList<>();
            // Vary the number of reminders in each update
            int numReminders = (i % 3) + 1; // 1, 2, or 3 reminders

            for (int j = 0; j < numReminders; j++) {
                reminders.add(new ReminderRequest(
                    15 + (i * 10) + (j * 5),  // Unique offset
                    j % 2 == 0 ? NotificationType.EMAIL : NotificationType.PUSH
                ));
            }

            updateRequest.setReminders(reminders);
            taskService.updateTask(taskUid, updateRequest);

            // Verify correct count after each update
            long actualCount = reminderRepository.countByTask_Uid(taskUid);
            assertEquals(numReminders, actualCount,
                        "After update " + i + ", expected " + numReminders + " reminders, got " + actualCount);
        }
    }

    private TaskRequest createTaskRequest() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Flush Test Task");
        request.setDescription("Testing flush logic");
        request.setStartDatetimeLocal(LocalDateTime.now().plusHours(2));
        request.setEndDatetimeLocal(LocalDateTime.now().plusHours(3));
        request.setTimezone(ZoneId.systemDefault().getId());
        request.setColor("#FF5722");
        request.setLocation("Test Location");
        return request;
    }
}