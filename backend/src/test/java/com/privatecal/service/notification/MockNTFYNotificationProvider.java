package com.privatecal.service.notification;

import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock NTFY notification provider for testing
 * Simulates successful notifications without making real HTTP calls
 */
@Component
@Profile("test")
public class MockNTFYNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockNTFYNotificationProvider.class);

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        logger.info("Mock NTFY: Simulating reminder notification for task '{}' to user ID {}",
                reminder.getTask().getTitle(),
                reminder.getTask().getUser().getId());

        // Simulate successful sending
        return true;
    }

    @Override
    public boolean sendTestNotification(Long userId, String message) {
        logger.info("Mock NTFY: Simulating test notification to user ID {} with message: '{}'",
                userId, message);

        // Simulate successful sending
        return true;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }

    @Override
    public boolean isEnabled() {
        return true; // Always enabled in test environment
    }

    @Override
    public String getProviderName() {
        return "Mock-NTFY";
    }
}