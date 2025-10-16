package com.privatecal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.entity.TelegramRegistrationToken;
import com.privatecal.entity.User;
import com.privatecal.repository.TelegramRegistrationTokenRepository;
import com.privatecal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for managing Telegram bot registration and interactions
 */
@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private static final String TOKEN_PREFIX = "pcal_";
    private static final int TOKEN_LENGTH = 16;
    private static final int TOKEN_EXPIRY_MINUTES = 10;
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Value("${app.telegram.enabled:false}")
    private boolean telegramEnabled;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.api-url:https://api.telegram.org}")
    private String apiUrl;

    private final UserRepository userRepository;
    private final TelegramRegistrationTokenRepository tokenRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    public TelegramService(UserRepository userRepository,
                          TelegramRegistrationTokenRepository tokenRepository,
                          RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a registration token for a user
     * @param userId The user ID
     * @return The generated token string
     * @throws IllegalStateException if Telegram is not enabled or user not found
     */
    @Transactional
    public String generateRegistrationToken(Long userId) {
        if (!telegramEnabled) {
            throw new IllegalStateException("Telegram integration is not enabled");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if user already has a valid token
        Optional<TelegramRegistrationToken> existingToken = tokenRepository.findByUser(user);
        if (existingToken.isPresent() && existingToken.get().isValid()) {
            logger.debug("Returning existing valid token for user: {}", userId);
            return existingToken.get().getToken();
        }

        // Delete old tokens for this user
        existingToken.ifPresent(tokenRepository::delete);

        // Generate new token
        String token = TOKEN_PREFIX + generateRandomString(TOKEN_LENGTH);
        Instant expiresAt = Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        TelegramRegistrationToken registrationToken = new TelegramRegistrationToken(user, token, expiresAt);
        tokenRepository.save(registrationToken);

        logger.info("Generated Telegram registration token for user: {} (expires in {} minutes)",
                userId, TOKEN_EXPIRY_MINUTES);

        return token;
    }

    /**
     * Process a /start command with registration token
     * @param chatId The Telegram chat ID
     * @param token The registration token
     * @return true if registration was successful, false otherwise
     */
    @Transactional
    public boolean processStartCommand(String chatId, String token) {
        if (!telegramEnabled) {
            logger.warn("Telegram is disabled, ignoring /start command");
            return false;
        }

        // Find valid token
        Optional<TelegramRegistrationToken> tokenOpt = tokenRepository.findValidToken(token, Instant.now());

        if (tokenOpt.isEmpty()) {
            logger.warn("Invalid or expired registration token: {}", token);
            sendMessage(chatId, "❌ Invalid or expired registration token. Please generate a new one in P-Cal.");
            return false;
        }

        TelegramRegistrationToken registrationToken = tokenOpt.get();
        User user = registrationToken.getUser();

        // Check if chat ID is already registered to another user
        Optional<User> existingUser = userRepository.findByTelegramChatId(chatId);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            logger.warn("Chat ID {} is already registered to another user", chatId);
            sendMessage(chatId, "❌ This Telegram account is already linked to another P-Cal user.");
            return false;
        }

        // Update user with chat ID
        user.setTelegramChatId(chatId);
        userRepository.save(user);

        // Mark token as used
        registrationToken.setUsed(true);
        tokenRepository.save(registrationToken);

        logger.info("Successfully registered Telegram chat ID for user: {} (username: {})",
                user.getId(), user.getUsername());

        // Send confirmation message
        String confirmationMessage = String.format(
                "✅ <b>Registration Successful!</b>\n\n" +
                "Your Telegram account has been linked to P-Cal user: <b>%s</b>\n\n" +
                "You will now receive calendar reminders here. 📅\n\n" +
                "You can manage your notification preferences in P-Cal settings.",
                user.getUsername()
        );
        sendMessage(chatId, confirmationMessage);

        return true;
    }

    /**
     * Check if a user has registered their Telegram chat ID
     * @param userId The user ID
     * @return true if registered, false otherwise
     */
    public boolean isUserRegistered(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getTelegramChatId() != null && !user.getTelegramChatId().trim().isEmpty())
                .orElse(false);
    }

    /**
     * Unregister a user's Telegram chat ID
     * @param userId The user ID
     * @return true if unregistered, false otherwise
     */
    @Transactional
    public boolean unregisterUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (user.getTelegramChatId() == null || user.getTelegramChatId().trim().isEmpty()) {
            return false;
        }

        String chatId = user.getTelegramChatId();
        user.setTelegramChatId(null);
        userRepository.save(user);

        // Send notification to user
        sendMessage(chatId, "ℹ️ Your Telegram account has been unlinked from P-Cal.");

        logger.info("Unregistered Telegram chat ID for user: {}", userId);
        return true;
    }

    /**
     * Get bot information
     * @return Map with bot info or empty map if error
     */
    public Map<String, Object> getBotInfo() {
        if (!telegramEnabled || botToken == null || botToken.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            String url = String.format("%s/bot%s/getMe", apiUrl, botToken);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.get("ok").asBoolean()) {
                    JsonNode result = root.get("result");
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", result.get("id").asLong());
                    info.put("username", result.get("username").asText());
                    info.put("first_name", result.get("first_name").asText());
                    info.put("can_read_all_group_messages", result.has("can_read_all_group_messages") ?
                            result.get("can_read_all_group_messages").asBoolean() : false);
                    return info;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting bot info", e);
        }

        return Collections.emptyMap();
    }

    /**
     * Process incoming webhook update from Telegram
     * @param updateJson The update JSON from Telegram
     */
    @Transactional
    public void processWebhookUpdate(String updateJson) {
        try {
            JsonNode update = objectMapper.readTree(updateJson);

            // Process message if present
            if (update.has("message")) {
                JsonNode message = update.get("message");
                String chatId = message.get("chat").get("id").asText();
                String text = message.has("text") ? message.get("text").asText() : "";

                // Check if it's a /start command
                if (text.startsWith("/start ")) {
                    String token = text.substring(7).trim();
                    processStartCommand(chatId, token);
                } else if (text.equals("/start")) {
                    sendMessage(chatId,
                        "👋 Welcome to P-Cal Bot!\n\n" +
                        "To link your Telegram account with P-Cal:\n" +
                        "1. Go to P-Cal settings\n" +
                        "2. Navigate to Telegram section\n" +
                        "3. Click 'Generate Registration Token'\n" +
                        "4. Send the command shown there to this bot");
                }
            }

        } catch (Exception e) {
            logger.error("Error processing Telegram webhook update", e);
        }
    }

    /**
     * Send a message to a Telegram chat
     * @param chatId The chat ID
     * @param text The message text
     */
    private void sendMessage(String chatId, String text) {
        try {
            String url = String.format("%s/bot%s/sendMessage", apiUrl, botToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", text);
            requestBody.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        } catch (Exception e) {
            logger.error("Error sending Telegram message to chat: {}", chatId, e);
        }
    }

    /**
     * Generate a cryptographically secure random string
     * @param length The desired length
     * @return Random string
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALLOWED_CHARS.charAt(secureRandom.nextInt(ALLOWED_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Cleanup expired tokens (runs every hour)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            tokenRepository.deleteExpiredTokens(Instant.now());
            logger.debug("Cleaned up expired Telegram registration tokens");
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens", e);
        }
    }
}
