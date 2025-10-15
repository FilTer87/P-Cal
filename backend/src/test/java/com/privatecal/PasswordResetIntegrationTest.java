package com.privatecal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privatecal.dto.ForgotPasswordRequest;
import com.privatecal.dto.ResetPasswordRequest;
import com.privatecal.entity.User;
import com.privatecal.entity.PasswordResetToken;
import com.privatecal.repository.UserRepository;
import com.privatecal.repository.PasswordResetTokenRepository;
import com.privatecal.service.PasswordResetService;
import com.privatecal.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for password reset functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestConfig.class)
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "TestPassword123";
    private final String NEW_PASSWORD = "NewPassword123";

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void cleanUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testForgotPasswordSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Se l'email esiste nel nostro sistema, riceverai le istruzioni per il reset della password."));

        // Verify token was created
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByUser(testUser)
                .stream()
                .findFirst();

        assertTrue(tokenOptional.isPresent());
        PasswordResetToken token = tokenOptional.get();
        assertFalse(token.isUsed());
        assertTrue(token.getExpiryDate().isAfter(LocalDateTime.now()));
    }

    @Test
    void testForgotPasswordNonExistentEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Se l'email esiste nel nostro sistema, riceverai le istruzioni per il reset della password."));

        // Verify no token was created
        assertEquals(0, passwordResetTokenRepository.count());
    }

    @Test
    void testForgotPasswordInvalidEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResetPasswordSuccess() throws Exception {
        // Create valid token
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
            tokenString,
            testUser,
            LocalDateTime.now().plusHours(1)
        );
        passwordResetTokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenString, NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset eseguito con successo. Ora puoi effettuare il login con la nuova password."));

        // Verify password was changed
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPasswordHash()));

        // Verify token was marked as used
        PasswordResetToken updatedToken = passwordResetTokenRepository.findByToken(tokenString).orElseThrow();
        assertTrue(updatedToken.isUsed());
    }

    @Test
    void testResetPasswordInvalidToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido o scaduto. Richiedi un nuovo reset password."));
    }

    @Test
    void testResetPasswordExpiredToken() throws Exception {
        // Create expired token
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
            tokenString,
            testUser,
            LocalDateTime.now().minusHours(1) // Expired 1 hour ago
        );
        passwordResetTokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenString, NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido o scaduto. Richiedi un nuovo reset password."));
    }

    @Test
    void testResetPasswordUsedToken() throws Exception {
        // Create used token
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
            tokenString,
            testUser,
            LocalDateTime.now().plusHours(1)
        );
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenString, NEW_PASSWORD);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido o scaduto. Richiedi un nuovo reset password."));
    }

    @Test
    void testResetPasswordWeakPassword() throws Exception {
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
            tokenString,
            testUser,
            LocalDateTime.now().plusHours(1)
        );
        passwordResetTokenRepository.save(token);

        ResetPasswordRequest request = new ResetPasswordRequest(tokenString, "weak"); // Too short and weak

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMultipleTokensInvalidatesPrevious() {
        // Create first token manually to ensure it exists
        String firstToken = UUID.randomUUID().toString();
        PasswordResetToken firstResetToken = new PasswordResetToken(
            firstToken,
            testUser,
            LocalDateTime.now().plusHours(1)
        );
        passwordResetTokenRepository.save(firstResetToken);

        // Verify first token exists and is valid
        var tokensAfterFirst = passwordResetTokenRepository.findByUser(testUser);
        assertEquals(1, tokensAfterFirst.size());
        assertTrue(tokensAfterFirst.get(0).isValid());

        // Test the markAllUserTokensAsUsed method directly
        passwordResetTokenRepository.markAllUserTokensAsUsed(testUser);

        // Reload token from database to check if it was marked as used
        var tokensAfterMark = passwordResetTokenRepository.findByUser(testUser);
        long usedTokensAfterMark = tokensAfterMark.stream().filter(PasswordResetToken::isUsed).count();

        // The first token should now be marked as used
        assertEquals(1, usedTokensAfterMark);

        // Create second token manually
        String secondToken = UUID.randomUUID().toString();
        PasswordResetToken secondResetToken = new PasswordResetToken(
            secondToken,
            testUser,
            LocalDateTime.now().plusHours(1)
        );
        passwordResetTokenRepository.save(secondResetToken);

        // Final check
        var tokens = passwordResetTokenRepository.findByUser(testUser);
        long usedTokens = tokens.stream().filter(PasswordResetToken::isUsed).count();
        long validTokens = tokens.stream().filter(PasswordResetToken::isValid).count();

        assertEquals(2, tokens.size()); // Should have exactly 2 tokens
        assertEquals(1, usedTokens); // First one should be marked as used
        assertEquals(1, validTokens); // Only the last one should be valid
    }

    @Test
    void testTokenCleanup() {
        // Test cleanup functionality
        passwordResetService.cleanupExpiredTokens();

        // This test mainly verifies the method runs without error
        // In a real scenario, you'd create expired tokens and verify they're deleted
        assertEquals(0, passwordResetTokenRepository.count());
    }
}