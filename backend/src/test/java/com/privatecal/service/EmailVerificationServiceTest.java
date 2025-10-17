package com.privatecal.service;

import com.privatecal.dto.PasswordResetResponse;
import com.privatecal.entity.User;
import com.privatecal.entity.EmailVerificationToken;
import com.privatecal.repository.UserRepository;
import com.privatecal.repository.EmailVerificationTokenRepository;
import com.privatecal.config.EmailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for email verification functionality in EmailVerificationService
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateBuilder templateBuilder;

    @Mock
    private EmailConfig emailConfig;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail(TEST_EMAIL);
        testUser.setEmailVerified(false);
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Configure mocks (lenient to avoid unnecessary stubbing warnings)
        lenient().when(emailConfig.getBaseUrl()).thenReturn("http://localhost:3000");
        lenient().when(templateBuilder.getEmailVerificationSubject(any(User.class)))
            .thenReturn("Email Verification - P-Cal");
        lenient().when(templateBuilder.buildEmailVerificationEmail(any(User.class), anyString()))
            .thenReturn("<html>Email Verification</html>");
    }

    @Test
    void testSendVerificationEmail_Success() {
        // Given
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        boolean result = emailVerificationService.sendVerificationEmail(testUser);

        // Then
        assertTrue(result);
        verify(emailVerificationTokenRepository).markAllUserTokensAsUsed(testUser);
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), anyString());
    }

    @Test
    void testSendVerificationEmail_EmailFailure() {
        // Given
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        // When
        boolean result = emailVerificationService.sendVerificationEmail(testUser);

        // Then
        assertFalse(result);
        verify(emailVerificationTokenRepository).markAllUserTokensAsUsed(testUser);
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), anyString());
    }

    @Test
    void testVerifyEmail_ValidToken() {
        // Given
        String tokenString = "valid-token";
        EmailVerificationToken token = new EmailVerificationToken(
            tokenString,
            testUser,
            LocalDateTime.now().plusHours(48)
        );

        when(emailVerificationTokenRepository.findValidTokenByToken(eq(tokenString), any(LocalDateTime.class)))
            .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class))).thenReturn(token);
        when(emailService.sendWelcomeEmail(any(User.class))).thenReturn(true);

        // When
        PasswordResetResponse response = emailVerificationService.verifyEmail(tokenString);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("Email verificata con successo! Ora puoi effettuare il login.",
                    response.getMessage());

        verify(emailVerificationTokenRepository).findValidTokenByToken(eq(tokenString), any(LocalDateTime.class));
        verify(userRepository).save(testUser);
        verify(emailVerificationTokenRepository).save(token);
        verify(emailService).sendWelcomeEmail(testUser);

        assertTrue(testUser.getEmailVerified());
        assertTrue(token.isUsed());
    }

    @Test
    void testVerifyEmail_InvalidToken() {
        // Given
        String tokenString = "invalid-token";
        when(emailVerificationTokenRepository.findValidTokenByToken(eq(tokenString), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // When
        PasswordResetResponse response = emailVerificationService.verifyEmail(tokenString);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Token non valido o scaduto. Richiedi un nuovo link di verifica.",
                    response.getMessage());

        verify(emailVerificationTokenRepository).findValidTokenByToken(eq(tokenString), any(LocalDateTime.class));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any());
    }

    @Test
    void testVerifyEmail_WelcomeEmailFailure() {
        // Given - welcome email fails but verification should still succeed
        String tokenString = "valid-token";
        EmailVerificationToken token = new EmailVerificationToken(
            tokenString,
            testUser,
            LocalDateTime.now().plusHours(48)
        );

        when(emailVerificationTokenRepository.findValidTokenByToken(eq(tokenString), any(LocalDateTime.class)))
            .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class))).thenReturn(token);
        when(emailService.sendWelcomeEmail(any(User.class))).thenThrow(new RuntimeException("Email service error"));

        // When
        PasswordResetResponse response = emailVerificationService.verifyEmail(tokenString);

        // Then - should still succeed even if welcome email fails
        assertTrue(response.isSuccess());
        assertEquals("Email verificata con successo! Ora puoi effettuare il login.",
                    response.getMessage());

        verify(userRepository).save(testUser);
        verify(emailService).sendWelcomeEmail(testUser);
        assertTrue(testUser.getEmailVerified());
    }

    @Test
    void testResendVerification_Success() {
        // Given
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(emailVerificationTokenRepository.countRecentTokensByUser(eq(testUser), any(LocalDateTime.class)))
            .thenReturn(2L); // Under the rate limit of 5
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        PasswordResetResponse response = emailVerificationService.resendVerificationEmail(TEST_EMAIL);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("Se l'email esiste e non è ancora verificata, riceverai un nuovo link di verifica.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(emailVerificationTokenRepository).countRecentTokensByUser(eq(testUser), any(LocalDateTime.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), anyString());
    }

    @Test
    void testResendVerification_UserNotFound() {
        // Given
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        PasswordResetResponse response = emailVerificationService.resendVerificationEmail(TEST_EMAIL);

        // Then
        assertTrue(response.isSuccess()); // Should still return success for security
        assertEquals("Se l'email esiste e non è ancora verificata, riceverai un nuovo link di verifica.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(emailVerificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testResendVerification_AlreadyVerified() {
        // Given
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // When
        PasswordResetResponse response = emailVerificationService.resendVerificationEmail(TEST_EMAIL);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("Questa email è già stata verificata. Puoi effettuare il login.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(emailVerificationTokenRepository, never()).countRecentTokensByUser(any(), any());
        verify(emailVerificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testResendVerification_RateLimitExceeded() {
        // Given
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(emailVerificationTokenRepository.countRecentTokensByUser(eq(testUser), any(LocalDateTime.class)))
            .thenReturn(5L); // At the rate limit

        // When
        PasswordResetResponse response = emailVerificationService.resendVerificationEmail(TEST_EMAIL);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Hai richiesto troppi link di verifica. Riprova tra un'ora.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(emailVerificationTokenRepository).countRecentTokensByUser(eq(testUser), any(LocalDateTime.class));
        verify(emailVerificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testResendVerification_EmailSendFailure() {
        // Given
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(emailVerificationTokenRepository.countRecentTokensByUser(eq(testUser), any(LocalDateTime.class)))
            .thenReturn(2L);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        // When
        PasswordResetResponse response = emailVerificationService.resendVerificationEmail(TEST_EMAIL);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Si è verificato un errore durante l'invio dell'email. Riprova più tardi.",
                    response.getMessage());
    }

    @Test
    void testCleanupExpiredVerificationTokens() {
        // Given
        doNothing().when(emailVerificationTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        doNothing().when(emailVerificationTokenRepository).deleteUsedTokensOlderThan(any(LocalDateTime.class));

        // When
        emailVerificationService.cleanupExpiredVerificationTokens();

        // Then
        verify(emailVerificationTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(emailVerificationTokenRepository).deleteUsedTokensOlderThan(any(LocalDateTime.class));
    }
}
