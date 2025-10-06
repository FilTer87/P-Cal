package com.privatecal.service.notification;

import com.privatecal.dto.NotificationMessage;
import org.springframework.stereotype.Component;

/**
 * Formatter for NTFY notifications
 *
 * NTFY supports:
 * - Plain text and Markdown in body (with X-Markdown header)
 * - Emoji in title and body
 * - Action buttons with specific format: "action=view, label=..., url=..."
 * - Priority levels: min, low, default, high, urgent
 */
@Component
public class NtfyFormatter implements NotificationFormatter {

    @Override
    public String formatTitle(NotificationMessage message) {
        // NTFY supports plain text in title, we can add emoji icon
        if (message.getIconEmoji() != null && !message.getIconEmoji().isEmpty()) {
            return message.getIconEmoji() + " " + message.getTitle();
        }
        return message.getTitle();
    }

    @Override
    public String formatBody(NotificationMessage message) {
        // NTFY supports both plain text and Markdown
        // Body is already formatted by NotificationMessageBuilder
        return message.getBody();
    }

    @Override
    public String formatActions(NotificationMessage message) {
        if (!message.hasActions()) {
            return null;
        }

        // NTFY action format: "action=view, label=View Task, url=https://..."
        // Currently we only support the first action
        NotificationMessage.NotificationAction action = message.getActions().get(0);

        return String.format("action=%s, label=%s, url=%s",
            action.getType(),
            action.getLabel(),
            action.getUrl()
        );
    }

    @Override
    public String formatPriority(NotificationMessage.NotificationPriority priority) {
        // Map standard priority to NTFY priority levels
        return switch (priority) {
            case MIN -> "min";
            case LOW -> "low";
            case DEFAULT -> "default";
            case HIGH -> "high";
            case URGENT -> "urgent";
        };
    }

    @Override
    public boolean supportsFormat(NotificationMessage.FormatType formatType) {
        // NTFY supports both plain text and Markdown
        return formatType == NotificationMessage.FormatType.PLAIN_TEXT ||
               formatType == NotificationMessage.FormatType.MARKDOWN;
    }

    /**
     * Check if Markdown should be enabled for this message
     * Returns true if message uses MARKDOWN format type
     */
    public boolean shouldEnableMarkdown(NotificationMessage message) {
        return message.getFormatType() == NotificationMessage.FormatType.MARKDOWN;
    }
}
