package com.privatecal.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.config.TestConfig;
import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Calendar;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.CalendarRepository;
import com.privatecal.repository.TaskRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.UserDetailsImpl;

/**
 * Integration tests for Multiple Calendars functionality
 * Tests:
 * - Task creation with specified calendarId
 * - Task creation without calendarId (fallback to default)
 * - Calendar ownership validation
 * - Import with calendar selection
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class MultipleCalendarsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;
    private Calendar defaultCalendar;
    private Calendar workCalendar;
    private Calendar otherUserCalendar;
    private UsernamePasswordAuthenticationToken authToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser@example.com");
        testUser.setEmail("testuser@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create default calendar for test user
        defaultCalendar = new Calendar();
        defaultCalendar.setUser(testUser);
        defaultCalendar.setName("Default Calendar");
        defaultCalendar.setSlug("default");
        defaultCalendar.setColor("#3788d8");
        defaultCalendar.setIsDefault(true);
        defaultCalendar.setIsVisible(true);
        defaultCalendar.setTimezone("Europe/Rome");
        defaultCalendar.setCreatedAt(Instant.now());
        defaultCalendar = calendarRepository.save(defaultCalendar);

        // Create work calendar for test user
        workCalendar = new Calendar();
        workCalendar.setUser(testUser);
        workCalendar.setName("Work");
        workCalendar.setSlug("work");
        workCalendar.setColor("#ff5722");
        workCalendar.setIsDefault(false);
        workCalendar.setIsVisible(true);
        workCalendar.setTimezone("Europe/Rome");
        workCalendar.setCreatedAt(Instant.now());
        workCalendar = calendarRepository.save(workCalendar);

        // Create other user and their calendar
        otherUser = new User();
        otherUser.setUsername("otheruser@example.com");
        otherUser.setEmail("otheruser@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode("password"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);

        otherUserCalendar = new Calendar();
        otherUserCalendar.setUser(otherUser);
        otherUserCalendar.setName("Other Calendar");
        otherUserCalendar.setSlug("other");
        otherUserCalendar.setColor("#4caf50");
        otherUserCalendar.setIsDefault(true);
        otherUserCalendar.setIsVisible(true);
        otherUserCalendar.setTimezone("Europe/Rome");
        otherUserCalendar.setCreatedAt(Instant.now());
        otherUserCalendar = calendarRepository.save(otherUserCalendar);

        // Setup auth token
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }

    @AfterEach
    void tearDown() {
        taskRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Task creation with specified calendarId should assign to that calendar")
    void testCreateTaskWithSpecifiedCalendar() throws Exception {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Work Task");
        taskRequest.setStartDatetimeLocal(LocalDateTime.now().plusDays(1));
        taskRequest.setEndDatetimeLocal(LocalDateTime.now().plusDays(1).plusHours(1));
        taskRequest.setTimezone("Europe/Rome");
        taskRequest.setCalendarId(workCalendar.getId());

        mockMvc.perform(post("/api/tasks")
                        .with(authentication(authToken))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calendarId").value(workCalendar.getId()))
                .andExpect(jsonPath("$.calendarName").value("Work"))
                .andExpect(jsonPath("$.calendarSlug").value("work"))
                .andExpect(jsonPath("$.calendarColor").value("#ff5722"));
    }

    @Test
    @DisplayName("Task creation without calendarId should fallback to default calendar")
    void testCreateTaskWithoutCalendarId() throws Exception {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Default Task");
        taskRequest.setStartDatetimeLocal(LocalDateTime.now().plusDays(1));
        taskRequest.setEndDatetimeLocal(LocalDateTime.now().plusDays(1).plusHours(1));
        taskRequest.setTimezone("Europe/Rome");
        // No calendarId specified

        mockMvc.perform(post("/api/tasks")
                        .with(authentication(authToken))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calendarId").value(defaultCalendar.getId()))
                .andExpect(jsonPath("$.calendarName").value("Default Calendar"))
                .andExpect(jsonPath("$.calendarSlug").value("default"));
    }

    @Test
    @DisplayName("Task creation with other user's calendar should fail with error")
    void testCreateTaskWithOtherUserCalendar() throws Exception {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Unauthorized Task");
        taskRequest.setStartDatetimeLocal(LocalDateTime.now().plusDays(1));
        taskRequest.setEndDatetimeLocal(LocalDateTime.now().plusDays(1).plusHours(1));
        taskRequest.setTimezone("Europe/Rome");
        taskRequest.setCalendarId(otherUserCalendar.getId()); // Other user's calendar

        mockMvc.perform(post("/api/tasks")
                        .with(authentication(authToken))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Task creation with non-existent calendar should fail")
    void testCreateTaskWithNonExistentCalendar() throws Exception {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Invalid Calendar Task");
        taskRequest.setStartDatetimeLocal(LocalDateTime.now().plusDays(1));
        taskRequest.setEndDatetimeLocal(LocalDateTime.now().plusDays(1).plusHours(1));
        taskRequest.setTimezone("Europe/Rome");
        taskRequest.setCalendarId(99999L); // Non-existent calendar

        mockMvc.perform(post("/api/tasks")
                        .with(authentication(authToken))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Task response should include calendar details")
    void testTaskResponseIncludesCalendarDetails() throws Exception {
        // Create a task directly in DB
        Task task = new Task();
        task.setUid("test-uid-" + System.currentTimeMillis());
        task.setTitle("Test Task");
        task.setUser(testUser);
        task.setCalendar(workCalendar);
        task.setStartDatetimeLocal(LocalDateTime.now().plusDays(1));
        task.setEndDatetimeLocal(LocalDateTime.now().plusDays(1).plusHours(1));
        task.setTaskTimezone("Europe/Rome");
        task = taskRepository.save(task);

        mockMvc.perform(get("/api/tasks/" + task.getUid())
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarId").value(workCalendar.getId()))
                .andExpect(jsonPath("$.calendarName").value("Work"))
                .andExpect(jsonPath("$.calendarSlug").value("work"))
                .andExpect(jsonPath("$.calendarColor").value("#ff5722"));
    }

    @Test
    @DisplayName("Get tasks should return calendar info for all tasks")
    void testGetTasksIncludesCalendarInfo() throws Exception {
        // Create tasks in different calendars
        Task task1 = new Task();
        task1.setUid("test-uid-1-" + System.currentTimeMillis());
        task1.setTitle("Default Task");
        task1.setUser(testUser);
        task1.setCalendar(defaultCalendar);
        task1.setStartDatetimeLocal(LocalDateTime.now().plusDays(1));
        task1.setEndDatetimeLocal(LocalDateTime.now().plusDays(1).plusHours(1));
        task1.setTaskTimezone("Europe/Rome");
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setUid("test-uid-2-" + System.currentTimeMillis());
        task2.setTitle("Work Task");
        task2.setUser(testUser);
        task2.setCalendar(workCalendar);
        task2.setStartDatetimeLocal(LocalDateTime.now().plusDays(2));
        task2.setEndDatetimeLocal(LocalDateTime.now().plusDays(2).plusHours(1));
        task2.setTaskTimezone("Europe/Rome");
        taskRepository.save(task2);

        mockMvc.perform(get("/api/tasks/date-range")
                        .param("startDate", LocalDateTime.now().toLocalDate().toString())
                        .param("endDate", LocalDateTime.now().plusDays(3).toLocalDate().toString())
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].calendarName", hasItems("Default Calendar", "Work")));
    }

    @Test
    @DisplayName("Import with calendar selection should assign all tasks to specified calendar")
    void testImportWithCalendarSelection() throws Exception {
        String icsContent = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Test//Test Calendar//EN
                BEGIN:VEVENT
                UID:test-import-1
                DTSTART:20251115T100000
                DTEND:20251115T110000
                SUMMARY:Imported Event 1
                END:VEVENT
                BEGIN:VEVENT
                UID:test-import-2
                DTSTART:20251116T140000
                DTEND:20251116T150000
                SUMMARY:Imported Event 2
                END:VEVENT
                END:VCALENDAR
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ics",
                "text/calendar",
                icsContent.getBytes()
        );

        mockMvc.perform(multipart("/api/calendar/import/confirm")
                        .file(file)
                        .param("duplicateStrategy", "SKIP")
                        .param("calendarId", workCalendar.getId().toString())
                        .with(authentication(authToken))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.successCount").value(2));

        // Verify tasks were assigned to work calendar
        Task importedTask1 = taskRepository.findById("test-import-1").orElseThrow();
        assert importedTask1.getCalendar().getId().equals(workCalendar.getId());

        Task importedTask2 = taskRepository.findById("test-import-2").orElseThrow();
        assert importedTask2.getCalendar().getId().equals(workCalendar.getId());
    }

    @Test
    @DisplayName("Import without calendar selection should fail")
    void testImportWithoutCalendarSelection() throws Exception {
        String icsContent = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Test//Test Calendar//EN
                BEGIN:VEVENT
                UID:test-import-3
                DTSTART:20251115T100000
                DTEND:20251115T110000
                SUMMARY:Imported Event
                END:VEVENT
                END:VCALENDAR
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ics",
                "text/calendar",
                icsContent.getBytes()
        );

        mockMvc.perform(multipart("/api/calendar/import/confirm")
                        .file(file)
                        .param("duplicateStrategy", "SKIP")
                        // No calendarId param
                        .with(authentication(authToken))
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }
}
