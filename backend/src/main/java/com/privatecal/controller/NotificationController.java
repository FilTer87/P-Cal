package com.privatecal.controller;

import com.privatecal.dto.NotificationType;
import com.privatecal.service.NotificationService;
import com.privatecal.security.UserPrincipal;
import com.privatecal.service.UserService;
import com.privatecal.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for notification management
 * Provides APIs for NTFY configuration and test notifications
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * Get notification configuration for frontend
     * Provides NTFY server URL and topic prefix (read-only)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getNotificationConfig() {
        Map<String, Object> config = new HashMap<>();

        // Provide NTFY server URL for client configuration (read-only)
        config.put("ntfyServerUrl", notificationService.getNtfyServerUrl());
        config.put("ntfyTopicPrefix", notificationService.getNtfyTopicPrefix());
        config.put("enabledProviders", notificationService.getEnabledProviders());

        // Indicate which notification types are supported
        config.put("supportsPush", notificationService.isNotificationTypeSupported(NotificationType.PUSH));
        config.put("supportsEmail", notificationService.isNotificationTypeSupported(NotificationType.EMAIL));

        logger.debug("Notification config requested: {}", config);
        return ResponseEntity.ok(config);
    }

    /**
     * Update user's NTFY topic
     * Validates format and uniqueness before updating
     */
    @PutMapping("/ntfy/topic")
    public ResponseEntity<Map<String, Object>> updateNtfyTopic(@RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();
        Long userId = userService.getCurrentUser().getId();

        try {
            String newTopic = request.get("topic");
            if (newTopic == null || newTopic.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Topic cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = notificationService.updateUserNtfyTopic(
                userId,
                newTopic.trim()
            );

            if (success) {
                response.put("success", true);
                response.put("message", "NTFY topic updated successfully");
                response.put("topic", newTopic.trim());
                logger.info("User {} updated NTFY topic to: {}", userId, newTopic);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Invalid topic format or topic already in use. Topic must start with your user prefix.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error updating NTFY topic for user {}", userId, e);
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Send test notification
     * Allows users to test their notification configuration
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();
        Long userId = userService.getCurrentUser().getId();

        try {
            // Get notification type (default to PUSH)
            String typeStr = (String) request.getOrDefault("type", "PUSH");
            NotificationType type;
            try {
                type = NotificationType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("error", "Invalid notification type. Use PUSH or EMAIL");
                return ResponseEntity.badRequest().body(response);
            }

            // Get custom message or use default
            String message = (String) request.getOrDefault("message",
                "🧪 This is a test notification from P-Cal. If you receive this, your notification setup is working correctly!");

            boolean success = notificationService.sendTestNotification(
                userId,
                type,
                message
            );

            if (success) {
                response.put("success", true);
                response.put("message", "Test notification sent successfully via " + type);
                logger.info("Test notification sent to user {} via {}", userId, type);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to send test notification. Check your configuration and try again.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error sending test notification for user {}", userId, e);
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get user's current NTFY subscription URL
     * Used for QR code generation and manual subscription
     */
    @GetMapping("/ntfy/subscription-url")
    public ResponseEntity<Map<String, Object>> getNtfySubscriptionUrl() {

        Map<String, Object> response = new HashMap<>();

        // Get user from database to access their NTFY topic
        User user = userService.getCurrentUser();
        // User user = userService.getUserById(userPrincipal.getUserId());
        try {
            if (user.getNtfyTopic() == null || user.getNtfyTopic().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "User has no NTFY topic configured. Please contact support.");
                return ResponseEntity.badRequest().body(response);
            }

            // Construct subscription URL: ntfyServerUrl + "/" + user.getNtfyTopic()
            String subscriptionUrl = notificationService.getNtfyServerUrl() + "/" + user.getNtfyTopic();

            response.put("success", true);
            response.put("subscriptionUrl", subscriptionUrl);
            response.put("topic", user.getNtfyTopic());
            response.put("serverUrl", notificationService.getNtfyServerUrl());

            logger.info("Subscription URL provided for user {}: {}", user.getId(), user.getNtfyTopic());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting subscription URL for user {}", user.getId(), e);
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Generate QR code for NTFY subscription (future feature)
     * TODO: Implement using ZXing library
     */
    @GetMapping("/ntfy/qr-code")
    public ResponseEntity<Map<String, Object>> generateNtfyQrCode() {

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "QR code generation not yet implemented. Will be added in a future update using ZXing library.");

        // TODO: Implement QR code generation
        // 1. Get user's subscription URL
        // 2. Generate QR code using ZXing
        // 3. Return QR code as base64 image or binary data

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    /**
     * Get notification statistics and status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            status.put("enabledProviders", notificationService.getEnabledProviders());
            status.put("supportedTypes", Map.of(
                "PUSH", notificationService.isNotificationTypeSupported(NotificationType.PUSH),
                "EMAIL", notificationService.isNotificationTypeSupported(NotificationType.EMAIL)
            ));
            status.put("ntfyServerUrl", notificationService.getNtfyServerUrl());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting notification status", e);
            status.put("error", "Failed to get notification status");
            return ResponseEntity.internalServerError().body(status);
        }
    }
}