package com.privatecal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard notification message DTO
 * Used by all notification providers (NTFY, Telegram, Slack, etc.)
 */
@Data
@Builder
public class NotificationMessage {

    /**
     * Notification title/subject
     */
    private String title;

    /**
     * Main message body
     */
    private String body;

    /**
     * Message priority level
     */
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.DEFAULT;

    /**
     * Icon emoji (fallback for providers without image support)
     */
    private String iconEmoji;

    /**
     * Image URL (for providers with image support)
     */
    private String imageUrl;

    /**
     * Formatting type for message body
     */
    @Builder.Default
    private FormatType formatType = FormatType.PLAIN_TEXT;

    /**
     * Action buttons/links
     */
    @Builder.Default
    private List<NotificationAction> actions = new ArrayList<>();

    /**
     * Provider-specific metadata (custom headers, options, etc.)
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Notification priority levels
     * Maps to provider-specific priority systems
     */
    public enum NotificationPriority {
        MIN,      // Lowest priority, silent notification
        LOW,      // Low priority
        DEFAULT,  // Normal priority
        HIGH,     // High priority, may bypass DND on some platforms
        URGENT    // Critical, urgent notification
    }

    /**
     * Message formatting type
     * Determines how providers should render the message
     */
    public enum FormatType {
        PLAIN_TEXT,   // Plain text, no formatting
        MARKDOWN,     // Markdown formatting (NTFY, Telegram, Discord)
        HTML          // HTML formatting (some email-style notifications)
    }

    /**
     * Notification action (button, link, etc.)
     */
    @Data
    @Builder
    public static class NotificationAction {
        /**
         * Action identifier (e.g., "view_task", "complete", "snooze")
         */
        private String id;

        /**
         * Action label/button text (localized)
         */
        private String label;

        /**
         * Action URL or deep link
         */
        private String url;

        /**
         * Action type (view, http, broadcast, etc.)
         * Provider-specific interpretation
         */
        @Builder.Default
        private String type = "view";

        /**
         * Additional action metadata
         */
        @Builder.Default
        private Map<String, String> metadata = new HashMap<>();
    }

    /**
     * Add action to the notification
     */
    public void addAction(NotificationAction action) {
        this.actions.add(action);
    }

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    /**
     * Check if notification has actions
     */
    public boolean hasActions() {
        return actions != null && !actions.isEmpty();
    }

    /**
     * Check if notification has image
     */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    /**
     * Check if notification uses formatted content
     */
    public boolean isFormatted() {
        return formatType != FormatType.PLAIN_TEXT;
    }
}
