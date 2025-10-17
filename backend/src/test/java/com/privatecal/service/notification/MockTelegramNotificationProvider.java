package com.privatecal.service.notification;

import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock Telegram notification provider for testing
 * Simulates successful notifications without making real HTTP calls
 */
@Component
@Profile("test")
public class MockTelegramNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockTelegramNotificationProvider.class);

    private String lastSentMessage;
    private String lastSentChatId;

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        String chatId = reminder.getTask().getUser().getTelegramChatId();

        logger.info("Mock Telegram: Simulating reminder notification for task '{}' to user ID {} (chatId: {})",
                reminder.getTask().getTitle(),
                reminder.getTask().getUser().getId(),
                chatId);

        // Store for verification in tests
        lastSentChatId = chatId;
        lastSentMessage = "Reminder: " + reminder.getTask().getTitle();

        // Simulate successful sending
        return chatId != null && !chatId.trim().isEmpty();
    }

    @Override
    public boolean sendTestNotification(Long userId, String message) {
        logger.info("Mock Telegram: Simulating test notification to user ID {} with message: '{}'",
                userId, message);

        // Store for verification in tests
        lastSentMessage = message;

        // Simulate successful sending
        return true;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.TELEGRAM;
    }

    @Override
    public boolean isEnabled() {
        return true; // Always enabled in test environment
    }

    @Override
    public String getProviderName() {
        return "Mock-Telegram";
    }

    // Test helper methods
    public String getLastSentMessage() {
        return lastSentMessage;
    }

    public String getLastSentChatId() {
        return lastSentChatId;
    }

    public void reset() {
        lastSentMessage = null;
        lastSentChatId = null;
    }
}
