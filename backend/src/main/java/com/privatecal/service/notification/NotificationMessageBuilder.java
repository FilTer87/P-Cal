package com.privatecal.service.notification;

import com.privatecal.dto.NotificationMessage;
import com.privatecal.entity.User;
import com.privatecal.util.TimezoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service for building localized notification messages
 * Supports all notification providers (NTFY, Telegram, Slack, etc.)
 */
@Service
@RequiredArgsConstructor
public class NotificationMessageBuilder {

    private final MessageSource notificationMessageSource;

    @Value("${app.email.base-url:http://localhost:3000}")
    private String appBaseUrl;

    /**
     * Build task reminder notification message
     */
    public NotificationMessage buildTaskReminder(User user, NotificationData data) {
        Locale locale = getUserLocale(user);

        return NotificationMessage.builder()
            .title(getMessage("notification.reminder.title", locale))
            .body(buildReminderBody(locale, data))
            .priority(NotificationMessage.NotificationPriority.DEFAULT)
            .iconEmoji("⏰")
            .formatType(NotificationMessage.FormatType.PLAIN_TEXT) // Can be changed to MARKDOWN if needed
            .actions(buildReminderActions(locale, data))
            .build();
    }

    /**
     * Build test notification message
     */
    public NotificationMessage buildTestNotification(User user, String customMessage) {
        Locale locale = getUserLocale(user);

        String message = customMessage != null && !customMessage.trim().isEmpty()
            ? customMessage
            : getMessage("notification.test.message", locale);

        return NotificationMessage.builder()
            .title(getMessage("notification.test.title", locale))
            .body(message)
            .priority(NotificationMessage.NotificationPriority.DEFAULT)
            .iconEmoji("🧪")
            .formatType(NotificationMessage.FormatType.PLAIN_TEXT)
            .build();
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    /**
     * Build reminder message body with localized text
     */
    private String buildReminderBody(Locale locale, NotificationData data) {
        StringBuilder body = new StringBuilder();

        // Task title prefix
        body.append(getMessage("notification.reminder.prefix", locale))
            .append(" ").append(data.getTaskTitle());

        // Start time
        if (data.getTaskStartTime() != null) {
            String startTime = TimezoneUtils.formatInstantInTimezone(
                data.getTaskStartTime(),
                data.getUserTimezone()
            );
            body.append("\n\n")
                .append(getMessage("notification.reminder.scheduledFor", locale))
                .append(" ").append(startTime);
        }

        // Location
        if (data.getTaskLocation() != null && !data.getTaskLocation().trim().isEmpty()) {
            body.append("\n")
                .append(getMessage("notification.reminder.location", locale))
                .append(" ").append(data.getTaskLocation());
        }

        // Description (truncated if too long)
        if (data.getTaskDescription() != null && !data.getTaskDescription().trim().isEmpty()) {
            String description = data.getTaskDescription();
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            body.append("\n")
                .append(getMessage("notification.reminder.description", locale))
                .append(" ").append(description);
        }

        // Time until start
        body.append("\n\n").append(formatTimeUntilStart(locale, data));

        return body.toString();
    }

    /**
     * Format time until task starts with localized text
     */
    private String formatTimeUntilStart(Locale locale, NotificationData data) {
        long minutesUntil = data.getMinutesUntilTaskStart();

        if (minutesUntil > 0) {
            String timeFormatted = data.getFormattedTimeUntilStart();
            return getMessage("notification.reminder.startsIn", locale) + " " + timeFormatted;
        } else if (minutesUntil == 0) {
            return getMessage("notification.reminder.startingNow", locale);
        } else {
            return getMessage("notification.reminder.alreadyStarted", locale);
        }
    }

    /**
     * Build action buttons for reminder notification
     */
    private List<NotificationMessage.NotificationAction> buildReminderActions(Locale locale, NotificationData data) {
        List<NotificationMessage.NotificationAction> actions = new ArrayList<>();

        // Add "View Task" action if task ID is available
        if (data.getTaskId() != null) {
            actions.add(NotificationMessage.NotificationAction.builder()
                .id("view_task")
                .label(getMessage("notification.action.viewTask", locale))
                .url(appBaseUrl + "/tasks/" + data.getTaskId())
                .type("view")
                .build());
        }

        return actions;
    }

    /**
     * Get user locale from User entity, fallback to en-US
     */
    private Locale getUserLocale(User user) {
        if (user != null && user.getLocale() != null && !user.getLocale().trim().isEmpty()) {
            return Locale.forLanguageTag(user.getLocale());
        }
        return Locale.forLanguageTag("en-US");
    }

    /**
     * Get localized message from properties file
     */
    private String getMessage(String key, Locale locale, Object... params) {
        return notificationMessageSource.getMessage(key, params, locale);
    }
}
