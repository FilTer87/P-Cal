package com.privatecal.controller;

import com.privatecal.dto.TelegramRegistrationResponse;
import com.privatecal.dto.TelegramStatusResponse;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
import com.privatecal.service.notification.telegram.TelegramNotificationProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Telegram bot integration
 */
@RestController
@RequestMapping("/api/telegram")
@Tag(name = "Telegram", description = "Telegram bot integration endpoints")
public class TelegramController {

    private static final Logger logger = LoggerFactory.getLogger(TelegramController.class);

    private final TelegramNotificationProvider telegramProvider;
    private final UserRepository userRepository;

    public TelegramController(TelegramNotificationProvider telegramProvider, UserRepository userRepository) {
        this.telegramProvider = telegramProvider;
        this.userRepository = userRepository;
    }

    /**
     * Generate a registration token for linking Telegram account
     */
    @PostMapping("/generate-token")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Generate Telegram registration token",
               description = "Generates a temporary token for linking user's Telegram account")
    public ResponseEntity<?> generateRegistrationToken(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            String token = telegramProvider.generateRegistrationToken(user.getId());

            // Get bot info
            Map<String, Object> botInfo = telegramProvider.getBotInfo();
            String botUsername = botInfo.containsKey("username") ?
                    (String) botInfo.get("username") : "PrivateCalBot";

            TelegramRegistrationResponse response = new TelegramRegistrationResponse(
                    token,
                    botUsername,
                    600 // 10 minutes
            );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            logger.error("Error generating Telegram token", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error generating Telegram token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate registration token"));
        }
    }

    /**
     * Check Telegram registration status
     */
    @GetMapping("/status")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Check Telegram registration status",
               description = "Check if user has linked their Telegram account")
    public ResponseEntity<TelegramStatusResponse> checkStatus(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            boolean registered = telegramProvider.isUserRegistered(user.getId());
            String chatId = registered ? user.getTelegramChatId() : null;

            return ResponseEntity.ok(new TelegramStatusResponse(registered, chatId));

        } catch (Exception e) {
            logger.error("Error checking Telegram status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TelegramStatusResponse(false, null));
        }
    }

    /**
     * Unlink Telegram account
     */
    @DeleteMapping("/unlink")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Unlink Telegram account",
               description = "Remove Telegram chat ID from user account")
    public ResponseEntity<?> unlinkTelegram(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            boolean success = telegramProvider.unregisterUser(user.getId());

            if (success) {
                return ResponseEntity.ok(Map.of("message", "Telegram account unlinked successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No Telegram account linked"));
            }

        } catch (Exception e) {
            logger.error("Error unlinking Telegram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unlink Telegram account"));
        }
    }

    /**
     * Webhook endpoint for receiving updates from Telegram
     * This endpoint is called by Telegram when webhook is configured
     */
    @PostMapping("/webhook")
    @Operation(summary = "Telegram webhook endpoint",
               description = "Receives updates from Telegram (internal use)")
    public ResponseEntity<Void> handleWebhook(@RequestBody String update) {
        try {
            logger.debug("Received Telegram webhook update");
            telegramProvider.processWebhookUpdate(update);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error processing Telegram webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bot information (for testing/debugging)
     */
    @GetMapping("/bot-info")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Get bot information",
               description = "Get Telegram bot information (admin/debug)")
    public ResponseEntity<?> getBotInfo() {
        try {
            Map<String, Object> botInfo = telegramProvider.getBotInfo();

            if (botInfo.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Telegram bot not configured or unavailable"));
            }

            return ResponseEntity.ok(botInfo);

        } catch (Exception e) {
            logger.error("Error getting bot info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get bot information"));
        }
    }
}
