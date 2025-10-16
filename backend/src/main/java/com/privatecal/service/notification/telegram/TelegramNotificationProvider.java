package com.privatecal.service.notification.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.dto.NotificationMessage;
import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
import com.privatecal.service.notification.NotificationData;
import com.privatecal.service.notification.NotificationMessageBuilder;
import com.privatecal.service.notification.NotificationProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final TelegramFormatter telegramFormatter;
    private final ObjectMapper objectMapper;

    // Polling state
    private final AtomicBoolean pollingActive = new AtomicBoolean(false);
    private final AtomicLong lastUpdateId = new AtomicLong(0);
    private Thread pollingThread;

    public TelegramNotificationProvider(RestTemplateBuilder restTemplateBuilder,
                                       UserRepository userRepository,
                                       NotificationMessageBuilder messageBuilder,
                                       TelegramFormatter telegramFormatter,
                                       ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.userRepository = userRepository;
        this.messageBuilder = messageBuilder;
        this.telegramFormatter = telegramFormatter;
        this.objectMapper = objectMapper;
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
            String replyMarkup = telegramFormatter.formatActions(message);

            // Send via Telegram API
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
     * This will be implemented in the TelegramService
     */
    private void processMessage(JsonNode message) {
        // This method will be called by polling
        // The actual logic will be in TelegramService to handle /start commands
        logger.debug("Received Telegram message: {}", message);
    }

    /**
     * Get bot information for testing
     */
    public Map<String, Object> getBotInfo() {
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
                    return info;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting bot info", e);
        }
        return null;
    }
}
