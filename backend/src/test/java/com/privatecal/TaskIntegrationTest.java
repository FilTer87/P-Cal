package com.privatecal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.config.TestConfig;
import com.privatecal.dto.NotificationType;
import com.privatecal.dto.ReminderRequest;
import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.UserDetailsImpl;

/**
 * Integration tests for Task functionality
 * Tests the complete application stack including database
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        // Create a test user in the database
        testUser = new User();
        testUser.setUsername("testuser@example.com");
        testUser.setEmail("testuser@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create another user for access control tests
        otherUser = new User();
        otherUser.setUsername("otheruser@example.com");
        otherUser.setEmail("otheruser@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode("password"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);

        // Set up security context with UserDetailsImpl
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data since we removed @Transactional
        taskRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUserAuthentication_ShouldWork() throws Exception {
        // Test that authentication works by calling GET /api/tasks
        mockMvc.perform(get("/api/tasks"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void createAndRetrieveTask_ShouldWorkEndToEnd() throws Exception {
        // Given - use constructor to avoid extra serialized fields
        TaskRequest taskRequest = new TaskRequest(
            "Integration Test Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );
        taskRequest.setDescription("This is a test task created during integration testing");

        // When - Create task
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("This is a test task created during integration testing"))
                .andExpect(jsonPath("$.startDatetime").exists());

        // Then - Retrieve tasks and verify creation
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Integration Test Task"));
    }

    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        // Test that the application context loads and health endpoint is accessible
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiDocs_ShouldBeAccessible() throws Exception {
        // Test that OpenAPI documentation is accessible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void updateTask_ShouldUpdateExistingTask() throws Exception {
        // Given - Create a task first
        TaskRequest createRequest = new TaskRequest(
            "Original Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );
        createRequest.setDescription("Original description");

        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        // When - Update the task
        TaskRequest updateRequest = new TaskRequest(
            "Updated Task Title",
            Instant.parse("2024-12-25T14:00:00Z"),
            Instant.parse("2024-12-25T15:30:00Z")
        );
        updateRequest.setDescription("Updated description");
        updateRequest.setColor("#ff0000");

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Updated Task Title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.color").value("#ff0000"));

        // Then - Verify the task was updated in database
        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task Title"));
    }

    @Test
    void createTaskWithReminders_ShouldSaveAndRetrieveReminders() throws Exception {
        // Given - Task with reminders
        TaskRequest taskRequest = new TaskRequest(
            "Task with Reminders",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );
        taskRequest.setDescription("This task has multiple reminders");
        taskRequest.setReminders(Arrays.asList(
            new ReminderRequest(15, NotificationType.PUSH),
            new ReminderRequest(5, NotificationType.EMAIL)
        ));

        // When - Create task (reminders should be saved but are not returned in POST response)
        String response = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Task with Reminders"))
                .andExpect(jsonPath("$.reminders").isArray())
                // POST response doesn't include reminders (by design), but they are saved in DB
                .andExpect(jsonPath("$.reminders", hasSize(0)))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        // Then - Verify reminders were saved correctly by fetching the task
        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminders", hasSize(2)))
                .andExpect(jsonPath("$.reminders[0].reminderOffsetMinutes").value(15))
                .andExpect(jsonPath("$.reminders[0].notificationType").value("PUSH"))
                .andExpect(jsonPath("$.reminders[1].reminderOffsetMinutes").value(5))
                .andExpect(jsonPath("$.reminders[1].notificationType").value("EMAIL"));
    }

    @Test
    void deleteTask_ShouldRemoveTaskFromDatabase() throws Exception {
        // Given - Create a task first
        TaskRequest taskRequest = new TaskRequest(
            "Task to Delete",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );

        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        // When - Delete the task (accept both 200 OK and 204 No Content)
        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .with(csrf()))
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200 && status != 204) {
                        throw new AssertionError("Expected status 200 or 204 but was " + status);
                    }
                });

        // Then - Verify task is deleted
        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isNotFound());

        // And - Verify it's not in the task list
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + taskId + ")]").doesNotExist());
    }

    @Test
    void createTask_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Given - Task request without authentication
        TaskRequest taskRequest = new TaskRequest(
            "Unauthorized Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );

        // When - Try to create task without authentication by using anonymous user
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest))
                .with(csrf())
                .with(anonymous())) // Use Spring Security test support for anonymous user
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTask_WithDifferentUser_ShouldReturnForbiddenOrNotFound() throws Exception {
        // Given - Create task with testUser
        TaskRequest taskRequest = new TaskRequest(
            "Private Task",
            Instant.parse("2024-12-25T10:00:00Z"),
            Instant.parse("2024-12-25T11:00:00Z")
        );

        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        // When - Switch to otherUser by explicitly setting security context
        UserDetailsImpl otherUserDetails = UserDetailsImpl.build(otherUser);
        UsernamePasswordAuthenticationToken otherAuthentication =
            new UsernamePasswordAuthenticationToken(otherUserDetails, null, otherUserDetails.getAuthorities());

        // Perform request with different user authentication
        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                .with(authentication(otherAuthentication)))
                .andDo(print())
                .andExpect(status().isNotFound()); // User should not see tasks from other users

        // And - Should not see the task in their task list when authenticated as otherUser
        mockMvc.perform(get("/api/tasks")
                .with(authentication(otherAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + taskId + ")]").doesNotExist());

        // No need to restore authentication as we used .with(authentication()) per-request
    }
}