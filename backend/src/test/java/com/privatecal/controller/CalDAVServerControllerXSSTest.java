package com.privatecal.controller;

import com.privatecal.entity.Calendar;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.TaskRepository;
import com.privatecal.service.CalDAVService;
import com.privatecal.service.CalendarService;
import com.privatecal.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Security test for CalDAVServerController to verify XSS/XML injection protection
 */
@ExtendWith(MockitoExtension.class)
class CalDAVServerControllerXSSTest {

    @Mock
    private CalDAVService calDAVService;

    @Mock
    private CalendarService calendarService;

    @Mock
    private UserService userService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CalDAVServerController controller;

    private User testUser;
    private Calendar testCalendar;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(LocalDateTime.now());

        testCalendar = new Calendar();
        testCalendar.setId(1L);
        testCalendar.setUser(testUser);
        testCalendar.setSlug("test-calendar");
        testCalendar.setName("Normal Calendar Name");

        request = new MockHttpServletRequest();
        request.setMethod("PROPFIND");
    }

    @Test
    void testXSSProtection_CalendarNameWithScriptTag() throws Exception {
        // Given - Calendar with malicious name containing script tag
        testCalendar.setName("<script>alert('XSS')</script>");

        Task task1 = createTask("task-uid-1", "Task 1");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(calendarService.getCalendarBySlugAndUsername(anyString(), anyString())).thenReturn(testCalendar);
        when(taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(anyLong())).thenReturn(Arrays.asList(task1));
        when(calDAVService.getTaskETag(anyString())).thenReturn("etag-123");

        // When
        ResponseEntity<String> response = controller.propfind(request, "testuser", "test-calendar", null, "1");

        // Then
        assertNotNull(response);
        assertEquals(207, response.getStatusCode().value());

        String body = response.getBody();
        assertNotNull(body);

        // Verify XSS is prevented - script tag should be escaped
        assertFalse(body.contains("<script>"), "XML should not contain unescaped script tag");
        assertTrue(body.contains("&lt;script&gt;"), "XML should contain escaped script tag");
        assertTrue(body.contains("&lt;/script&gt;"), "XML should contain escaped closing script tag");
    }

    @Test
    void testXMLInjectionProtection_TaskUidWithXMLBreakout() throws Exception {
        // Given - Task with UID trying to break out of XML structure
        Task maliciousTask = createTask("\"><injected/><foo bar=\"", "Task 1");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(calendarService.getCalendarBySlugAndUsername(anyString(), anyString())).thenReturn(testCalendar);
        when(taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(anyLong())).thenReturn(Arrays.asList(maliciousTask));
        when(calDAVService.getTaskETag(anyString())).thenReturn("etag-123");

        // When
        ResponseEntity<String> response = controller.propfind(request, "testuser", "test-calendar", null, "1");

        // Then
        assertNotNull(response);
        assertEquals(207, response.getStatusCode().value());

        String body = response.getBody();
        assertNotNull(body);

        // Verify XML injection is prevented
        assertFalse(body.contains("\"><injected/>"), "XML should not contain unescaped injection");
        assertFalse(body.contains("<injected/>"), "XML should not contain injected tag");
        assertTrue(body.contains("&quot;&gt;&lt;injected/&gt;"), "XML should contain escaped characters");
    }

    @Test
    void testXSSProtection_AllXMLSpecialCharacters() throws Exception {
        // Given - Calendar name with all XML special characters
        testCalendar.setName("Test & <tag> \"quoted\" 'apostrophe'");

        Task task1 = createTask("normal-uid", "Task 1");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(calendarService.getCalendarBySlugAndUsername(anyString(), anyString())).thenReturn(testCalendar);
        when(taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(anyLong())).thenReturn(Arrays.asList(task1));
        when(calDAVService.getTaskETag(anyString())).thenReturn("etag-123");

        // When
        ResponseEntity<String> response = controller.propfind(request, "testuser", "test-calendar", null, "1");

        // Then
        assertNotNull(response);
        String body = response.getBody();
        assertNotNull(body);

        // Verify all special characters are escaped
        assertTrue(body.contains("Test &amp; &lt;tag&gt; &quot;quoted&quot; &apos;apostrophe&apos;"),
                "All XML special characters should be escaped");

        // Verify original unescaped characters are not present in content
        assertFalse(body.contains("Test & <tag>"), "Original unescaped content should not be present");
    }

    @Test
    void testXSSProtection_UsernameAndSlugInHref() throws Exception {
        // Given - Username and slug with special characters (though these are typically validated at creation)
        // We test escaping by using the actual username but with malicious calendar name
        String maliciousUsername = "testuser";
        String maliciousSlug = "test<script>";

        Task task1 = createTask("task-uid-1", "Task 1");

        // Set calendar slug to contain malicious content
        testCalendar.setSlug("test<script>");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(calendarService.getCalendarBySlugAndUsername(anyString(), anyString())).thenReturn(testCalendar);
        when(taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(anyLong())).thenReturn(Arrays.asList(task1));
        when(calDAVService.getTaskETag(anyString())).thenReturn("etag-123");

        // When
        ResponseEntity<String> response = controller.propfind(request, maliciousUsername, maliciousSlug, null, "1");

        // Then
        assertNotNull(response);
        String body = response.getBody();
        assertNotNull(body);

        // Verify href contains escaped values
        assertTrue(body.contains("test&lt;script&gt;"), "Calendar slug should be escaped in href");
        assertFalse(body.contains("test<script>"), "Unescaped script tag should not be present");
    }

    @Test
    void testNoXSS_NormalContent() throws Exception {
        // Given - Normal content without special characters
        testCalendar.setName("My Work Calendar");

        Task task1 = createTask("abc123-task", "Meeting");
        Task task2 = createTask("def456-task", "Review");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(calendarService.getCalendarBySlugAndUsername(anyString(), anyString())).thenReturn(testCalendar);
        when(taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(anyLong()))
                .thenReturn(Arrays.asList(task1, task2));
        when(calDAVService.getTaskETag("abc123-task")).thenReturn("etag-abc");
        when(calDAVService.getTaskETag("def456-task")).thenReturn("etag-def");

        // When
        ResponseEntity<String> response = controller.propfind(request, "testuser", "test-calendar", null, "1");

        // Then
        assertNotNull(response);
        assertEquals(207, response.getStatusCode().value());

        String body = response.getBody();
        assertNotNull(body);

        // Verify normal content is present and unchanged
        assertTrue(body.contains("My Work Calendar"), "Normal content should be present");
        assertTrue(body.contains("abc123-task"), "Task UID should be present");
        assertTrue(body.contains("def456-task"), "Task UID should be present");

        // Verify valid XML structure
        assertTrue(body.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>"));
        assertTrue(body.contains("<D:multistatus"));
        assertTrue(body.contains("</D:multistatus>"));
    }

    @Test
    void testXSSProtection_ETagWithSpecialCharacters() throws Exception {
        // Given - ETag with special characters (shouldn't happen but testing escaping)
        Task task1 = createTask("task-uid-1", "Task 1");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(calendarService.getCalendarBySlugAndUsername(anyString(), anyString())).thenReturn(testCalendar);
        when(taskRepository.findByCalendar_IdOrderByStartDatetimeAsc(anyLong())).thenReturn(Arrays.asList(task1));
        when(calDAVService.getTaskETag(anyString())).thenReturn("etag-<malicious>");

        // When
        ResponseEntity<String> response = controller.propfind(request, "testuser", "test-calendar", null, "1");

        // Then
        assertNotNull(response);
        String body = response.getBody();
        assertNotNull(body);

        // Verify ETag is escaped
        assertTrue(body.contains("etag-&lt;malicious&gt;"), "ETag should be escaped");
        assertFalse(body.contains("etag-<malicious>"), "ETag should not contain unescaped content");
    }

    // Helper method
    private Task createTask(String uid, String title) {
        Task task = new Task();
        task.setUid(uid);
        task.setUser(testUser);
        task.setCalendar(testCalendar);
        task.setTitle(title);
        task.setStartDatetime(Instant.parse("2025-10-24T10:00:00Z"));
        task.setEndDatetime(Instant.parse("2025-10-24T11:00:00Z"));
        task.setColor("#3788d8");
        return task;
    }
}
