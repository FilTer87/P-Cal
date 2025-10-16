package com.privatecal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.dto.NotificationType;
import com.privatecal.dto.ReminderRequest;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.security.UserDetailsImpl;
import com.privatecal.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ReminderController endpoints
 * Tests reminder CRUD operations, presets, and date range queries
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class ReminderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setUsername("remindertest@example.com");
        testUser.setEmail("remindertest@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("TestPassword123"));
        testUser.setFirstName("Reminder");
        testUser.setLastName("Test");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create a test task
        testTask = new Task();
        testTask.setTitle("Test Task for Reminders");
        testTask.setDescription("A task to test reminder functionality");
        testTask.setStartDatetime(Instant.parse("2024-12-25T10:00:00Z"));
        testTask.setEndDatetime(Instant.parse("2024-12-25T11:00:00Z"));
        testTask.setUser(testUser);
        testTask.setCreatedAt(Instant.now());
        testTask = taskRepository.save(testTask);

        // Set up security context
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        reminderRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllReminders_ShouldReturnUserReminders() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createReminderForTask_WithValidData_ShouldCreateReminder() throws Exception {
        ReminderRequest reminderRequest = new ReminderRequest(15, NotificationType.PUSH);

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderOffsetMinutes").value(15))
                .andExpect(jsonPath("$.notificationType").value("PUSH"))
                .andExpect(jsonPath("$.reminderTime").exists());
    }

    @Test
    void createReminderForTask_WithEmailType_ShouldCreateEmailReminder() throws Exception {
        ReminderRequest reminderRequest = new ReminderRequest(30, NotificationType.EMAIL);

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderOffsetMinutes").value(30))
                .andExpect(jsonPath("$.notificationType").value("EMAIL"));
    }

    @Test
    void createReminderForTask_WithInvalidTaskId_ShouldReturnBadRequest() throws Exception {
        ReminderRequest reminderRequest = new ReminderRequest(15, NotificationType.PUSH);

        mockMvc.perform(post("/api/reminders/task/{taskId}", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReminderForTask_WithNegativeOffset_ShouldReturnBadRequest() throws Exception {
        ReminderRequest reminderRequest = new ReminderRequest(-10, NotificationType.PUSH);

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRemindersForTask_ShouldReturnTaskReminders() throws Exception {
        // First create a reminder
        ReminderRequest reminderRequest = new ReminderRequest(10, NotificationType.PUSH);
        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andExpect(status().isCreated());

        // Then get reminders for the task
        mockMvc.perform(get("/api/reminders/task/{taskId}", testTask.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].reminderOffsetMinutes").value(10));
    }

    @Test
    void getRemindersForTask_WithInvalidTaskId_ShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/reminders/task/{taskId}", 999999L))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getUpcomingReminders_ShouldReturnUpcomingReminders() throws Exception {
        mockMvc.perform(get("/api/reminders/upcoming"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRemindersInRange_WithValidDates_ShouldReturnReminders() throws Exception {
        String startDate = "2024-12-24T00:00:00Z";
        String endDate = "2024-12-26T23:59:59Z";

        mockMvc.perform(get("/api/reminders/range")
                .param("startDate", startDate)
                .param("endDate", endDate))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRemindersInRange_WithInvalidDates_ShouldReturnError() throws Exception {
        String invalidDate = "invalid-date";

        mockMvc.perform(get("/api/reminders/range")
                .param("startDate", invalidDate)
                .param("endDate", "2024-12-26T23:59:59Z"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateReminder_WithValidData_ShouldUpdateReminder() throws Exception {
        // First create a reminder
        ReminderRequest createRequest = new ReminderRequest(15, NotificationType.PUSH);
        String createResponse = mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reminderId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then update the reminder
        ReminderRequest updateRequest = new ReminderRequest(30, NotificationType.EMAIL);
        mockMvc.perform(put("/api/reminders/{reminderId}", reminderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderOffsetMinutes").value(30))
                .andExpect(jsonPath("$.notificationType").value("EMAIL"));
    }

    @Test
    void updateReminder_WithInvalidReminderId_ShouldReturnBadRequest() throws Exception {
        ReminderRequest updateRequest = new ReminderRequest(30, NotificationType.EMAIL);

        mockMvc.perform(put("/api/reminders/{reminderId}", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReminder_WithValidReminderId_ShouldDeleteReminder() throws Exception {
        // First create a reminder
        ReminderRequest createRequest = new ReminderRequest(15, NotificationType.PUSH);
        String createResponse = mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reminderId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then delete the reminder
        mockMvc.perform(delete("/api/reminders/{reminderId}", reminderId)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reminder deleted successfully"));
    }

    @Test
    void deleteReminder_WithInvalidReminderId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/reminders/{reminderId}", 999999L)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteRemindersForTask_ShouldDeleteAllTaskReminders() throws Exception {
        // First create multiple reminders
        ReminderRequest reminder1 = new ReminderRequest(15, NotificationType.PUSH);
        ReminderRequest reminder2 = new ReminderRequest(30, NotificationType.EMAIL);

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminder1))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminder2))
                .with(csrf()))
                .andExpect(status().isCreated());

        // Then delete all reminders for the task
        mockMvc.perform(delete("/api/reminders/task/{taskId}", testTask.getId())
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All reminders deleted for task"));

        // Verify reminders are deleted
        mockMvc.perform(get("/api/reminders/task/{taskId}", testTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteRemindersForTask_WithInvalidTaskId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/reminders/task/{taskId}", 999999L)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReminderPresets_WithValidPresets_ShouldCreateMultipleReminders() throws Exception {
        Map<String, Object> presetsRequest = Map.of(
            "presets", List.of("5min", "15min", "1hour")
        );

        mockMvc.perform(post("/api/reminders/presets/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presetsRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void createReminderPresets_WithEmptyPresets_ShouldReturnBadRequest() throws Exception {
        Map<String, Object> presetsRequest = Map.of("presets", List.of());

        mockMvc.perform(post("/api/reminders/presets/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presetsRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReminderPresets_WithInvalidPresets_ShouldFilterInvalidOnes() throws Exception {
        Map<String, Object> presetsRequest = Map.of(
            "presets", List.of("5min", "invalid_preset", "1hour")
        );

        mockMvc.perform(post("/api/reminders/presets/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presetsRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2))); // Only valid presets should be created
    }

    @Test
    void createReminderPresets_WithoutPresets_ShouldReturnBadRequest() throws Exception {
        Map<String, Object> presetsRequest = Map.of();

        mockMvc.perform(post("/api/reminders/presets/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presetsRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReminderPresets_ShouldReturnAvailablePresets() throws Exception {
        mockMvc.perform(get("/api/reminders/presets"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.common").isArray())
                .andExpect(jsonPath("$.quick").isArray())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.common", hasSize(8))) // Check all 8 common presets are there
                .andExpect(jsonPath("$.quick", hasSize(4))); // Check all 4 quick presets are there
    }

    @Test
    void getReminderPresets_ShouldContainExpectedPresets() throws Exception {
        mockMvc.perform(get("/api/reminders/presets"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.common[0].name").value("5 minutes before"))
                .andExpect(jsonPath("$.common[0].value").value("5min"))
                .andExpect(jsonPath("$.common[0].minutes").value(5))
                .andExpect(jsonPath("$.quick[0]").value("5min"))
                .andExpect(jsonPath("$.quick[1]").value("15min"));
    }

    @Test
    void reminderEndpoints_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Test that reminder endpoints require authentication
        mockMvc.perform(get("/api/reminders")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/reminders/task/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reminderOffsetMinutes\":15}")
                .with(csrf())
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reminders/upcoming")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReminderForTask_WithZeroOffset_ShouldCreateReminder() throws Exception {
        ReminderRequest reminderRequest = new ReminderRequest(0, NotificationType.PUSH);

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderOffsetMinutes").value(0))
                .andExpect(jsonPath("$.notificationType").value("PUSH"));
    }

    @Test
    void createReminderForTask_WithLargeOffset_ShouldCreateReminder() throws Exception {
        ReminderRequest reminderRequest = new ReminderRequest(10080, NotificationType.EMAIL); // 1 week

        mockMvc.perform(post("/api/reminders/task/{taskId}", testTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderOffsetMinutes").value(10080))
                .andExpect(jsonPath("$.notificationType").value("EMAIL"));
    }

    @Test
    void createReminderForRecurringTask_ShouldUseNextOccurrence() throws Exception {
        // Create a recurring task that started in the past (7 days ago + 1 hour)
        // Daily recurrence, so next occurrence should be today
        Instant now = Instant.now();
        Instant pastStart = now.minus(java.time.Duration.ofDays(7)).plus(Duration.ofHours(1));
        Instant pastEnd = pastStart.plus(java.time.Duration.ofHours(1));

        System.out.println("📅 TEST: Creating recurring task:");
        System.out.println("  now = " + now);
        System.out.println("  pastStart = " + pastStart);
        System.out.println("  pastEnd = " + pastEnd);

        Task recurringTask = new Task();
        recurringTask.setTitle("Past Recurring Task");
        recurringTask.setDescription("Daily task that started a week ago");
        recurringTask.setStartDatetime(pastStart);
        recurringTask.setEndDatetime(pastEnd);
        recurringTask.setRecurrenceRule("FREQ=DAILY");
        recurringTask.setUser(testUser);
        recurringTask.setCreatedAt(Instant.now());

        System.out.println("📅 TEST: Before save:");
        System.out.println("  task.getStartDatetime() = " + recurringTask.getStartDatetime());

        recurringTask = taskRepository.save(recurringTask);

        System.out.println("📅 TEST: After save:");
        System.out.println("  task.getStartDatetime() = " + recurringTask.getStartDatetime());

        // Create a reminder: 30 minutes before
        ReminderRequest reminderRequest = new ReminderRequest(5, NotificationType.PUSH);

        String response = mockMvc.perform(post("/api/reminders/task/{taskId}", recurringTask.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reminderRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderOffsetMinutes").value(5))
                .andExpect(jsonPath("$.notificationType").value("PUSH"))
                .andExpect(jsonPath("$.reminderTime").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse the response to check reminder time
        Map<String, Object> reminderResponse = objectMapper.readValue(response, Map.class);
        String reminderTimeStr = (String) reminderResponse.get("reminderTime");
        Instant reminderTime = Instant.parse(reminderTimeStr);

        // Instant now = Instant.now();

        // Reminder should be in the future (or very close to now for today's occurrence)
        // Allow 35 minutes in the past to account for today's occurrence that might be happening soon
        assert reminderTime.isAfter(now.minus(java.time.Duration.ofMinutes(35))) :
            "Reminder should be based on next occurrence, not past start date. ReminderTime: " + reminderTime + ", Now: " + now;

        // Reminder should not be more than 24 hours in the future (for daily recurrence)
        assert reminderTime.isBefore(now.plus(java.time.Duration.ofHours(6))) :
            "Reminder should be within next 24 hours for daily task. ReminderTime: " + reminderTime + ", Now: " + now;

        System.out.println("✓ Recurring task reminder correctly calculated for next occurrence");
        System.out.println("  Task start (7 days ago): " + pastStart);
        System.out.println("  Reminder time: " + reminderTime);
        System.out.println("  Now: " + now);
    }
}