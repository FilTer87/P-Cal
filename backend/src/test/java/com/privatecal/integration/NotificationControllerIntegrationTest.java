package com.privatecal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for NotificationController endpoints
 * Tests notification configuration, NTFY settings, and test notifications
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user with NTFY topic
        testUser = new User();
        testUser.setUsername("notificationtest@example.com");
        testUser.setEmail("notificationtest@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("TestPassword123"));
        testUser.setFirstName("Notification");
        testUser.setLastName("Test");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setNtfyTopic("pcal_user_123_notifications");
        testUser = userRepository.save(testUser);

        // Set up security context
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getNotificationConfig_ShouldReturnConfiguration() throws Exception {
        mockMvc.perform(get("/api/notifications/config"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ntfyServerUrl").exists())
                .andExpect(jsonPath("$.ntfyTopicPrefix").exists())
                .andExpect(jsonPath("$.enabledProviders").exists())
                .andExpect(jsonPath("$.supportsPush").exists())
                .andExpect(jsonPath("$.supportsEmail").exists());
    }

    @Test
    void getNotificationConfig_WithoutAuthentication_ShouldRequireAuth() throws Exception {
        // Notification config requires authentication
        mockMvc.perform(get("/api/notifications/config")
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateNtfyTopic_WithValidTopic_ShouldUpdateSuccessfully() throws Exception {
        // Create a topic with the correct format: prefix-userId-suffix
        String validTopic = "p-cal-test-" + testUser.getId() + "-newtopic";
        Map<String, String> topicRequest = Map.of("topic", validTopic);

        mockMvc.perform(put("/api/notifications/ntfy/topic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topicRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("NTFY topic updated successfully"))
                .andExpect(jsonPath("$.topic").value(validTopic));
    }

    @Test
    void updateNtfyTopic_WithEmptyTopic_ShouldReturnBadRequest() throws Exception {
        Map<String, String> topicRequest = Map.of("topic", "");

        mockMvc.perform(put("/api/notifications/ntfy/topic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topicRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Topic cannot be empty"));
    }

    @Test
    void updateNtfyTopic_WithoutTopic_ShouldReturnBadRequest() throws Exception {
        Map<String, String> topicRequest = Map.of();

        mockMvc.perform(put("/api/notifications/ntfy/topic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topicRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Topic cannot be empty"));
    }

    @Test
    void sendTestNotification_WithDefaultSettings_ShouldSucceed() throws Exception {
        Map<String, Object> testRequest = Map.of(
            "type", "PUSH",
            "message", "Test notification from integration test"
        );

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendTestNotification_WithEmailType_ShouldSucceed() throws Exception {
        Map<String, Object> testRequest = Map.of(
            "type", "EMAIL",
            "message", "Test email notification from integration test"
        );

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendTestNotification_WithInvalidType_ShouldReturnBadRequest() throws Exception {
        Map<String, Object> testRequest = Map.of(
            "type", "INVALID_TYPE",
            "message", "Test notification"
        );

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid notification type. Use PUSH or EMAIL"));
    }

    @Test
    void sendTestNotification_WithDefaultMessage_ShouldUseDefaultMessage() throws Exception {
        Map<String, Object> testRequest = Map.of("type", "PUSH");

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendTestNotification_WithEmptyRequest_ShouldUseDefaults() throws Exception {
        Map<String, Object> testRequest = Map.of();

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getNtfySubscriptionUrl_WithConfiguredTopic_ShouldReturnUrl() throws Exception {
        mockMvc.perform(get("/api/notifications/ntfy/subscription-url"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subscriptionUrl").exists())
                .andExpect(jsonPath("$.topic").value("pcal_user_123_notifications"))
                .andExpect(jsonPath("$.serverUrl").exists());
    }

    @Test
    void getNtfySubscriptionUrl_WithoutTopic_ShouldReturnError() throws Exception {
        // Update user to have no NTFY topic
        testUser.setNtfyTopic(null);
        userRepository.save(testUser);

        mockMvc.perform(get("/api/notifications/ntfy/subscription-url"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(containsString("no NTFY topic configured")));
    }

    @Test
    void getNtfySubscriptionUrl_WithEmptyTopic_ShouldReturnError() throws Exception {
        // Update user to have empty NTFY topic
        testUser.setNtfyTopic("");
        userRepository.save(testUser);

        mockMvc.perform(get("/api/notifications/ntfy/subscription-url"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(containsString("no NTFY topic configured")));
    }

    @Test
    void generateNtfyQrCode_ShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(get("/api/notifications/ntfy/qr-code"))
                .andDo(print())
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(containsString("not yet implemented")));
    }

    @Test
    void notificationEndpoints_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Test that protected notification endpoints require authentication
        mockMvc.perform(put("/api/notifications/ntfy/topic")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"test\"}")
                .with(csrf())
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"PUSH\"}")
                .with(csrf())
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/ntfy/subscription-url")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateNtfyTopic_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        Map<String, String> topicRequest = Map.of("topic", "test_topic");

        mockMvc.perform(put("/api/notifications/ntfy/topic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topicRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendTestNotification_WithLongMessage_ShouldSucceed() throws Exception {
        Map<String, Object> testRequest = Map.of(
            "type", "PUSH",
            "message", "This is a very long test notification message that should still be handled correctly by the notification system. It contains multiple sentences and should test the message handling capabilities."
        );

        mockMvc.perform(post("/api/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}