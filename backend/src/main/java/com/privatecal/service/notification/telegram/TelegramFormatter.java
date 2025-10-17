package com.privatecal.service.notification.telegram;

import com.privatecal.dto.NotificationMessage;
import com.privatecal.service.notification.NotificationFormatter;

import org.springframework.stereotype.Component;

/**
 * Formatter for Telegram Bot notifications
 *
 * Telegram supports:
 * - HTML and Markdown formatting in messages
 * - Emoji in messages
 * - Inline keyboard buttons
 * - Parse modes: HTML, Markdown, MarkdownV2
 */
@Component
public class TelegramFormatter implements NotificationFormatter {

    @Override
    public String formatTitle(NotificationMessage message) {
        // Telegram doesn't have separate title field, we'll include it in the body
        // Return null as Telegram combines title and body
        return null;
    }

    @Override
    public String formatBody(NotificationMessage message) {
        // Combine title and body for Telegram
        StringBuilder formattedMessage = new StringBuilder();

        // Add icon emoji if present
        if (message.getIconEmoji() != null && !message.getIconEmoji().isEmpty()) {
            formattedMessage.append(message.getIconEmoji()).append(" ");
        }

        // Add title in bold
        if (message.getTitle() != null && !message.getTitle().isEmpty()) {
            formattedMessage.append("<b>").append(escapeHtml(message.getTitle())).append("</b>\n\n");
        }

        // Add body
        if (message.getBody() != null && !message.getBody().isEmpty()) {
            formattedMessage.append(formatBodyWithHtml(message.getBody()));
        }

        return formattedMessage.toString();
    }

    @Override
    public String formatActions(NotificationMessage message) {
        if (!message.hasActions()) {
            return null;
        }

        // Telegram inline keyboard format will be handled by the provider
        // Return JSON representation of inline keyboard
        StringBuilder keyboard = new StringBuilder();
        keyboard.append("{\"inline_keyboard\":[[");

        boolean first = true;
        for (NotificationMessage.NotificationAction action : message.getActions()) {
            if (!first) {
                keyboard.append(",");
            }
            keyboard.append(String.format(
                "{\"text\":\"%s\",\"url\":\"%s\"}",
                escapeJson(action.getLabel()),
                escapeJson(action.getUrl())
            ));
            first = false;
        }

        keyboard.append("]]}");
        return keyboard.toString();
    }

    @Override
    public String formatPriority(NotificationMessage.NotificationPriority priority) {
        // Telegram doesn't have built-in priority system
        // We can add priority emoji to the message
        return switch (priority) {
            case MIN -> "";
            case LOW -> "🔵 ";
            case DEFAULT -> "";
            case HIGH -> "🟡 ";
            case URGENT -> "🔴 ";
        };
    }

    @Override
    public boolean supportsFormat(NotificationMessage.FormatType formatType) {
        // Telegram supports HTML and Markdown
        return formatType == NotificationMessage.FormatType.HTML ||
               formatType == NotificationMessage.FormatType.MARKDOWN ||
               formatType == NotificationMessage.FormatType.PLAIN_TEXT;
    }

    /**
     * Get parse mode for Telegram API
     * Returns "HTML" for HTML format, "Markdown" for Markdown, or null for plain text
     */
    public String getParseMode(NotificationMessage message) {
        return switch (message.getFormatType()) {
            case HTML -> "HTML";
            case MARKDOWN -> "Markdown";
            case PLAIN_TEXT -> null;
        };
    }

    /**
     * Format body text with HTML tags for Telegram
     * Telegram supports: <b>, <i>, <u>, <s>, <code>, <pre>, <a href="">
     */
    private String formatBodyWithHtml(String body) {
        // Body is already formatted by NotificationMessageBuilder
        // Just escape HTML special characters if needed
        return escapeHtml(body);
    }

    /**
     * Escape HTML special characters for Telegram
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    /**
     * Escape JSON special characters
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
