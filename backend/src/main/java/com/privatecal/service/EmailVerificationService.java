package com.privatecal.service;

import com.privatecal.dto.PasswordResetResponse;
import com.privatecal.entity.EmailVerificationToken;
import com.privatecal.entity.User;
import com.privatecal.repository.EmailVerificationTokenRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.config.EmailConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing email verification functionality
 * Handles the complete email verification flow from request to completion
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;
    private final EmailConfig emailConfig;

    /**
     * Send email verification token to user
     */
    public boolean sendVerificationEmail(User user) {
        try {
            logger.debug("Generating email verification token for user: {}", user.getUsername());

            // Invalidate any existing tokens for this user
            emailVerificationTokenRepository.markAllUserTokensAsUsed(user);
            emailVerificationTokenRepository.flush();

            // Generate new verification token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(48); // Token expires in 48 hours

            EmailVerificationToken verificationToken = new EmailVerificationToken(token, user, expiryDate);
            emailVerificationTokenRepository.save(verificationToken);

            // Send verification email
            String verificationUrl = buildEmailVerificationUrl(token);
            String subject = "P-Cal - Verifica il tuo indirizzo email";
            String htmlBody = templateBuilder.buildEmailVerificationEmail(user, verificationUrl);

            boolean emailSent = emailService.sendEmail(user.getEmail(), subject, htmlBody);

            if (emailSent) {
                logger.info("Verification email sent to: {}", user.getEmail());
            } else {
                logger.error("Failed to send verification email to: {}", user.getEmail());
            }

            return emailSent;

        } catch (Exception e) {
            logger.error("Error sending verification email for user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Verify user email using token
     */
    public PasswordResetResponse verifyEmail(String token) {
        logger.debug("Email verification attempt with token: {}", token);

        try {
            // Find valid token
            Optional<EmailVerificationToken> tokenOptional = emailVerificationTokenRepository
                .findValidTokenByToken(token, LocalDateTime.now());

            if (tokenOptional.isEmpty()) {
                logger.warn("Invalid or expired email verification token: {}", token);
                return new PasswordResetResponse(
                    "Token non valido o scaduto. Richiedi un nuovo link di verifica.",
                    false
                );
            }

            EmailVerificationToken verificationToken = tokenOptional.get();
            User user = verificationToken.getUser();

            // Mark email as verified
            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Mark token as used
            verificationToken.setUsed(true);
            emailVerificationTokenRepository.save(verificationToken);

            // Send welcome email after successful verification
            try {
                emailService.sendWelcomeEmail(user);
            } catch (Exception e) {
                logger.error("Failed to send welcome email after verification for user: {}", user.getUsername(), e);
                // Don't fail verification if welcome email fails
            }

            logger.info("Email verified successfully for user: {}", user.getUsername());

            return new PasswordResetResponse(
                "Email verificata con successo! Ora puoi effettuare il login.",
                true
            );

        } catch (Exception e) {
            logger.error("Error during email verification with token: {}", token, e);
            return new PasswordResetResponse(
                "Si è verificato un errore durante la verifica. Riprova più tardi.",
                false
            );
        }
    }

    /**
     * Resend verification email (with rate limiting)
     */
    public PasswordResetResponse resendVerificationEmail(String email) {
        logger.debug("Resend verification email requested for: {}", email);

        try {
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty()) {
                // Don't reveal if email exists or not for security
                logger.warn("Resend verification requested for non-existent email: {}", email);
                return new PasswordResetResponse(
                    "Se l'email esiste e non è ancora verificata, riceverai un nuovo link di verifica.",
                    true
                );
            }

            User user = userOptional.get();

            // Check if email is already verified
            if (user.getEmailVerified()) {
                logger.info("Resend verification requested for already verified user: {}", user.getUsername());
                return new PasswordResetResponse(
                    "Questa email è già stata verificata. Puoi effettuare il login.",
                    true
                );
            }

            // Rate limiting: max 5 resends per hour
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentTokensCount = emailVerificationTokenRepository.countRecentTokensByUser(user, oneHourAgo);

            if (recentTokensCount >= 5) {
                logger.warn("Rate limit exceeded for user: {} ({} requests in last hour)", user.getUsername(), recentTokensCount);
                return new PasswordResetResponse(
                    "Hai richiesto troppi link di verifica. Riprova tra un'ora.",
                    false
                );
            }

            // Send new verification email
            boolean emailSent = sendVerificationEmail(user);

            if (!emailSent) {
                return new PasswordResetResponse(
                    "Si è verificato un errore durante l'invio dell'email. Riprova più tardi.",
                    false
                );
            }

            return new PasswordResetResponse(
                "Se l'email esiste e non è ancora verificata, riceverai un nuovo link di verifica.",
                true
            );

        } catch (Exception e) {
            logger.error("Error during resend verification email for: {}", email, e);
            return new PasswordResetResponse(
                "Si è verificato un errore. Riprova più tardi.",
                false
            );
        }
    }

    /**
     * Cleanup expired email verification tokens (scheduled job)
     */
    public void cleanupExpiredVerificationTokens() {
        logger.debug("Cleaning up expired email verification tokens");

        try {
            // Delete tokens expired more than 48 hours ago
            LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
            emailVerificationTokenRepository.deleteExpiredTokens(cutoff);

            // Delete used tokens older than 7 days
            LocalDateTime usedCutoff = LocalDateTime.now().minusDays(7);
            emailVerificationTokenRepository.deleteUsedTokensOlderThan(usedCutoff);

            logger.info("Email verification tokens cleanup completed");

        } catch (Exception e) {
            logger.error("Error during email verification tokens cleanup", e);
        }
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    /**
     * Build email verification URL
     */
    private String buildEmailVerificationUrl(String token) {
        return emailConfig.getBaseUrl() + "/verify-email?token=" + token;
    }
}
