package com.privatecal.service;

import com.privatecal.dto.ForgotPasswordRequest;
import com.privatecal.dto.ResetPasswordRequest;
import com.privatecal.dto.PasswordResetResponse;
import com.privatecal.entity.User;
import com.privatecal.entity.PasswordResetToken;
import com.privatecal.repository.UserRepository;
import com.privatecal.repository.PasswordResetTokenRepository;
import com.privatecal.config.EmailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for password reset functionality in PasswordResetService
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateBuilder templateBuilder;

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "TestPassword123";
    private final String NEW_PASSWORD = "NewPassword123";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Configure EmailConfig mock (lenient to avoid unnecessary stubbing warnings)
        lenient().when(emailConfig.getBaseUrl()).thenReturn("http://localhost:3000");

        // Configure EmailTemplateBuilder mock (lenient to avoid unnecessary stubbing warnings)
        lenient().when(templateBuilder.getPasswordResetSubject(any(User.class)))
            .thenReturn("Password Reset - P-Cal");
        lenient().when(templateBuilder.buildPasswordResetEmail(any(User.class), anyString()))
            .thenReturn("<html>Password Reset Email</html>");
        lenient().when(templateBuilder.getPasswordResetConfirmationSubject(any(User.class)))
            .thenReturn("Password Reset Completed - P-Cal");
        lenient().when(templateBuilder.buildPasswordResetConfirmationEmail(any(User.class)))
            .thenReturn("<html>Password Reset Confirmation</html>");
    }

    @Test
    void testForgotPasswordSuccess() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        PasswordResetResponse response = passwordResetService.forgotPassword(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("Se l'email esiste nel nostro sistema, riceverai le istruzioni per il reset della password.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordResetTokenRepository).markAllUserTokensAsUsed(testUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), anyString());
    }

    @Test
    void testForgotPasswordUserNotFound() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        PasswordResetResponse response = passwordResetService.forgotPassword(request);

        // Then
        assertTrue(response.isSuccess()); // Should still return success for security
        assertEquals("Se l'email esiste nel nostro sistema, riceverai le istruzioni per il reset della password.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testResetPasswordSuccess() {
        // Given
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
            tokenString,
            testUser,
            LocalDateTime.now().plusHours(1)
        );

        ResetPasswordRequest request = new ResetPasswordRequest(tokenString, NEW_PASSWORD);

        when(passwordResetTokenRepository.findValidTokenByToken(eq(tokenString), any(LocalDateTime.class)))
            .thenReturn(Optional.of(token));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(token);
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        PasswordResetResponse response = passwordResetService.resetPassword(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("Password reset eseguito con successo. Ora puoi effettuare il login con la nuova password.",
                    response.getMessage());

        verify(passwordResetTokenRepository).findValidTokenByToken(eq(tokenString), any(LocalDateTime.class));
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).save(token);
        verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), anyString());

        assertTrue(token.isUsed());
    }

    @Test
    void testResetPasswordInvalidToken() {
        // Given
        String tokenString = "invalid-token";
        ResetPasswordRequest request = new ResetPasswordRequest(tokenString, NEW_PASSWORD);

        when(passwordResetTokenRepository.findValidTokenByToken(eq(tokenString), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // When
        PasswordResetResponse response = passwordResetService.resetPassword(request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Token non valido o scaduto. Richiedi un nuovo reset password.",
                    response.getMessage());

        verify(passwordResetTokenRepository).findValidTokenByToken(eq(tokenString), any(LocalDateTime.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testCleanupExpiredTokens() {
        // Given
        doNothing().when(passwordResetTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        doNothing().when(passwordResetTokenRepository).deleteUsedTokensOlderThan(any(LocalDateTime.class));

        // When
        passwordResetService.cleanupExpiredTokens();

        // Then
        verify(passwordResetTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(passwordResetTokenRepository).deleteUsedTokensOlderThan(any(LocalDateTime.class));
    }

    @Test
    void testForgotPasswordEmailFailure() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString()))
            .thenReturn(false); // Email service returns false on failure

        // When
        PasswordResetResponse response = passwordResetService.forgotPassword(request);

        // Then
        assertFalse(response.isSuccess()); // Should return error response instead of throwing
        assertEquals("Si è verificato un errore durante l'elaborazione della richiesta. Riprova più tardi.",
                    response.getMessage());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordResetTokenRepository).markAllUserTokensAsUsed(testUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), anyString());
    }
}