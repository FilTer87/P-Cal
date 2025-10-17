package com.privatecal.service.notification.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.dto.NotificationMessage;
import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.TelegramRegistrationToken;
import com.privatecal.entity.User;
import com.privatecal.repository.TelegramRegistrationTokenRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.service.notification.NotificationData;
import com.privatecal.service.notification.NotificationMessageBuilder;
import com.privatecal.service.notification.NotificationProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Telegram Bot notification provider implementation
 * Supports both webhook (for production with public HTTPS) and polling (for self-hosted/localhost)
 */
@Component
public class TelegramNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationProvider.class);
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

    @Value("${app.telegram.webhook-url:}")
    private String webhookUrl;

    @Value("${app.telegram.polling-interval:2000}")
    private long pollingInterval;

    @Value("${app.telegram.use-inline-buttons:false}")
    private boolean useInlineButtons;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final TelegramRegistrationTokenRepository tokenRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final TelegramFormatter telegramFormatter;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;
    private final ApplicationContext applicationContext;

    // Polling state
    private final AtomicBoolean pollingActive = new AtomicBoolean(false);
    private final AtomicLong lastUpdateId = new AtomicLong(0);
    private Thread pollingThread;

    public TelegramNotificationProvider(RestTemplateBuilder restTemplateBuilder,
                                       UserRepository userRepository,
                                       TelegramRegistrationTokenRepository tokenRepository,
                                       NotificationMessageBuilder messageBuilder,
                                       TelegramFormatter telegramFormatter,
                                       ObjectMapper objectMapper,
                                       ApplicationContext applicationContext) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.messageBuilder = messageBuilder;
        this.telegramFormatter = telegramFormatter;
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initialize() {
        if (!isEnabled()) {
            logger.info("Telegram provider is disabled");
            return;
        }

        if (botToken == null || botToken.trim().isEmpty()) {
            logger.warn("Telegram bot token is not configured");
            return;
        }

        // Determine if using webhook or polling
        if (webhookUrl != null && !webhookUrl.trim().isEmpty()) {
            logger.info("Telegram provider initialized with WEBHOOK mode: {}", webhookUrl);
            setupWebhook();
        } else {
            logger.info("Telegram provider initialized with POLLING mode (interval: {}ms)", pollingInterval);
            startPolling();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (pollingActive.get()) {
            logger.info("Shutting down Telegram polling...");
            stopPolling();
        }
    }

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        if (!isEnabled()) {
            logger.debug("Telegram provider is disabled");
            return false;
        }

        try {
            User user = reminder.getTask().getUser();
            NotificationData data = NotificationData.fromReminder(reminder);

            if (user.getTelegramChatId() == null || user.getTelegramChatId().trim().isEmpty()) {
                logger.warn("User {} has no Telegram chat ID configured", data.getUserId());
                return false;
            }

            // Build localized notification message
            NotificationMessage message = messageBuilder.buildTaskReminder(user, data);

            // Format for Telegram
            String formattedText = telegramFormatter.formatBody(message);

            // Include inline buttons only if configured (requires public HTTPS URL)
            // Telegram rejects inline keyboard buttons with HTTP or non-public URLs
            String replyMarkup = useInlineButtons ? telegramFormatter.formatActions(message) : null;

            return sendMessage(user.getTelegramChatId(), formattedText, replyMarkup);

        } catch (Exception e) {
            logger.error("Error sending Telegram notification for reminder ID: {}",
                    reminder.getId(), e);
            return false;
        }
    }

    @Override
    public boolean sendTestNotification(Long userId, String customMessage) {
        if (!isEnabled()) {
            logger.debug("Telegram provider is disabled - cannot send test notification");
            return false;
        }

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User with ID {} not found for test notification", userId);
                return false;
            }

            User user = userOpt.get();
            if (user.getTelegramChatId() == null || user.getTelegramChatId().trim().isEmpty()) {
                logger.warn("User {} has no Telegram chat ID configured for test notification", userId);
                return false;
            }

            // Build localized test notification
            NotificationMessage message = messageBuilder.buildTestNotification(user, customMessage);

            // Format for Telegram
            String formattedText = telegramFormatter.formatBody(message);

            // Send test message
            return sendMessage(user.getTelegramChatId(), formattedText, null);

        } catch (Exception e) {
            logger.error("Error sending Telegram test notification to user ID: {}", userId, e);
            return false;
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.TELEGRAM;
    }

    @Override
    public boolean isEnabled() {
        return telegramEnabled && botToken != null && !botToken.trim().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "Telegram";
    }

    /**
     * Send a message via Telegram Bot API
     */
    private boolean sendMessage(String chatId, String text, String replyMarkup) {
        try {
            String url = String.format("%s/bot%s/sendMessage", apiUrl, botToken);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", text);
            requestBody.put("parse_mode", "HTML");

            if (replyMarkup != null && !replyMarkup.trim().isEmpty()) {
                requestBody.put("reply_markup", objectMapper.readTree(replyMarkup));
            }

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Send request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Telegram message sent successfully to chat: {}", chatId);
                return true;
            } else {
                logger.warn("Failed to send Telegram message. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            logger.error("Network error sending Telegram message to chat: {}", chatId, e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending Telegram message to chat: {}", chatId, e);
            return false;
        }
    }

    /**
     * Setup webhook for receiving updates
     */
    private void setupWebhook() {
        try {
            String url = String.format("%s/bot%s/setWebhook", apiUrl, botToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("url", webhookUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Telegram webhook configured successfully: {}", webhookUrl);
            } else {
                logger.error("Failed to set Telegram webhook. Response: {}", response.getBody());
            }

        } catch (Exception e) {
            logger.error("Error setting up Telegram webhook", e);
        }
    }

    /**
     * Start polling for updates (long polling)
     */
    @Async
    private void startPolling() {
        if (pollingActive.get()) {
            logger.warn("Telegram polling is already active");
            return;
        }

        pollingActive.set(true);
        pollingThread = new Thread(this::pollUpdates, "TelegramPollingThread");
        pollingThread.setDaemon(true);
        pollingThread.start();
        logger.info("Telegram polling started");
    }

    /**
     * Stop polling for updates
     */
    private void stopPolling() {
        pollingActive.set(false);
        if (pollingThread != null) {
            pollingThread.interrupt();
            try {
                pollingThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Telegram polling stopped");
    }

    /**
     * Poll for updates from Telegram API
     */
    private void pollUpdates() {
        while (pollingActive.get() && !Thread.currentThread().isInterrupted()) {
            try {
                String url = String.format("%s/bot%s/getUpdates?offset=%d&timeout=30",
                        apiUrl, botToken, lastUpdateId.get() + 1);

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    processUpdates(response.getBody());
                }

                Thread.sleep(pollingInterval);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                // Timeout is expected with long polling (30 seconds), just continue
                if (e.getCause() instanceof java.net.SocketTimeoutException) {
                    logger.trace("Telegram long polling timeout (expected), continuing...");
                } else {
                    logger.error("Network error polling Telegram updates", e);
                    try {
                        Thread.sleep(pollingInterval * 2); // Back off on error
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Error polling Telegram updates", e);
                try {
                    Thread.sleep(pollingInterval * 2); // Back off on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Process updates from Telegram
     */
    private void processUpdates(String updatesJson) {
        try {
            JsonNode root = objectMapper.readTree(updatesJson);
            JsonNode result = root.get("result");

            if (result != null && result.isArray()) {
                for (JsonNode update : result) {
                    long updateId = update.get("update_id").asLong();
                    lastUpdateId.set(Math.max(lastUpdateId.get(), updateId));

                    // Process message if present
                    if (update.has("message")) {
                        processMessage(update.get("message"));
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error processing Telegram updates", e);
        }
    }

    /**
     * Process a single message (for /start command with registration token)
     */
    private void processMessage(JsonNode message) {
        logger.debug("Received Telegram message: {}", message);

        try {
            // Extract chat ID
            String chatId = message.get("chat").get("id").asText();

            // Check if message has text
            if (!message.has("text")) {
                return;
            }

            String text = message.get("text").asText().trim();

            // Check for /start command with token
            if (text.startsWith("/start ")) {
                String token = text.substring(7).trim(); // Remove "/start "

                if (token.startsWith("pcal_")) {
                    logger.info("Processing registration token from chat ID: {}", chatId);
                    // Get bean from ApplicationContext to invoke through Spring proxy for @Transactional to work
                    TelegramNotificationProvider proxy = applicationContext.getBean(TelegramNotificationProvider.class);
                    boolean success = proxy.processStartCommand(chatId, token);

                    if (success) {
                        logger.info("Successfully registered Telegram chat ID: {}", chatId);
                    } else {
                        logger.warn("Failed to register Telegram chat ID: {}", chatId);
                    }
                }
            } else if (text.equals("/start")) {
                sendSimpleMessage(chatId,
                    "👋 Welcome to P-Cal Bot!\n\n" +
                    "To link your Telegram account with P-Cal:\n" +
                    "1. Go to P-Cal settings\n" +
                    "2. Navigate to Telegram section\n" +
                    "3. Click 'Generate Registration Token'\n" +
                    "4. Send the command shown there to this bot");
            }
        } catch (Exception e) {
            logger.error("Error processing Telegram message", e);
        }
    }

    /**
     * Get bot information for testing
     */
    public Map<String, Object> getBotInfo() {
        if (!isEnabled() || botToken == null || botToken.trim().isEmpty()) {
            return new HashMap<>();
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
        return new HashMap<>();
    }

    // ==================== Public API Methods ====================
    // These methods are called by TelegramController

    /**
     * Generate a registration token for a user
     * @param userId The user ID
     * @return The generated token string
     * @throws IllegalStateException if Telegram is not enabled
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public String generateRegistrationToken(Long userId) {
        if (!isEnabled()) {
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
     * Uses REQUIRES_NEW to create a new transaction even when called from non-transactional polling thread
     * @param chatId The Telegram chat ID
     * @param token The registration token
     * @return true if registration was successful, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processStartCommand(String chatId, String token) {
        logger.info("processStartCommand called with chatId: {} and token: {}", chatId, token);

        // Find valid token
        Optional<TelegramRegistrationToken> tokenOpt = tokenRepository.findValidToken(token, Instant.now());
        logger.info("Token lookup result: {}", tokenOpt.isPresent() ? "FOUND" : "NOT FOUND");

        if (tokenOpt.isEmpty()) {
            logger.warn("Invalid or expired registration token: {}", token);
            // Try to send error message (will fail silently if bot is not configured)
            sendSimpleMessage(chatId, "❌ Invalid or expired registration token. Please generate a new one in P-Cal.");
            return false;
        }

        TelegramRegistrationToken registrationToken = tokenOpt.get();
        User user = registrationToken.getUser();

        // Check if chat ID is already registered to another user
        Optional<User> existingUser = userRepository.findByTelegramChatId(chatId);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            logger.warn("Chat ID {} is already registered to another user", chatId);
            sendSimpleMessage(chatId, "❌ This Telegram account is already linked to another P-Cal user.");
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

        // Send confirmation message (will fail silently if bot is not configured)
        String confirmationMessage = String.format(
                "✅ <b>Registration Successful!</b>\n\n" +
                "Your Telegram account has been linked to P-Cal user: <b>%s</b>\n\n" +
                "You will now receive calendar reminders here. 📅\n\n" +
                "You can manage your notification preferences in P-Cal settings.",
                user.getUsername()
        );
        sendSimpleMessage(chatId, confirmationMessage);

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
        sendSimpleMessage(chatId, "ℹ️ Your Telegram account has been unlinked from P-Cal.");

        logger.info("Unregistered Telegram chat ID for user: {}", userId);
        return true;
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
                processMessage(update.get("message"));
            }

        } catch (Exception e) {
            logger.error("Error processing Telegram webhook update", e);
        }
    }

    /**
     * Cleanup expired tokens (runs every hour)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        if (!isEnabled()) {
            return;
        }

        try {
            tokenRepository.deleteExpiredTokens(Instant.now());
            logger.debug("Cleaned up expired Telegram registration tokens");
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens", e);
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Send a simple text message to a Telegram chat (without reply markup)
     * @param chatId The chat ID
     * @param text The message text
     */
    private void sendSimpleMessage(String chatId, String text) {
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
}
