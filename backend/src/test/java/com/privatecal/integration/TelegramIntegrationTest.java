package com.privatecal.integration;

import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
import com.privatecal.service.TelegramService;
import com.privatecal.service.notification.MockTelegramNotificationProvider;
import com.privatecal.service.notification.NotificationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Telegram notification system
 * Tests the full flow from user registration to notification sending with localization
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TelegramIntegrationTest {

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private List<NotificationProvider> providers;

    @Test
    void testTelegramProviderIsRegistered() {
        // Verify that Mock Telegram provider is registered
        boolean hasTelegramProvider = providers.stream()
                .anyMatch(p -> p.supports(NotificationType.TELEGRAM));

        assertTrue(hasTelegramProvider, "Telegram provider should be registered");

        NotificationProvider telegramProvider = providers.stream()
                .filter(p -> p.supports(NotificationType.TELEGRAM))
                .findFirst()
                .orElse(null);

        assertNotNull(telegramProvider);
        assertEquals("Mock-Telegram", telegramProvider.getProviderName());
        assertTrue(telegramProvider.isEnabled());
    }

    @Test
    void testFullRegistrationFlow() {
        // Given: Create a user with Italian locale
        User user = createTestUser("italian_user", "it-IT");
        user = userRepository.save(user);

        // When: Generate registration token
        String token = telegramService.generateRegistrationToken(user.getId());

        // Then: Token should be generated
        assertNotNull(token);
        assertTrue(token.startsWith("pcal_"));

        // When: Process /start command
        String chatId = "123456789";
        boolean success = telegramService.processStartCommand(chatId, token);

        // Then: Registration should succeed
        assertTrue(success);

        // Verify: User has chat ID
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(chatId, updatedUser.getTelegramChatId());
        assertTrue(telegramService.isUserRegistered(user.getId()));
    }

    @Test
    void testNotificationWithDifferentLocales() {
        // Test Italian locale
        User italianUser = createTestUser("italian_user", "it-IT");
        italianUser.setTelegramChatId("111111111");
        italianUser = userRepository.save(italianUser);

        Task italianTask = createTestTask(italianUser, "Riunione Team");
        Reminder italianReminder = createTestReminder(italianTask, NotificationType.TELEGRAM);

        MockTelegramNotificationProvider mockProvider = getMockTelegramProvider();
        mockProvider.reset();

        boolean sentItalian = mockProvider.sendReminderNotification(italianReminder);
        assertTrue(sentItalian);
        assertEquals("111111111", mockProvider.getLastSentChatId());

        // Test Spanish locale
        User spanishUser = createTestUser("spanish_user", "es-ES");
        spanishUser.setTelegramChatId("222222222");
        spanishUser = userRepository.save(spanishUser);

        Task spanishTask = createTestTask(spanishUser, "Reunión Equipo");
        Reminder spanishReminder = createTestReminder(spanishTask, NotificationType.TELEGRAM);

        mockProvider.reset();

        boolean sentSpanish = mockProvider.sendReminderNotification(spanishReminder);
        assertTrue(sentSpanish);
        assertEquals("222222222", mockProvider.getLastSentChatId());

        // Test English locale
        User englishUser = createTestUser("english_user", "en-US");
        englishUser.setTelegramChatId("333333333");
        englishUser = userRepository.save(englishUser);

        Task englishTask = createTestTask(englishUser, "Team Meeting");
        Reminder englishReminder = createTestReminder(englishTask, NotificationType.TELEGRAM);

        mockProvider.reset();

        boolean sentEnglish = mockProvider.sendReminderNotification(englishReminder);
        assertTrue(sentEnglish);
        assertEquals("333333333", mockProvider.getLastSentChatId());
    }

    @Test
    void testNotificationFailsWithoutChatId() {
        // Given: User without Telegram chat ID
        User user = createTestUser("no_telegram_user", "it-IT");
        user = userRepository.save(user);

        Task task = createTestTask(user, "Test Task");
        Reminder reminder = createTestReminder(task, NotificationType.TELEGRAM);

        MockTelegramNotificationProvider mockProvider = getMockTelegramProvider();
        mockProvider.reset();

        // When: Try to send notification
        boolean sent = mockProvider.sendReminderNotification(reminder);

        // Then: Should fail
        assertFalse(sent);
        assertNull(mockProvider.getLastSentChatId());
    }

    @Test
    void testUnregisterUser() {
        // Given: Registered user
        User user = createTestUser("to_unregister", "en-US");
        user.setTelegramChatId("999999999");
        user = userRepository.save(user);

        assertTrue(telegramService.isUserRegistered(user.getId()));

        // When: Unregister
        boolean success = telegramService.unregisterUser(user.getId());

        // Then: Should be unregistered
        assertTrue(success);
        assertFalse(telegramService.isUserRegistered(user.getId()));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertNull(updatedUser.getTelegramChatId());
    }

    // Helper methods

    private User createTestUser(String username, String locale) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        // Valid BCrypt hash (60 characters minimum)
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01");
        user.setLocale(locale);
        return user;
    }

    private Task createTestTask(User user, String title) {
        Task task = new Task();
        task.setUser(user);
        task.setTitle(title);
        task.setStartDatetime(Instant.now().plus(1, ChronoUnit.HOURS));
        task.setEndDatetime(Instant.now().plus(2, ChronoUnit.HOURS));
        return task;
    }

    private Reminder createTestReminder(Task task, NotificationType type) {
        Reminder reminder = new Reminder();
        reminder.setTask(task);
        reminder.setReminderOffsetMinutes(15);
        reminder.setReminderTime(Instant.now().minus(1, ChronoUnit.MINUTES));
        reminder.setNotificationType(type);
        reminder.setIsSent(false);
        return reminder;
    }

    private MockTelegramNotificationProvider getMockTelegramProvider() {
        return (MockTelegramNotificationProvider) providers.stream()
                .filter(p -> p instanceof MockTelegramNotificationProvider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MockTelegramNotificationProvider not found"));
    }
}
