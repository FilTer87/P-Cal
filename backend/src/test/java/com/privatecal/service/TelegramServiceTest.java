package com.privatecal.service;

import com.privatecal.entity.TelegramRegistrationToken;
import com.privatecal.entity.User;
import com.privatecal.repository.TelegramRegistrationTokenRepository;
import com.privatecal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for TelegramService
 */
@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TelegramRegistrationTokenRepository tokenRepository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private TelegramService telegramService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        telegramService = new TelegramService(userRepository, tokenRepository, restTemplateBuilder, objectMapper);

        // Set @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(telegramService, "telegramEnabled", true);
        ReflectionTestUtils.setField(telegramService, "botToken", "test-bot-token");
        ReflectionTestUtils.setField(telegramService, "apiUrl", "https://api.telegram.org");
    }

    @Test
    void testGenerateRegistrationToken_Success() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "it-IT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(TelegramRegistrationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String token = telegramService.generateRegistrationToken(userId);

        // Then
        assertNotNull(token);
        assertTrue(token.startsWith("pcal_"), "Token should have pcal_ prefix");
        assertEquals(21, token.length(), "Token should be 21 chars (pcal_ + 16 random)");
        verify(tokenRepository).save(any(TelegramRegistrationToken.class));
    }

    @Test
    void testGenerateRegistrationToken_ReuseValidToken() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "en-US");
        String existingToken = "pcal_existingtoken123";
        TelegramRegistrationToken token = new TelegramRegistrationToken(
                user, existingToken, Instant.now().plus(5, ChronoUnit.MINUTES));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        // When
        String result = telegramService.generateRegistrationToken(userId);

        // Then
        assertEquals(existingToken, result, "Should reuse existing valid token");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testGenerateRegistrationToken_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            telegramService.generateRegistrationToken(userId);
        }, "Should throw exception when user not found");
    }

    @Test
    void testIsUserRegistered_WithChatId() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "es-ES");
        user.setTelegramChatId("123456789");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean result = telegramService.isUserRegistered(userId);

        // Then
        assertTrue(result, "User with chat ID should be registered");
    }

    @Test
    void testIsUserRegistered_WithoutChatId() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "it-IT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean result = telegramService.isUserRegistered(userId);

        // Then
        assertFalse(result, "User without chat ID should not be registered");
    }

    @Test
    void testIsUserRegistered_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        boolean result = telegramService.isUserRegistered(userId);

        // Then
        assertFalse(result, "Non-existent user should not be registered");
    }

    @Test
    void testUnregisterUser_Success() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "en-US");
        user.setTelegramChatId("123456789");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        boolean result = telegramService.unregisterUser(userId);

        // Then
        assertTrue(result, "Should successfully unregister user");
        assertNull(user.getTelegramChatId(), "Chat ID should be cleared");
        verify(userRepository).save(user);
    }

    @Test
    void testUnregisterUser_NotRegistered() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, "testuser", "it-IT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean result = telegramService.unregisterUser(userId);

        // Then
        assertFalse(result, "Should return false when user not registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testProcessStartCommand_ValidToken() {
        // Given
        String chatId = "123456789";
        String token = "pcal_validtoken123";
        User user = createTestUser(1L, "testuser", "it-IT");

        TelegramRegistrationToken registrationToken = new TelegramRegistrationToken(
                user, token, Instant.now().plus(5, ChronoUnit.MINUTES));

        when(tokenRepository.findValidToken(eq(token), any(Instant.class)))
                .thenReturn(Optional.of(registrationToken));
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(TelegramRegistrationToken.class))).thenReturn(registrationToken);

        // Mock RestTemplate for sendMessage call
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(null);

        // When
        boolean result = telegramService.processStartCommand(chatId, token);

        // Then
        assertTrue(result, "Should successfully process start command");
        assertEquals(chatId, user.getTelegramChatId(), "User should have chat ID set");
        assertTrue(registrationToken.getUsed(), "Token should be marked as used");
        verify(userRepository).save(user);
        verify(tokenRepository).save(registrationToken);
    }

    @Test
    void testProcessStartCommand_InvalidToken() {
        // Given
        String chatId = "123456789";
        String token = "pcal_invalidtoken";

        lenient().when(tokenRepository.findValidToken(eq(token), any(Instant.class)))
                .thenReturn(Optional.empty());

        // Mock RestTemplate for sendMessage call (error message) - lenient as it may not be called if exception occurs
        lenient().when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(null);

        // When
        boolean result = telegramService.processStartCommand(chatId, token);

        // Then
        assertFalse(result, "Should fail with invalid token");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testProcessStartCommand_ChatIdAlreadyLinked() {
        // Given
        String chatId = "123456789";
        String token = "pcal_validtoken123";
        User existingUser = createTestUser(1L, "existinguser", "en-US");
        existingUser.setTelegramChatId(chatId);

        User newUser = createTestUser(2L, "newuser", "es-ES");

        TelegramRegistrationToken registrationToken = new TelegramRegistrationToken(
                newUser, token, Instant.now().plus(5, ChronoUnit.MINUTES));

        lenient().when(tokenRepository.findValidToken(eq(token), any(Instant.class)))
                .thenReturn(Optional.of(registrationToken));
        lenient().when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(existingUser));

        // Mock RestTemplate for sendMessage call (error message) - lenient as it may not be called if exception occurs
        lenient().when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(null);

        // When
        boolean result = telegramService.processStartCommand(chatId, token);

        // Then
        assertFalse(result, "Should fail when chat ID already linked to another user");
        verify(userRepository, never()).save(any());
    }

    // Helper method to create test user with locale
    private User createTestUser(Long id, String username, String locale) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        // Valid BCrypt hash (60 characters minimum)
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01");
        user.setLocale(locale);
        return user;
    }
}
