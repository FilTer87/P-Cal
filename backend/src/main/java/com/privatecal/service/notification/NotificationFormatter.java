package com.privatecal.service.notification;

import com.privatecal.dto.NotificationMessage;

/**
 * Interface for formatting NotificationMessage for specific providers
 * Each notification provider (NTFY, Telegram, Slack, etc.) can have its own formatter
 */
public interface NotificationFormatter {

    /**
     * Format notification title for the provider
     * @param message The notification message
     * @return Formatted title string, or null if provider doesn't support separate title
     */
    String formatTitle(NotificationMessage message);

    /**
     * Format notification body for the provider
     * @param message The notification message
     * @return Formatted body string
     */
    String formatBody(NotificationMessage message);

    /**
     * Format notification actions/buttons for the provider
     * @param message The notification message
     * @return Formatted actions string (provider-specific format), or null if no actions
     */
    String formatActions(NotificationMessage message);

    /**
     * Get priority string for the provider
     * @param priority The standard priority level
     * @return Provider-specific priority string
     */
    String formatPriority(NotificationMessage.NotificationPriority priority);

    /**
     * Check if provider supports the given format type
     * @param formatType The format type (PLAIN_TEXT, MARKDOWN, HTML)
     * @return true if supported, false otherwise
     */
    boolean supportsFormat(NotificationMessage.FormatType formatType);
}
