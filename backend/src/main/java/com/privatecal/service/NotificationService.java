package com.privatecal.service;

import com.privatecal.entity.Reminder;
import com.privatecal.entity.User;
import com.privatecal.dto.NotificationType;
import com.privatecal.service.notification.NotificationProvider;
import com.privatecal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Unified notification service using strategy pattern
 * Supports multiple notification providers (NTFY, Email, future providers like Gotify)
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Value("${app.ntfy.topic-prefix:calendar-user}")
    private String ntfyTopicPrefix;

    @Value("${app.ntfy.server-url}")
    private String ntfyServerUrl;

    @Value("${app.ntfy.auth-token:}")
    private String ntfyAuthToken;

    private final List<NotificationProvider> providers;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // Characters for random topic generation: a-z, A-Z, 0-9
    private static final String TOPIC_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public NotificationService(
            @Autowired List<NotificationProvider> providers,
            UserRepository userRepository) {
        this.providers = providers;
        this.userRepository = userRepository;

        logger.info("NotificationService initialized with {} providers: {}",
                   providers.size(),
                   providers.stream().map(NotificationProvider::getProviderName).toList());
    }

    /**
     * Send notification for a reminder using the appropriate provider
     */
    @Async
    public void sendReminderNotification(Reminder reminder) {
        if (reminder == null) {
            logger.warn("Cannot send notification: reminder is null");
            return;
        }

        NotificationType notificationType = reminder.getNotificationType();
        if (notificationType == null) {
            logger.warn("Cannot send notification: reminder {} has no notification type", reminder.getId());
            return;
        }

        logger.debug("Sending {} notification for reminder ID: {}", notificationType, reminder.getId());

        // Find provider that supports this notification type
        Optional<NotificationProvider> provider = providers.stream()
                .filter(p -> p.supports(notificationType) && p.isEnabled())
                .findFirst();

        if (provider.isEmpty()) {
            logger.warn("No enabled provider found for notification type: {}", notificationType);
            return;
        }

        // Send notification
        NotificationProvider selectedProvider = provider.get();
        try {
            boolean success = selectedProvider.sendReminderNotification(reminder);
            if (success) {
                logger.info("Notification sent successfully via {} for reminder ID: {}",
                        selectedProvider.getProviderName(), reminder.getId());
            } else {
                logger.warn("Failed to send notification via {} for reminder ID: {}",
                        selectedProvider.getProviderName(), reminder.getId());
            }
        } catch (Exception e) {
            logger.error("Error sending notification via {} for reminder ID: {}",
                    selectedProvider.getProviderName(), reminder.getId(), e);
        }
    }

    /**
     * Send test notification to a user
     */
    public boolean sendTestNotification(Long userId, NotificationType type, String message) {
        if (userId == null || type == null) {
            logger.warn("Cannot send test notification: userId or type is null");
            return false;
        }

        // Find provider for the notification type
        Optional<NotificationProvider> provider = providers.stream()
                .filter(p -> p.supports(type) && p.isEnabled())
                .findFirst();

        if (provider.isEmpty()) {
            logger.warn("No enabled provider found for notification type: {}", type);
            return false;
        }

        NotificationProvider selectedProvider = provider.get();
        try {
            boolean success = selectedProvider.sendTestNotification(userId, message);
            if (success) {
                logger.info("Test notification sent successfully via {} to user ID: {}",
                        selectedProvider.getProviderName(), userId);
            } else {
                logger.warn("Failed to send test notification via {} to user ID: {}",
                        selectedProvider.getProviderName(), userId);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error sending test notification via {} to user ID: {}",
                    selectedProvider.getProviderName(), userId, e);
            return false;
        }
    }

    /**
     * Generate and assign NTFY topic to a new user
     */
    public String generateNtfyTopicForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        String randomSuffix = generateRandomString(10);
        String topic = ntfyTopicPrefix + "-" + userId + "-" + randomSuffix;

        // Ensure uniqueness by checking database
        int attempts = 0;
        while (attempts < 10 && isTopicAlreadyUsed(topic)) {
            randomSuffix = generateRandomString(10);
            topic = ntfyTopicPrefix + "-" + userId + "-" + randomSuffix;
            attempts++;
        }

        if (attempts >= 10) {
            logger.error("Failed to generate unique NTFY topic after 10 attempts for user {}", userId);
            throw new RuntimeException("Could not generate unique NTFY topic");
        }

        logger.debug("Generated NTFY topic: {} for user: {}", topic, userId);
        return topic;
    }

    /**
     * Update user's NTFY topic (validating format and uniqueness)
     */
    public boolean updateUserNtfyTopic(Long userId, String newTopic) {
        if (userId == null || newTopic == null || newTopic.trim().isEmpty()) {
            return false;
        }

        newTopic = newTopic.trim();

        // Validate topic format: must start with prefix
        if (!newTopic.startsWith(ntfyTopicPrefix + "-" + userId + "-")) {
            logger.warn("Invalid topic format for user {}: {}. Must start with {}-{}-",
                       userId, newTopic, ntfyTopicPrefix, userId);
            return false;
        }

        // Check uniqueness
        if (isTopicAlreadyUsed(newTopic)) {
            logger.warn("Topic {} is already in use", newTopic);
            return false;
        }

        // Update user in database
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User {} not found", userId);
            return false;
        }

        User user = userOpt.get();
        user.setNtfyTopic(newTopic);
        userRepository.save(user);

        logger.info("Updated NTFY topic for user {}: {}", userId, newTopic);
        return true;
    }

    /**
     * Get NTFY server URL for frontend configuration
     */
    public String getNtfyServerUrl() {
        return ntfyServerUrl;
    }

    /**
     * Get NTFY topic prefix for validation
     */
    public String getNtfyTopicPrefix() {
        return ntfyTopicPrefix;
    }

    /**
     * Get NTFY auth token
     */
    public String getNtfyAuthToken() {
        return ntfyAuthToken;
    }

    /**
     * Get list of enabled notification providers
     */
    public List<String> getEnabledProviders() {
        return providers.stream()
                .filter(NotificationProvider::isEnabled)
                .map(NotificationProvider::getProviderName)
                .toList();
    }

    /**
     * Check if a provider is available for a notification type
     */
    public boolean isNotificationTypeSupported(NotificationType type) {
        return providers.stream()
                .anyMatch(p -> p.supports(type) && p.isEnabled());
    }

    // Private helper methods

    /**
     * Generate random alphanumeric string
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TOPIC_CHARS.charAt(secureRandom.nextInt(TOPIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Check if NTFY topic is already used by another user
     */
    private boolean isTopicAlreadyUsed(String topic) {
        return userRepository.existsByNtfyTopic(topic);
    }
}