package com.privatecal.dto;

/**
 * Enum for notification types
 * Note: PUSH represents NTFY notifications (maintained for backward compatibility)
 */
public enum NotificationType {
    PUSH,      // NTFY push notifications
    EMAIL,     // Email notifications
    TELEGRAM   // Telegram bot notifications
}