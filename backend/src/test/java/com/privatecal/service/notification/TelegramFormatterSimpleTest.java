package com.privatecal.service.notification;

import com.privatecal.dto.NotificationMessage;
import com.privatecal.service.notification.telegram.TelegramFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified test for TelegramFormatter focusing on key functionality
 */
class TelegramFormatterSimpleTest {

    private TelegramFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new TelegramFormatter();
    }

    @Test
    void testFormatTitle_ReturnsNull() {
        NotificationMessage message = NotificationMessage.builder()
                .title("Test Title")
                .build();

        assertNull(formatter.formatTitle(message));
    }

    @Test
    void testFormatBody_BasicMessage() {
        NotificationMessage message = NotificationMessage.builder()
                .title("Calendar Reminder")
                .body("Your meeting starts in 15 minutes")
                .iconEmoji("📅")
                .build();

        String result = formatter.formatBody(message);

        assertNotNull(result);
        assertTrue(result.contains("📅"));
        assertTrue(result.contains("Calendar Reminder"));
        assertTrue(result.contains("Your meeting starts in 15 minutes"));
    }

    @Test
    void testFormatBody_ItalianLocale() {
        NotificationMessage message = NotificationMessage.builder()
                .title("Promemoria Calendario")
                .body("Il tuo meeting inizia tra 15 minuti")
                .iconEmoji("📅")
                .build();

        String result = formatter.formatBody(message);

        assertTrue(result.contains("Promemoria Calendario"));
        assertTrue(result.contains("Il tuo meeting inizia tra 15 minuti"));
    }

    @Test
    void testFormatBody_SpanishLocale() {
        NotificationMessage message = NotificationMessage.builder()
                .title("Recordatorio del Calendario")
                .body("Tu reunión comienza en 15 minutos")
                .iconEmoji("📅")
                .build();

        String result = formatter.formatBody(message);

        assertTrue(result.contains("Recordatorio del Calendario"));
        assertTrue(result.contains("Tu reunión comienza en 15 minutos"));
    }

    @Test
    void testFormatActions_NoActions() {
        NotificationMessage message = NotificationMessage.builder()
                .title("Test")
                .build();

        assertNull(formatter.formatActions(message));
    }

    @Test
    void testSupportsAllFormats() {
        assertTrue(formatter.supportsFormat(NotificationMessage.FormatType.HTML));
        assertTrue(formatter.supportsFormat(NotificationMessage.FormatType.MARKDOWN));
        assertTrue(formatter.supportsFormat(NotificationMessage.FormatType.PLAIN_TEXT));
    }

    @Test
    void testHtmlEscaping() {
        NotificationMessage message = NotificationMessage.builder()
                .title("Test <script>alert('xss')</script>")
                .body("Body with & < > chars")
                .build();

        String result = formatter.formatBody(message);

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;") || result.contains("&amp;"));
    }
}
