package com.privatecal;

import com.privatecal.dto.*;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.User;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using real PostgreSQL to verify reminder duplication behavior
 * This test uses Testcontainers to spin up a real PostgreSQL database
 *
 * Note: These tests require Docker to be available. They will be skipped if Docker is not found.
 * Set DOCKER_AVAILABLE=true environment variable to enable these tests.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
@Testcontainers
@Transactional
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
public class TaskReminderPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("calendar_test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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
        testUser.setTimezone("UTC");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Set up security context
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testReminderDuplicationWithPostgreSQL() {
        // This test specifically reproduces the bug seen in PostgreSQL but not in H2

        // Create initial task with 2 reminders
        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(15, NotificationType.PUSH));
        initialReminders.add(new ReminderRequest(30, NotificationType.EMAIL));
        taskRequest.setReminders(initialReminders);

        // Create the task
        TaskResponse createdTask = taskService.createTask(taskRequest);
        Long taskId = createdTask.getId();

        // Verify initial state
        List<Reminder> remindersAfterCreate = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
        assertEquals(2, remindersAfterCreate.size(), "Task should have exactly 2 reminders after creation");

        // Log reminder IDs before update
        System.out.println("Before update - Reminder IDs: " +
                          remindersAfterCreate.stream().map(r -> r.getId()).toList());

        // Update task with different reminders
        TaskRequest updateRequest = createTaskRequest();
        updateRequest.setTitle("Updated Task Title");
        List<ReminderRequest> updatedReminders = new ArrayList<>();
        updatedReminders.add(new ReminderRequest(10, NotificationType.EMAIL));
        updatedReminders.add(new ReminderRequest(25, NotificationType.PUSH));
        updateRequest.setReminders(updatedReminders);

        // Update the task
        TaskResponse updatedTask = taskService.updateTask(taskId, updateRequest);

        // Verify post-update state
        List<Reminder> remindersAfterUpdate = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);

        // Log reminder IDs after update
        System.out.println("After update - Reminder IDs: " +
                          remindersAfterUpdate.stream().map(r -> r.getId()).toList());
        System.out.println("After update - Reminder details: " +
                          remindersAfterUpdate.stream().map(r ->
                              r.getReminderOffsetMinutes() + ":" + r.getNotificationType()).toList());

        // This assertion should pass with the flush() fix
        assertEquals(2, remindersAfterUpdate.size(),
                    "Task should have exactly 2 reminders after update, found: " + remindersAfterUpdate.size());

        // Verify correct reminder values (order doesn't matter)
        long count10MinEmail = remindersAfterUpdate.stream()
                .filter(r -> r.getReminderOffsetMinutes() == 10 && r.getNotificationType() == NotificationType.EMAIL)
                .count();
        long count25MinPush = remindersAfterUpdate.stream()
                .filter(r -> r.getReminderOffsetMinutes() == 25 && r.getNotificationType() == NotificationType.PUSH)
                .count();

        assertEquals(1, count10MinEmail, "Should have exactly 1 reminder with 10 minutes EMAIL");
        assertEquals(1, count25MinPush, "Should have exactly 1 reminder with 25 minutes PUSH");
    }

    @Test
    void testMultipleUpdatesWithPostgreSQL() {
        // This test verifies that multiple updates don't cause accumulation of reminders

        TaskRequest taskRequest = createTaskRequest();
        List<ReminderRequest> initialReminders = new ArrayList<>();
        initialReminders.add(new ReminderRequest(20, NotificationType.PUSH));
        taskRequest.setReminders(initialReminders);

        TaskResponse createdTask = taskService.createTask(taskRequest);
        Long taskId = createdTask.getId();

        // Perform 5 updates
        for (int i = 1; i <= 5; i++) {
            TaskRequest updateRequest = createTaskRequest();
            updateRequest.setTitle("Updated Task " + i);
            List<ReminderRequest> reminders = new ArrayList<>();
            reminders.add(new ReminderRequest(20 + i * 5, NotificationType.EMAIL));
            updateRequest.setReminders(reminders);

            taskService.updateTask(taskId, updateRequest);

            // Check after each update
            List<Reminder> remindersList = reminderRepository.findByTask_IdOrderByReminderTimeAsc(taskId);
            System.out.println("After update " + i + " - Reminder count: " + remindersList.size() +
                              ", IDs: " + remindersList.stream().map(r -> r.getId()).toList());

            assertEquals(1, remindersList.size(),
                        "Task should have exactly 1 reminder after update " + i + ", found: " + remindersList.size());
        }
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