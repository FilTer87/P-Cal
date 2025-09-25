package com.privatecal.service.notification;

import com.privatecal.entity.Reminder;
import com.privatecal.dto.NotificationType;

/**
 * Strategy interface for different notification providers
 * Allows extensibility for future notification implementations (Gotify, Slack, etc.)
 */
public interface NotificationProvider {

    /**
     * Send a notification for a task reminder
     * @param reminder The reminder containing task and timing information
     * @return true if notification was sent successfully, false otherwise
     */
    boolean sendReminderNotification(Reminder reminder);

    /**
     * Send a test notification to verify the provider is working
     * @param userId The user ID to send test notification to
     * @param message The test message content
     * @return true if test notification was sent successfully, false otherwise
     */
    boolean sendTestNotification(Long userId, String message);

    /**
     * Check if this provider supports the given notification type
     * @param type The notification type to check
     * @return true if this provider can handle the notification type
     */
    boolean supports(NotificationType type);

    /**
     * Check if the provider is enabled and properly configured
     * @return true if the provider is ready to send notifications
     */
    boolean isEnabled();

    /**
     * Get the provider name for logging and identification purposes
     * @return The provider name (e.g., "NTFY", "Email", "Gotify")
     */
    String getProviderName();
}