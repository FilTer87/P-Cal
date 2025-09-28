package com.privatecal.service.notification;

import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock Email notification provider for testing
 * Simulates successful email notifications without sending real emails
 */
@Component
@Profile("test")
public class MockEmailNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailNotificationProvider.class);

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        logger.info("Mock Email: Simulating reminder email for task '{}' to user {} ({})",
                reminder.getTask().getTitle(),
                reminder.getTask().getUser().getEmail(),
                reminder.getTask().getUser().getId());

        // Simulate successful sending
        return true;
    }

    @Override
    public boolean sendTestNotification(Long userId, String message) {
        logger.info("Mock Email: Simulating test email to user ID {} with message: '{}'",
                userId, message);

        // Simulate successful sending
        return true;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return true; // Always enabled in test environment
    }

    @Override
    public String getProviderName() {
        return "Mock-Email";
    }
}