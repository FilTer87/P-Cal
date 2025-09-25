package com.privatecal.controller;

import com.privatecal.dto.AuthRequest;
import com.privatecal.dto.AuthResponse;
import com.privatecal.dto.NotificationType;
import com.privatecal.dto.UserResponse;
import com.privatecal.dto.UserPreferencesRequest;
import com.privatecal.dto.UserPreferencesResponse;
import com.privatecal.dto.TwoFactorSetupResponse;
import com.privatecal.dto.TwoFactorVerifyRequest;
import com.privatecal.dto.TwoFactorDisableRequest;
import com.privatecal.entity.User;
import com.privatecal.service.AuthService;
import com.privatecal.service.NotificationService;
import com.privatecal.service.UserService;
import com.privatecal.service.TwoFactorService;
import com.privatecal.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private EmailService emailService;

    /**
     * User login endpoint
     * POST /api/auth/login
     */
    @Operation(
        summary = "User Login", 
        description = "Authenticate user with username/email and password. Returns JWT tokens on successful authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful", 
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or request format"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "429", description = "Too many failed login attempts")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "Login credentials", required = true)
            @RequestBody AuthRequest loginRequest, 
            HttpServletRequest request) {
        try {
            // Custom validation for login request
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Username is required"));
            }
            
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Password is required"));
            }
            
            logger.info("Login attempt for user: {} from IP: {}", 
                       loginRequest.getUsername(), getClientIpAddress(request));
            
            AuthResponse response = authService.login(loginRequest);

            // Check if 2FA is required
            if (response.isRequiresTwoFactor()) {
                logger.info("2FA required for user: {}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            if (response.isSuccess()) {
                logger.info("Login successful for user: {}", loginRequest.getUsername());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Login failed for user: {} - {}",
                           loginRequest.getUsername(), response.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Login error for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Login failed due to server error"));
        }
    }
    
    /**
     * User registration endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest registerRequest,
                                               HttpServletRequest request) {
        try {
            // Custom validation for registration request
            if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Username is required"));
            }
            
            if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Email is required"));
            }
            
            if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Password is required"));
            }
            
            // Basic email validation
            if (!registerRequest.getEmail().contains("@") || !registerRequest.getEmail().contains(".")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Email must be valid"));
            }
            
            logger.info("Registration attempt for user: {} from IP: {}", 
                       registerRequest.getUsername(), getClientIpAddress(request));
            
            AuthResponse response = authService.register(registerRequest);
            
            if (response.isSuccess()) {
                logger.info("Registration successful for user: {}", registerRequest.getUsername());
                
                // Send welcome notification after successful registration
                try {
                    notificationService.sendTestNotification(
                        response.getUser().getId(),
                        NotificationType.PUSH,
                        "Welcome to P-Cal! Your account has been created successfully."
                    );
                } catch (Exception e) {
                    logger.warn("Failed to send welcome notification", e);
                }
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Registration failed for user: {} - {}", 
                           registerRequest.getUsername(), response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Registration error for user: {}", registerRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Registration failed due to server error"));
        }
    }
    
    /**
     * Token refresh endpoint
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Refresh token is required"));
            }
            
            logger.debug("Token refresh attempt");
            
            AuthResponse response = authService.refreshToken(refreshToken);
            
            if (response.isSuccess()) {
                logger.debug("Token refresh successful");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Token refresh failed - {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Token refresh failed due to server error"));
        }
    }
    
    /**
     * Get current user profile
     * GET /api/auth/me
     * GET /api/auth/profile
     */
    @GetMapping({"/me", "/profile"})
    public ResponseEntity<UserResponse> getCurrentUser() {
        try {
            UserResponse user = userService.getCurrentUserProfile();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error getting current user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update current user profile
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestBody UserResponse updateRequest) {
        try {
            UserResponse updatedUser = userService.updateCurrentUserProfile(updateRequest);
            logger.info("User profile updated: {}", updatedUser.getUsername());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }
    
    /**
     * Change password
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> request) {
        try {
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");
            
            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Current password and new password are required"));
            }
            
            userService.changeCurrentUserPassword(currentPassword, newPassword);
            
            logger.info("Password changed successfully");
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Password changed successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Check username availability
     * GET /api/auth/check-username/{username}
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@PathVariable String username) {
        try {
            boolean available = authService.isUsernameAvailable(username);
            
            return ResponseEntity.ok(Map.of(
                "username", username,
                "available", available,
                "message", available ? "Username is available" : "Username is already taken"
            ));
            
        } catch (Exception e) {
            logger.error("Error checking username availability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error"));
        }
    }
    
    /**
     * Check email availability
     * GET /api/auth/check-email/{email}
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(@RequestParam String email) {
        try {
            boolean available = authService.isEmailAvailable(email);
            
            return ResponseEntity.ok(Map.of(
                "email", email,
                "available", available,
                "message", available ? "Email is available" : "Email is already registered"
            ));
            
        } catch (Exception e) {
            logger.error("Error checking email availability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error"));
        }
    }
    
    /**
     * Get notification settings for current user
     * GET /api/auth/notification-settings
     * DEPRECATED: Use /api/notifications/config and /api/notifications/ntfy/subscription-url instead
     */
    @GetMapping("/notification-settings")
    public ResponseEntity<Map<String, Object>> getNotificationSettings() {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "This endpoint is deprecated. Use /api/notifications/config for notification configuration.",
                "configEndpoint", "/api/notifications/config",
                "subscriptionEndpoint", "/api/notifications/ntfy/subscription-url",
                "testEndpoint", "/api/notifications/test"
            ));
        } catch (Exception e) {
            logger.error("Error getting notification settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error"));
        }
    }
    
    /**
     * Send test notification
     * POST /api/auth/test-notification
     */
    @PostMapping("/test-notification")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody Map<String, String> request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            String message = request.getOrDefault("message", "This is a test notification from PrivateCal");

            notificationService.sendTestNotification(currentUserId, NotificationType.PUSH, message);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test notification sent successfully"
            ));

        } catch (Exception e) {
            logger.error("Error sending test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to send test notification"));
        }
    }

    /**
     * Test email configuration
     * POST /api/auth/test-email
     */
    @Operation(
        summary = "Test Email Configuration",
        description = "Send a test email to verify email configuration is working correctly."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Test email sent successfully"),
        @ApiResponse(responseCode = "400", description = "Email service not available or invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/test-email")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            // Check if email service is available
            if (!emailService.isEmailServiceAvailable()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "message", "Email service is not available or not properly configured",
                            "config", emailService.getEmailConfigInfo()
                        ));
            }

            // Get current user
            User currentUser = userService.getCurrentUser();
            if (currentUser.getEmail() == null || currentUser.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "message", "Current user has no email address configured"
                        ));
            }

            // Get custom message or use default
            String message = request.getOrDefault("message", "This is a test email to verify P-Cal email configuration is working correctly!");

            // Send test email
            boolean success = emailService.sendTestEmail(currentUser.getEmail(), message);

            if (success) {
                logger.info("Test email sent successfully to: {}", currentUser.getEmail());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent successfully to " + currentUser.getEmail(),
                    "recipient", currentUser.getEmail()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "success", false,
                            "message", "Failed to send test email"
                        ));
            }

        } catch (Exception e) {
            logger.error("Error sending test email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Failed to send test email: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get email service status
     * GET /api/auth/email-status
     */
    @Operation(
        summary = "Get Email Service Status",
        description = "Check if email service is properly configured and available."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email service status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/email-status")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Map<String, Object>> getEmailServiceStatus() {
        try {
            boolean isAvailable = emailService.isEmailServiceAvailable();
            String configInfo = emailService.getEmailConfigInfo();

            return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "configuration", configInfo,
                "message", isAvailable ? "Email service is available" : "Email service is not properly configured"
            ));

        } catch (Exception e) {
            logger.error("Error checking email service status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "available", false,
                        "message", "Error checking email service status: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get current user preferences
     * GET /api/auth/preferences
     */
    @Operation(
        summary = "Get User Preferences",
        description = "Retrieve current user's application preferences including theme, timezone, and notification settings."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserPreferencesResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/preferences")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<UserPreferencesResponse> getUserPreferences() {
        try {
            UserPreferencesResponse preferences = userService.getCurrentUserPreferences();
            logger.debug("User preferences retrieved successfully");
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            logger.error("Error getting user preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update current user preferences
     * PUT /api/auth/preferences
     */
    @Operation(
        summary = "Update User Preferences",
        description = "Update current user's application preferences. Only provided fields will be updated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully",
                    content = @Content(schema = @Schema(implementation = UserPreferencesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid preferences data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PutMapping("/preferences")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<UserPreferencesResponse> updateUserPreferences(
            @Parameter(description = "User preferences to update", required = true)
            @Valid @RequestBody UserPreferencesRequest preferencesRequest) {
        try {
            UserPreferencesResponse updatedPreferences = userService.updateCurrentUserPreferences(preferencesRequest);
            logger.info("User preferences updated successfully");
            return ResponseEntity.ok(updatedPreferences);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid preferences data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating user preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Logout endpoint (client-side token invalidation)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // Since we're using stateless JWT tokens, logout is handled client-side
        // The client should remove the tokens from storage
        logger.info("Logout request received");
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Logged out successfully"
        ));
    }
    
    /**
     * Delete current user account
     * DELETE /api/auth/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody Map<String, Object> request) {
        try {
            String password = null;

            if (request.containsKey("data") && request.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> data = (Map<String, String>) request.get("data");
                password = data.get("password");
            } else if (request.containsKey("password")) {
                password = (String) request.get("password");
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Password is required to delete account"));
            }

            // Verify password before deletion
            Long currentUserId = userService.getCurrentUserId();
            userService.changePassword(currentUserId, password, password); // This will throw if password is wrong

            // Delete account
            userService.deleteCurrentUserAccount();

            logger.warn("User account deleted successfully");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Account deleted successfully"
            ));

        } catch (Exception e) {
            logger.error("Error deleting account", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Setup Two-Factor Authentication
     * POST /api/auth/2fa/setup
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setupTwoFactor() {
        try {
            User currentUser = userService.getCurrentUser();

            if (currentUser.getTwoFactorEnabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "2FA is already enabled"));
            }

            TwoFactorSetupResponse setupResponse = twoFactorService.setupTwoFactor(currentUser);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "secret", setupResponse.getSecret(),
                "qrCodeUrl", setupResponse.getQrCodeUrl(),
                "manualEntryKey", setupResponse.getManualEntryKey()
            ));

        } catch (Exception e) {
            logger.error("Error setting up 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to setup 2FA"));
        }
    }

    /**
     * Verify and Enable Two-Factor Authentication
     * POST /api/auth/2fa/enable
     */
    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enableTwoFactor(@Valid @RequestBody Map<String, String> request) {
        try {
            String secret = request.get("secret");
            String code = request.get("code");

            if (secret == null || code == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Secret and code are required"));
            }

            User currentUser = userService.getCurrentUser();

            if (currentUser.getTwoFactorEnabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "2FA is already enabled"));
            }

            boolean isValid = twoFactorService.verifyCode(secret, code);

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Invalid verification code"));
            }

            twoFactorService.enableTwoFactor(currentUser, secret);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "2FA enabled successfully"
            ));

        } catch (Exception e) {
            logger.error("Error enabling 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to enable 2FA"));
        }
    }

    /**
     * Disable Two-Factor Authentication
     * POST /api/auth/2fa/disable
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFactor(@Valid @RequestBody TwoFactorDisableRequest request) {
        try {
            User currentUser = userService.getCurrentUser();

            if (!currentUser.getTwoFactorEnabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "2FA is not enabled"));
            }

            boolean passwordValid = authService.verifyPassword(currentUser, request.getPassword());

            if (!passwordValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid password"));
            }

            twoFactorService.disableTwoFactor(currentUser);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "2FA disabled successfully"
            ));

        } catch (Exception e) {
            logger.error("Error disabling 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to disable 2FA"));
        }
    }

    /**
     * Verify Two-Factor Authentication code during login
     * POST /api/auth/2fa/verify
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFactorCode(@Valid @RequestBody TwoFactorVerifyRequest request) {
        try {
            User currentUser = userService.getCurrentUser();

            if (!currentUser.getTwoFactorEnabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "2FA is not enabled for this user"));
            }

            boolean isValid = twoFactorService.verifyCode(currentUser.getTwoFactorSecret(), request.getCode());

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid verification code"));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "2FA verification successful"
            ));

        } catch (Exception e) {
            logger.error("Error verifying 2FA code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to verify 2FA code"));
        }
    }

    /**
     * Export user data (GDPR compliance)
     * GET /api/auth/export
     */
    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> exportUserData() {
        try {
            User currentUser = userService.getCurrentUser();

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String timestamp = java.time.LocalDateTime.now().format(formatter);
            String filename = String.format("p-cal_%s_%s.json", currentUser.getUsername(), timestamp);

            java.util.Map<String, Object> exportData = new java.util.LinkedHashMap<>();

            exportData.put("exportDate", java.time.Instant.now().toString());
            exportData.put("userId", currentUser.getId());

            java.util.Map<String, Object> userData = new java.util.LinkedHashMap<>();
            userData.put("username", currentUser.getUsername());
            userData.put("email", currentUser.getEmail());
            userData.put("firstName", currentUser.getFirstName());
            userData.put("lastName", currentUser.getLastName());
            userData.put("accountCreatedAt", currentUser.getCreatedAt());
            userData.put("accountUpdatedAt", currentUser.getUpdatedAt());
            exportData.put("user", userData);

            java.util.Map<String, Object> preferences = new java.util.LinkedHashMap<>();
            preferences.put("theme", currentUser.getTheme());
            preferences.put("timezone", currentUser.getTimezone());
            preferences.put("timeFormat", currentUser.getTimeFormat());
            preferences.put("calendarView", currentUser.getCalendarView());
            preferences.put("emailNotifications", currentUser.getEmailNotifications());
            preferences.put("reminderNotifications", currentUser.getReminderNotifications());
            exportData.put("preferences", preferences);

            java.util.Map<String, Object> security = new java.util.LinkedHashMap<>();
            security.put("twoFactorEnabled", currentUser.getTwoFactorEnabled());
            exportData.put("security", security);

            java.util.List<com.privatecal.entity.Task> userTasks = currentUser.getTasks();
            java.util.List<java.util.Map<String, Object>> tasksData = new java.util.ArrayList<>();

            for (com.privatecal.entity.Task task : userTasks) {
                java.util.Map<String, Object> taskData = new java.util.LinkedHashMap<>();
                taskData.put("id", task.getId());
                taskData.put("title", task.getTitle());
                taskData.put("description", task.getDescription());
                taskData.put("startDatetime", task.getStartDatetime().toString());
                taskData.put("endDatetime", task.getEndDatetime().toString());
                taskData.put("color", task.getColor());
                taskData.put("location", task.getLocation());
                taskData.put("createdAt", task.getCreatedAt().toString());
                taskData.put("updatedAt", task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : null);

                java.util.List<java.util.Map<String, Object>> remindersData = new java.util.ArrayList<>();
                for (com.privatecal.entity.Reminder reminder : task.getReminders()) {
                    java.util.Map<String, Object> reminderData = new java.util.LinkedHashMap<>();
                    reminderData.put("id", reminder.getId());
                    reminderData.put("reminderTime", reminder.getReminderTime().toString());
                    reminderData.put("reminderOffsetMinutes", reminder.getReminderOffsetMinutes());
                    reminderData.put("notificationType", reminder.getNotificationType().toString());
                    reminderData.put("isSent", reminder.getIsSent());
                    reminderData.put("createdAt", reminder.getCreatedAt().toString());
                    remindersData.add(reminderData);
                }
                taskData.put("reminders", remindersData);

                tasksData.add(taskData);
            }

            exportData.put("tasks", tasksData);
            exportData.put("statistics", Map.of(
                "totalTasks", tasksData.size(),
                "totalReminders", tasksData.stream()
                    .mapToInt(task -> ((java.util.List<?>) task.get("reminders")).size())
                    .sum()
            ));

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            String jsonContent = objectMapper.writeValueAsString(exportData);

            byte[] bytes = jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(bytes);

            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                       "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .contentLength(bytes.length)
                .body(resource);

        } catch (Exception e) {
            logger.error("Error exporting user data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}