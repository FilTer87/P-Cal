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

import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
import com.privatecal.util.TimezoneUtils;

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

    @Value("${app.email.base-url:http://localhost:3000}")
    private String appBaseURl;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    public NTFYNotificationProvider(RestTemplateBuilder restTemplateBuilder, UserRepository userRepository) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.userRepository = userRepository;
    }

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        if (!isEnabled()) {
            logger.debug("NTFY provider is disabled");
            return false;
        }

        try {
            NotificationData data = NotificationData.fromReminder(reminder);

            if (data.getUserNtfyTopic() == null || data.getUserNtfyTopic().trim().isEmpty()) {
                logger.warn("User {} has no NTFY topic configured", data.getUserId());
                return false;
            }

            String ntfyUrl = ntfyServerUrl + "/" + data.getUserNtfyTopic();

            // Create notification content
            String title = "📅 Task Reminder";
            String message = createReminderMessage(data);
            String priority = determinePriority(data);

            // Create NTFY payload using text/plain format (more reliable)
            HttpHeaders headers = createNtfyHeaders();
            headers.set("X-Title", title);
            headers.set("X-Priority", priority);
            headers.set("X-Tags", "calendar,reminder,⏰");

            // Add action buttons for better UX
            if (data.getTaskId() != null) {
                headers.set("X-Actions", createActionButtons(data));
            }

            // Create request entity with message as body
            HttpEntity<String> requestEntity = new HttpEntity<>(message, headers);

            // Send notification
            ResponseEntity<String> response = restTemplate.exchange(
                    ntfyUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("NTFY reminder notification sent successfully for task: {} to user: {}",
                        data.getTaskTitle(), data.getUserId());
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
    public boolean sendTestNotification(Long userId, String message) {
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

            // Create test notification content
            String title = "🧪 P-Cal Test Notification";
            String testMessage = message != null ? message :
                "This is a test notification from P-Cal. If you receive this, your notification setup is working correctly! 🎉";

            // Create NTFY payload using text/plain format
            HttpHeaders headers = createNtfyHeaders();
            headers.set("X-Title", title);
            headers.set("X-Priority", "default");
            headers.set("X-Tags", "test,calendar,✅");

            // Create request entity with message as body
            HttpEntity<String> requestEntity = new HttpEntity<>(testMessage, headers);

            // Send test notification
            ResponseEntity<String> response = restTemplate.exchange(
                    ntfyUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("NTFY test notification sent successfully to user: {}", userId);
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

    /**
     * Create formatted reminder message
     */
    private String createReminderMessage(NotificationData data) {
        StringBuilder message = new StringBuilder();
        message.append("Reminder: ").append(data.getTaskTitle());

        if (data.getTaskStartTime() != null) {
            // Format time in user's timezone
            String startTime = TimezoneUtils.formatInstantInTimezone(
                data.getTaskStartTime(),
                data.getUserTimezone()
            );
            message.append("\n\n📅 Scheduled for: ").append(startTime);
        }

        if (data.getTaskLocation() != null && !data.getTaskLocation().trim().isEmpty()) {
            message.append("\n📍 Location: ").append(data.getTaskLocation());
        }

        if (data.getTaskDescription() != null && !data.getTaskDescription().trim().isEmpty()) {
            String description = data.getTaskDescription();
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            message.append("\n💭 Description: ").append(description);
        }

        long minutesUntil = data.getMinutesUntilTaskStart();
        if (minutesUntil > 0) {
            message.append("\n\n⏰ Starts in ").append(data.getFormattedTimeUntilStart());
        } else if (minutesUntil == 0) {
            message.append("\n\n🚀 Starting now!");
        } else {
            message.append("\n\n⚠️ Task has already started");
        }

        return message.toString();
    }

    /**
     * Determine notification priority based on urgency
     */
    private String determinePriority(NotificationData data) {
        long minutesUntil = data.getMinutesUntilTaskStart();

        if (minutesUntil <= 0) {
            return "urgent"; // Task is starting now or overdue
        } else if (minutesUntil <= 5) {
            return "high"; // Very soon
        } else if (minutesUntil <= 15) {
            return "default"; // Soon
        } else if (minutesUntil <= 60) {
            return "low"; // Within an hour
        } else {
            return "min"; // More than an hour
        }
    }

    /**
     * Create action buttons for the notification
     */
    private String createActionButtons(NotificationData data) {
        // Format: action=view, label=View Task, url=https://example.com/tasks/123
        return String.format("action=view, label=View Task, url=%s/tasks/%d",
                appBaseURl, data.getTaskId());
    }
}