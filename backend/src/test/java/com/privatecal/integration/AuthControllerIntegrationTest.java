package com.privatecal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.dto.AuthRequest;
import com.privatecal.dto.ForgotPasswordRequest;
import com.privatecal.dto.ResetPasswordRequest;
import com.privatecal.dto.UserPreferencesRequest;
import com.privatecal.entity.User;
import com.privatecal.repository.CalendarRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.repository.PasswordResetTokenRepository;
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
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;

/**
 * Integration tests for AuthController endpoints
 * Tests authentication, user management, preferences, and password reset functionality
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up any existing data first (in case previous test failed)
        // Order matters: delete children first, then parents
        passwordResetTokenRepository.deleteAll();
        calendarRepository.deleteAll();  // Delete calendars before users (FK constraint)
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setUsername("authtest@example.com");
        testUser.setEmail("authtest@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("TestPassword123"));
        testUser.setFirstName("Auth");
        testUser.setLastName("Test");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setTheme("dark");
        testUser.setTimezone("Europe/Rome");
        testUser.setTimeFormat("24h");
        testUser.setCalendarView("week");
        testUser.setEmailNotifications(true);
        testUser.setReminderNotifications(true);
        testUser = userRepository.save(testUser);

        // Set up security context
        UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        // Order matters: delete children first, then parents
        passwordResetTokenRepository.deleteAll();
        calendarRepository.deleteAll();  // Delete calendars before users (FK constraint)
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_WithValidCredentials_ShouldReturnSuccess() throws Exception {
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("authtest@example.com");
        loginRequest.setPassword("TestPassword123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username").value("authtest@example.com"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("authtest@example.com");
        loginRequest.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_WithMissingFields_ShouldReturnBadRequest() throws Exception {
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("");
        loginRequest.setPassword("TestPassword123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is required"));
    }

    @Test
    void register_WithValidData_ShouldCreateUser() throws Exception {
        // Use a shorter unique email to avoid conflicts and fit username constraints (3-50 chars)
        // String uniqueEmail = "usr.test" + System.currentTimeMillis() + "@example.com";
        AuthRequest registerRequest = new AuthRequest();
        String username = "user_test";
        String email = "user.test@example.com";
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword("NewPassword123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername("invalid");
        registerRequest.setEmail("invalid-email");
        registerRequest.setPassword("Password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must be valid"));
    }

    @Test
    void getCurrentUser_WithAuthentication_ShouldReturnUserProfile() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("authtest@example.com"))
                .andExpect(jsonPath("$.email").value("authtest@example.com"))
                .andExpect(jsonPath("$.firstName").value("Auth"))
                .andExpect(jsonPath("$.lastName").value("Test"));
    }

    @Test
    void getCurrentUser_AlternativeEndpoint_ShouldReturnUserProfile() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("authtest@example.com"));
    }

    @Test
    void updateProfile_WithValidData_ShouldUpdateUser() throws Exception {
        Map<String, Object> updateRequest = Map.of(
            "firstName", "Updated",
            "lastName", "Name",
            "email", "authtest@example.com"
        );

        mockMvc.perform(put("/api/auth/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    void changePassword_WithValidData_ShouldSucceed() throws Exception {
        Map<String, String> passwordRequest = Map.of(
            "currentPassword", "TestPassword123",
            "newPassword", "NewTestPassword123"
        );

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldFail() throws Exception {
        Map<String, String> passwordRequest = Map.of(
            "currentPassword", "WrongPassword",
            "newPassword", "NewTestPassword123"
        );

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getUserPreferences_ShouldReturnCurrentPreferences() throws Exception {
        mockMvc.perform(get("/api/auth/preferences"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.timezone").value("Europe/Rome"))
                .andExpect(jsonPath("$.timeFormat").value("24h"))
                .andExpect(jsonPath("$.calendarView").value("week"))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.reminderNotifications").value(true));
    }

    @Test
    void updateUserPreferences_WithValidData_ShouldUpdatePreferences() throws Exception {
        UserPreferencesRequest preferencesRequest = new UserPreferencesRequest();
        preferencesRequest.setTheme("light");
        preferencesRequest.setTimezone("America/New_York");
        preferencesRequest.setTimeFormat("12h");
        preferencesRequest.setCalendarView("month");
        preferencesRequest.setEmailNotifications(false);
        preferencesRequest.setReminderNotifications(false);

        mockMvc.perform(put("/api/auth/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.timeFormat").value("12h"))
                .andExpect(jsonPath("$.calendarView").value("month"))
                .andExpect(jsonPath("$.emailNotifications").value(false))
                .andExpect(jsonPath("$.reminderNotifications").value(false));
    }

    @Test
    void logout_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void getNotificationSettings_ShouldReturnDeprecationMessage() throws Exception {
        mockMvc.perform(get("/api/auth/notification-settings"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("deprecated")))
                .andExpect(jsonPath("$.configEndpoint").value("/api/notifications/config"));
    }

    @Test
    void sendTestNotification_WithValidData_ShouldSucceed() throws Exception {
        Map<String, String> testRequest = Map.of(
            "message", "Test notification from integration test"
        );

        mockMvc.perform(post("/api/auth/test-notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getEmailServiceStatus_ShouldReturnStatusOrError() throws Exception {
        // Email service may not be configured in test environment
        mockMvc.perform(get("/api/auth/email-status"))
                .andDo(print())
                .andExpect(status().is(anyOf(is(200), is(500))));
    }

    @Test
    void sendTestEmail_ShouldAttemptSendOrError() throws Exception {
        Map<String, String> emailRequest = Map.of(
            "message", "Test email from integration test"
        );

        // Email service may not be configured in test environment
        mockMvc.perform(post("/api/auth/test-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().is(anyOf(is(200), is(500))));
    }

    @Test
    void forgotPassword_WithValidEmail_ShouldReturnSuccess() throws Exception {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("authtest@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("riceverai le istruzioni")));
    }

    @Test
    void forgotPassword_WithNonExistentEmail_ShouldStillReturnSuccess() throws Exception {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("nonexistent@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldReturnError() throws Exception {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest("invalid-token", "NewPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("non valido")));
    }

    @Test
    void refreshToken_WithoutToken_ShouldReturnBadRequest() throws Exception {
        Map<String, String> refreshRequest = Map.of("refreshToken", "");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
                .with(csrf())
                .with(anonymous()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is required"));
    }

    @Test
    void exportUserData_ShouldReturnUserDataAsJson() throws Exception {
        mockMvc.perform(get("/api/auth/export"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void deleteAccount_WithoutPassword_ShouldReturnBadRequest() throws Exception {
        Map<String, Object> deleteRequest = Map.of("password", "");

        mockMvc.perform(delete("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required to delete account"));
    }

    @Test
    void setup2FA_ShouldReturnSetupData() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/setup")
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.secret").exists())
                .andExpect(jsonPath("$.qrCodeUrl").exists());
    }

    @Test
    void enable2FA_WithoutRequiredFields_ShouldReturnBadRequest() throws Exception {
        Map<String, String> enable2FARequest = Map.of("secret", "", "code", "");

        mockMvc.perform(post("/api/auth/2fa/enable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enable2FARequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void disable2FA_WhenNotEnabled_ShouldReturnBadRequest() throws Exception {
        Map<String, String> disable2FARequest = Map.of("password", "TestPassword123");

        mockMvc.perform(post("/api/auth/2fa/disable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disable2FARequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("2FA is not enabled"));
    }

    @Test
    void verify2FA_WhenNotEnabled_ShouldReturnBadRequest() throws Exception {
        Map<String, String> verify2FARequest = Map.of("code", "123456");

        mockMvc.perform(post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verify2FARequest))
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("2FA is not enabled for this user"));
    }

    @Test
    void authEndpoints_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Test that protected endpoints require authentication
        mockMvc.perform(get("/api/auth/me")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/preferences")
                .with(anonymous()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"test\",\"newPassword\":\"test\"}")
                .with(csrf())
                .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}