package com.privatecal;

import com.privatecal.dto.*;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.entity.Reminder;
import com.privatecal.repository.TaskRepository;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.UserDetailsImpl;
import com.privatecal.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to reproduce and verify the reminder duplication bug during task
 * updates
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskReminderUpdateTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setUsername("testuser@example.com");
        testUser.setEmail("testuser@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // // Set up security context
        // UsernamePasswordAuthenticationToken auth =
        // new UsernamePasswordAuthenticationToken(
        // testUser.getUsername(),
        // null,
        // List.of(new SimpleGrantedAuthority("ROLE_USER"))
        // );
        // SecurityContextHolder.getContext().setAuthentication(auth);
        // Set up security context with UserDetailsImpl
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testReminderDuplicationOnUpdate() {
        // Create initial task with 2 reminders
        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(15, NotificationType.PUSH));
        initialReminders.add(new ReminderRequest(30, NotificationType.EMAIL));
        taskRequest.setReminders(initialReminders);

        // Create the task
        TaskResponse createdTask = taskService.createTask(taskRequest);
        Long taskId = createdTask.getId();

        // Verify initial state: task should have 2 reminders
        List<Reminder> remindersAfterCreate = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(2, remindersAfterCreate.size(), "Task should have exactly 2 reminders after creation");

        // Verify the reminder details
        if (remindersAfterCreate.get(0).getNotificationType() == NotificationType.PUSH) {
            assertEquals(15, remindersAfterCreate.get(0).getReminderOffsetMinutes());
            assertEquals(30, remindersAfterCreate.get(1).getReminderOffsetMinutes());
            assertEquals(NotificationType.EMAIL, remindersAfterCreate.get(1).getNotificationType());
        } else {
            assertEquals(15, remindersAfterCreate.get(1).getReminderOffsetMinutes());
            assertEquals(NotificationType.PUSH, remindersAfterCreate.get(1).getNotificationType());
            assertEquals(30, remindersAfterCreate.get(0).getReminderOffsetMinutes());
            assertEquals(NotificationType.EMAIL, remindersAfterCreate.get(0).getNotificationType());
        }

        // Update task with modified reminders (add one, modify one, remove one)
        TaskRequest updateRequest = createTaskRequest();
        updateRequest.setTitle("Updated Task Title");
        List<ReminderRequest> updatedReminders = new ArrayList<>();
        updatedReminders.add(new ReminderRequest(10, NotificationType.EMAIL)); // New reminder
        updatedReminders.add(new ReminderRequest(25, NotificationType.PUSH)); // Modified reminder (was 30 EMAIL)
        updateRequest.setReminders(updatedReminders);

        // Update the task
        TaskResponse updatedTask = taskService.updateTask(taskId, updateRequest);

        // Verify post-update state: task should have exactly 2 reminders (no
        // duplicates)
        List<Reminder> remindersAfterUpdate = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(2, remindersAfterUpdate.size(),
                "Task should have exactly 2 reminders after update, found: " + remindersAfterUpdate.size());

        // // Verify the updated reminder details
        if (remindersAfterUpdate.get(0).getNotificationType() == NotificationType.EMAIL) {
            assertEquals(10, remindersAfterUpdate.get(0).getReminderOffsetMinutes());
            assertEquals(NotificationType.EMAIL, remindersAfterUpdate.get(0).getNotificationType());
            assertEquals(25, remindersAfterUpdate.get(1).getReminderOffsetMinutes());
            assertEquals(NotificationType.PUSH, remindersAfterUpdate.get(1).getNotificationType());
        } else {
            assertEquals(10, remindersAfterUpdate.get(1).getReminderOffsetMinutes());
            assertEquals(NotificationType.EMAIL, remindersAfterUpdate.get(1).getNotificationType());
            assertEquals(25, remindersAfterUpdate.get(0).getReminderOffsetMinutes());
            assertEquals(NotificationType.PUSH, remindersAfterUpdate.get(0).getNotificationType());
        }

        // Additional verification: count total reminders for this task in database
        long totalRemindersCount = reminderRepository.countByTask_Id(taskId);
        assertEquals(2, totalRemindersCount, "Database should contain exactly 2 reminders for this task");
    }

    @Test
    void testMultipleUpdatesNoDuplication() {
        // Create initial task with 1 reminder
        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(20, NotificationType.PUSH));
        taskRequest.setReminders(initialReminders);

        TaskResponse createdTask = taskService.createTask(taskRequest);
        Long taskId = createdTask.getId();

        // Perform multiple updates
        for (int i = 1; i <= 3; i++) {
            TaskRequest updateRequest = createTaskRequest();
            updateRequest.setTitle("Updated Task " + i);
            List<ReminderRequest> reminders = new ArrayList<>();
            reminders.add(new ReminderRequest(20 + i * 5, NotificationType.EMAIL));
            updateRequest.setReminders(reminders);

            taskService.updateTask(taskId, updateRequest);

            // Verify no duplication after each update
            List<Reminder> reminders_list = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
            assertEquals(1, reminders_list.size(),
                    "Task should have exactly 1 reminder after update " + i + ", found: " + reminders_list.size());
        }
    }

    @Test
    void testRemoveAllReminders() {
        // Create task with 2 reminders
        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(15, NotificationType.PUSH));
        initialReminders.add(new ReminderRequest(30, NotificationType.EMAIL));
        taskRequest.setReminders(initialReminders);

        TaskResponse createdTask = taskService.createTask(taskRequest);
        Long taskId = createdTask.getId();

        List<Reminder> reminders = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(2, reminders.size(), "Task should have exactly 2 reminders");

        // Update task with empty reminders list
        TaskRequest updateRequest = createTaskRequest();
        updateRequest.setReminders(new ArrayList<>()); // Empty list

        taskService.updateTask(taskId, updateRequest);

        // Verify all reminders are removed
        List<Reminder> remindersAfterUpdate = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(0, remindersAfterUpdate.size(), "Task should have no reminders after clearing them");
    }

    @Test
    void testAddRemindersToTaskWithoutReminders() {
        // Create task without reminders
        TaskRequest taskRequest = createTaskRequest();
        taskRequest.setReminders(new ArrayList<>()); // No reminders initially

        TaskResponse createdTask = taskService.createTask(taskRequest);
        Long taskId = createdTask.getId();

        // Verify initial state: no reminders
        List<Reminder> initialReminders = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(0, initialReminders.size(), "Task should have no reminders initially");

        // Update task to add reminders
        TaskRequest updateRequest = createTaskRequest();
        List<ReminderRequest> newReminders = new ArrayList<>();
        newReminders.add(new ReminderRequest(10, NotificationType.PUSH));
        newReminders.add(new ReminderRequest(60, NotificationType.EMAIL));
        updateRequest.setReminders(newReminders);

        taskService.updateTask(taskId, updateRequest);

        // Verify reminders are added correctly
        List<Reminder> remindersAfterUpdate = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(2, remindersAfterUpdate.size(), "Task should have exactly 2 reminders after adding them");
    }

    private TaskRequest createTaskRequest() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Test Task");
        request.setDescription("Test Description");
        request.setStartDatetime(Instant.now().plus(1, ChronoUnit.HOURS));
        request.setEndDatetime(Instant.now().plus(2, ChronoUnit.HOURS));
        request.setColor("#3788d8");
        request.setLocation("Test Location");
        return request;
    }
}