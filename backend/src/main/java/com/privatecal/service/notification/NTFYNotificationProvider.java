package com.privatecal.service.notification;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.privatecal.dto.NotificationMessage;
import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;

/**
 * NTFY notification provider implementation
 * Handles sending push notifications via NTFY server
 */
@Component
public class NTFYNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(NTFYNotificationProvider.class);

    @Value("${app.ntfy.server-url}")
    private String ntfyServerUrl;

    @Value("${app.ntfy.enabled:true}")
    private boolean ntfyEnabled;

    @Value("${app.ntfy.auth-token:}")
    private String ntfyAuthToken;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final NtfyFormatter ntfyFormatter;

    public NTFYNotificationProvider(RestTemplateBuilder restTemplateBuilder,
                                    UserRepository userRepository,
                                    NotificationMessageBuilder messageBuilder,
                                    NtfyFormatter ntfyFormatter) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.userRepository = userRepository;
        this.messageBuilder = messageBuilder;
        this.ntfyFormatter = ntfyFormatter;
    }

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        if (!isEnabled()) {
            logger.debug("NTFY provider is disabled");
            return false;
        }

        try {
            User user = reminder.getTask().getUser();
            NotificationData data = NotificationData.fromReminder(reminder);

            if (data.getUserNtfyTopic() == null || data.getUserNtfyTopic().trim().isEmpty()) {
                logger.warn("User {} has no NTFY topic configured", data.getUserId());
                return false;
            }

            String ntfyUrl = ntfyServerUrl + "/" + data.getUserNtfyTopic();

            // Build localized notification message
            NotificationMessage message = messageBuilder.buildTaskReminder(user, data);

            // Format for NTFY
            String title = ntfyFormatter.formatTitle(message);
            String body = ntfyFormatter.formatBody(message);
            String priority = ntfyFormatter.formatPriority(message.getPriority());

            // Create NTFY headers
            HttpHeaders headers = createNtfyHeaders();
            headers.set("X-Title", title);
            headers.set("X-Priority", priority);
            headers.set("X-Tags", "calendar,reminder," + message.getIconEmoji());

            // Enable Markdown if message uses it
            if (ntfyFormatter.shouldEnableMarkdown(message)) {
                headers.set("X-Markdown", "true");
            }

            // Add action buttons
            String actions = ntfyFormatter.formatActions(message);
            if (actions != null) {
                headers.set("X-Actions", actions);
            }

            // Create request entity with message body
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // Send notification
            ResponseEntity<String> response = restTemplate.exchange(
                    ntfyUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("NTFY reminder notification sent successfully for task: {} to user: {} (locale: {})",
                        data.getTaskTitle(), data.getUserId(), user.getLocale());
                return true;
            } else {
                logger.warn("Failed to send NTFY notification. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            logger.error("Network error sending NTFY notification for reminder ID: {}",
                    reminder.getId(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending NTFY notification for reminder ID: {}",
                    reminder.getId(), e);
            return false;
        }
    }

    @Override
    public boolean sendTestNotification(Long userId, String customMessage) {
        if (!isEnabled()) {
            logger.debug("NTFY provider is disabled - cannot send test notification");
            return false;
        }

        try {
            // Get user's NTFY topic from database
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User with ID {} not found for test notification", userId);
                return false;
            }

            User user = userOpt.get();
            if (user.getNtfyTopic() == null || user.getNtfyTopic().trim().isEmpty()) {
                logger.warn("User {} has no NTFY topic configured for test notification", userId);
                return false;
            }

            String ntfyUrl = ntfyServerUrl + "/" + user.getNtfyTopic();

            // Build localized test notification
            NotificationMessage message = messageBuilder.buildTestNotification(user, customMessage);

            // Format for NTFY
            String title = ntfyFormatter.formatTitle(message);
            String body = ntfyFormatter.formatBody(message);
            String priority = ntfyFormatter.formatPriority(message.getPriority());

            // Create NTFY headers
            HttpHeaders headers = createNtfyHeaders();
            headers.set("X-Title", title);
            headers.set("X-Priority", priority);
            headers.set("X-Tags", "test,calendar," + message.getIconEmoji());

            // Create request entity with message body
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // Send test notification
            ResponseEntity<String> response = restTemplate.exchange(
                    ntfyUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("NTFY test notification sent successfully to user: {} (locale: {})",
                        userId, user.getLocale());
                return true;
            } else {
                logger.warn("Failed to send NTFY test notification. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            logger.error("Network error sending NTFY test notification to user ID: {}", userId, e);
            return false;
        } catch (Exception e) {
            logger.error("Error sending NTFY test notification to user ID: {}", userId, e);
            return false;
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return NotificationType.PUSH.equals(type);
    }

    @Override
    public boolean isEnabled() {
        boolean basicConfig = ntfyEnabled && ntfyServerUrl != null && !ntfyServerUrl.trim().isEmpty();

        if (!basicConfig) {
            return false;
        }

        // Log authentication status for debugging
        if (ntfyAuthToken != null && !ntfyAuthToken.trim().isEmpty()) {
            logger.debug("NTFY provider enabled with authentication");
        } else {
            logger.debug("NTFY provider enabled without authentication (public server)");
        }

        return true;
    }

    @Override
    public String getProviderName() {
        return "NTFY";
    }

    /**
     * Create HTTP headers with authentication for NTFY requests
     */
    private HttpHeaders createNtfyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        // Add authentication token if configured
        if (ntfyAuthToken != null && !ntfyAuthToken.trim().isEmpty()) {
            headers.set("Authorization", "Bearer " + ntfyAuthToken.trim());
            logger.debug("Adding NTFY authentication header");
        }

        return headers;
    }
}