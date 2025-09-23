package com.privatecal.controller;

import com.privatecal.dto.AuthRequest;
import com.privatecal.dto.AuthResponse;
import com.privatecal.dto.UserResponse;
import com.privatecal.dto.UserPreferencesRequest;
import com.privatecal.dto.UserPreferencesResponse;
import com.privatecal.service.AuthService;
import com.privatecal.service.NotificationService;
import com.privatecal.service.UserService;
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
     * PUT /api/auth/me
     */
    @PutMapping("/me")
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
     */
    @GetMapping("/notification-settings")
    public ResponseEntity<Map<String, Object>> getNotificationSettings() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            String ntfyTopic = notificationService.getNtfyTopicForUser(currentUserId);
            String subscriptionUrl = notificationService.getNtfySubscriptionUrl(currentUserId);
            boolean notificationsEnabled = notificationService.areNotificationsEnabled();
            
            return ResponseEntity.ok(Map.of(
                "notificationsEnabled", notificationsEnabled,
                "ntfyTopic", ntfyTopic,
                "subscriptionUrl", subscriptionUrl,
                "instructions", "Subscribe to the NTFY topic to receive push notifications"
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

            notificationService.sendTestNotification(currentUserId, message);

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
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody Map<String, String> request) {
        try {
            String password = request.get("password");
            
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