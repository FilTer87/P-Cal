package com.privatecal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.dto.NotificationType;
import com.privatecal.dto.ReminderRequest;
import com.privatecal.dto.TaskRequest;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive integration tests for TaskController endpoints
 * Tests additional task operations not covered in existing TaskIntegrationTest
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class TaskControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setUsername("tasktest@example.com");
        testUser.setEmail("tasktest@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("TestPassword123"));
        testUser.setFirstName("Task");
        testUser.setLastName("Test");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

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
    void getTasksInDateRange_WithValidRange_ShouldReturnTasks() throws Exception {
        // Create tasks in different date ranges
        TaskRequest taskInRange = new TaskRequest(
            "Task in Range",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );

        TaskRequest taskOutOfRange = new TaskRequest(
            "Task out of Range",
            Instant.parse("2024-11-15T10:00:00Z"),
            Instant.parse("2024-11-15T11:00:00Z")
        );

        // Create both tasks
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskInRange))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskOutOfRange))
                .with(csrf()))
                .andExpect(status().isCreated());

        // Get tasks in date range (should only return the task in range)
        mockMvc.perform(get("/api/tasks/date-range")
                .param("startDate", "2024-12-24")
                .param("endDate", "2024-12-26"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Task in Range"));
    }

    @Test
    void getTasksInDateRange_WithInvalidDateFormat_ShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/tasks/date-range")
                .param("startDate", "invalid-date")
                .param("endDate", "2024-12-26"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getTodayTasks_ShouldReturnTasksForToday() throws Exception {
        // Create a task for today (using a date close to current time)
        Instant today = Instant.now();
        TaskRequest todayTask = new TaskRequest(
            "Today's Task",
            today,
            today.plusSeconds(3600) // 1 hour later
        );

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(todayTask))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/today"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchTasks_WithValidQuery_ShouldReturnMatchingTasks() throws Exception {
        // Create tasks with different titles and descriptions
        TaskRequest task1 = new TaskRequest(
            "Meeting with client",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );
        task1.setDescription("Important client meeting");

        TaskRequest task2 = new TaskRequest(
            "Code review",
            Instant.parse("2024-12-25T14:00:00Z"),
            Instant.parse("2024-12-25T15:00:00Z")
        );
        task2.setDescription("Review pull requests");

        // Create both tasks
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task1))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task2))
                .with(csrf()))
                .andExpect(status().isCreated());

        // Search for tasks containing "meeting"
        mockMvc.perform(get("/api/tasks/search")
                .param("q", "meeting"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title").value(containsStringIgnoringCase("meeting")));
    }

    @Test
    void searchTasks_WithEmptyQuery_ShouldReturnAllTasks() throws Exception {
        mockMvc.perform(get("/api/tasks/search")
                .param("q", ""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }




    @Test
    void cloneTask_WithValidTaskAndNewTime_ShouldCreateClone() throws Exception {
        // Create original task
        TaskRequest originalTask = new TaskRequest(
            "Original Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );
        originalTask.setDescription("Original description");
        originalTask.setColor("#ff0000");

        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalTask))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        // Clone the task with new start time
        Map<String, String> cloneRequest = Map.of(
            "newStartTime", "2024-12-26T10:00:00Z"
        );

        mockMvc.perform(post("/api/tasks/{taskId}/clone", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cloneRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(containsString("Original Task")))
                .andExpect(jsonPath("$.description").value("Original description"))
                .andExpect(jsonPath("$.color").value("#ff0000"))
                .andExpect(jsonPath("$.id").value(not(taskId))); // Should have different ID
    }

    @Test
    void cloneTask_WithEmptyNewTime_ShouldReturnBadRequest() throws Exception {
        // Create original task first
        TaskRequest originalTask = new TaskRequest(
            "Original Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );

        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalTask))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        // Try to clone with empty new start time
        Map<String, String> cloneRequest = Map.of("newStartTime", "");

        mockMvc.perform(post("/api/tasks/{taskId}/clone", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cloneRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void cloneTask_WithInvalidTaskId_ShouldReturnBadRequest() throws Exception {
        Map<String, String> cloneRequest = Map.of(
            "newStartTime", "2024-12-26T10:00:00Z"
        );

        mockMvc.perform(post("/api/tasks/{taskId}/clone", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cloneRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTaskStatistics_ShouldReturnStatistics() throws Exception {
        // Create a few tasks first
        TaskRequest task1 = new TaskRequest(
            "Statistics Task 1",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );

        TaskRequest task2 = new TaskRequest(
            "Statistics Task 2",
            Instant.parse("2024-12-25T14:00:00Z"),
            Instant.parse("2024-12-25T15:00:00Z")
        );

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task1))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task2))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").exists())
                .andExpect(jsonPath("$.todayTasks").exists())
                .andExpect(jsonPath("$.upcomingTasks").exists())
                .andExpect(jsonPath("$.overdueTasks").exists());
    }

    @Test
    void createTaskWithComplexData_ShouldHandleAllFields() throws Exception {
        TaskRequest complexTask = new TaskRequest(
            "Complex Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:30:00Z")
        );
        complexTask.setDescription("A very detailed description with special characters: àáâäãåāčćøěñß");
        complexTask.setColor("#3366cc");
        complexTask.setLocation("Conference Room A, Building 2, Floor 3");
        complexTask.setReminders(Arrays.asList(
            new ReminderRequest(15, NotificationType.PUSH),
            new ReminderRequest(60, NotificationType.EMAIL),
            new ReminderRequest(1440, NotificationType.PUSH) // 1 day
        ));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(complexTask))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Complex Task"))
                .andExpect(jsonPath("$.description").value(containsString("special characters")))
                .andExpect(jsonPath("$.color").value("#3366cc"))
                .andExpect(jsonPath("$.location").value(containsString("Conference Room")));
    }

    @Test
    void taskEndpoints_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Test additional task endpoints require authentication
        mockMvc.perform(get("/api/tasks/today")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/tasks/search")
                .param("q", "test")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/tasks/stats")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskIds\":[1]}")
                .with(csrf())
                .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTask_WithMinimalData_ShouldSucceed() throws Exception {
        TaskRequest minimalTask = new TaskRequest(
            "Minimal Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T10:30:00Z")
        );

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalTask))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Minimal Task"));
    }

    @Test
    void createTask_WithInvalidTimeRange_ShouldReturnBadRequest() throws Exception {
        // End time before start time
        TaskRequest invalidTask = new TaskRequest(
            "Invalid Task",
            Instant.parse("2024-12-25T11:00:00Z"),
            Instant.parse("2024-12-25T10:00:00Z") // End before start
        );

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTask))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}