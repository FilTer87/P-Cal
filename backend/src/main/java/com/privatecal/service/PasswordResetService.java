package com.privatecal.service;

import com.privatecal.dto.ForgotPasswordRequest;
import com.privatecal.dto.PasswordResetResponse;
import com.privatecal.dto.ResetPasswordRequest;
import com.privatecal.entity.PasswordResetToken;
import com.privatecal.entity.User;
import com.privatecal.repository.PasswordResetTokenRepository;
import com.privatecal.repository.UserRepository;
import com.privatecal.config.EmailConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing password reset functionality
 * Handles the complete password reset flow from request to completion
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;
    private final EmailConfig emailConfig;
    private final PasswordEncoder passwordEncoder;

    /**
     * Initiate password reset process
     */
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        logger.debug("Password reset requested for email: {}", request.getEmail());

        try {
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                // Don't reveal if email exists or not for security
                logger.warn("Password reset requested for non-existent email: {}", request.getEmail());
                return new PasswordResetResponse(
                    "Se l'email esiste nel nostro sistema, riceverai le istruzioni per il reset della password.",
                    true
                );
            }

            User user = userOptional.get();

            // Invalidate any existing tokens for this user
            passwordResetTokenRepository.markAllUserTokensAsUsed(user);
            passwordResetTokenRepository.flush(); // Force execution of the update query

            // Generate new reset token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(1); // Token expires in 1 hour

            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
            passwordResetTokenRepository.save(resetToken);

            // Send password reset email
            boolean emailSent = sendPasswordResetEmail(user, token);

            if (!emailSent) {
                logger.error("Failed to send password reset email for user: {}", user.getUsername());
                return new PasswordResetResponse(
                    "Si è verificato un errore durante l'elaborazione della richiesta. Riprova più tardi.",
                    false
                );
            }

            logger.info("Password reset token generated for user: {}", user.getUsername());

            return new PasswordResetResponse(
                "Se l'email esiste nel nostro sistema, riceverai le istruzioni per il reset della password.",
                true
            );

        } catch (Exception e) {
            logger.error("Error during password reset request for email: {}", request.getEmail(), e);
            return new PasswordResetResponse(
                "Si è verificato un errore durante l'elaborazione della richiesta. Riprova più tardi.",
                false
            );
        }
    }

    /**
     * Reset password using token
     */
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        logger.debug("Password reset attempt with token: {}", request.getToken());

        try {
            // Find valid token
            Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository
                .findValidTokenByToken(request.getToken(), LocalDateTime.now());

            if (tokenOptional.isEmpty()) {
                logger.warn("Invalid or expired password reset token: {}", request.getToken());
                return new PasswordResetResponse(
                    "Token non valido o scaduto. Richiedi un nuovo reset password.",
                    false
                );
            }

            PasswordResetToken resetToken = tokenOptional.get();
            User user = resetToken.getUser();

            // Update user password
            String hashedPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPasswordHash(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Mark token as used
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            // Send confirmation email
            sendPasswordResetConfirmationEmail(user);

            logger.info("Password reset successfully for user: {}", user.getUsername());

            return new PasswordResetResponse(
                "Password reset eseguito con successo. Ora puoi effettuare il login con la nuova password.",
                true
            );

        } catch (Exception e) {
            logger.error("Error during password reset with token: {}", request.getToken(), e);
            return new PasswordResetResponse(
                "Si è verificato un errore durante il reset della password. Riprova più tardi.",
                false
            );
        }
    }

    /**
     * Cleanup expired password reset tokens (scheduled job)
     */
    public void cleanupExpiredTokens() {
        logger.debug("Cleaning up expired password reset tokens");

        try {
            // Delete tokens expired more than 24 hours ago
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            passwordResetTokenRepository.deleteExpiredTokens(cutoff);

            // Delete used tokens older than 7 days
            LocalDateTime usedCutoff = LocalDateTime.now().minusDays(7);
            passwordResetTokenRepository.deleteUsedTokensOlderThan(usedCutoff);

            logger.info("Password reset tokens cleanup completed");

        } catch (Exception e) {
            logger.error("Error during password reset tokens cleanup", e);
        }
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    /**
     * Send password reset email to user
     */
    private boolean sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = buildPasswordResetUrl(token);
            String subject = templateBuilder.getPasswordResetSubject(user);
            String htmlBody = templateBuilder.buildPasswordResetEmail(user, resetUrl);

            boolean emailSent = emailService.sendEmail(user.getEmail(), user.getFullName(), subject, htmlBody);
            if (emailSent) {
                logger.info("Password reset email sent to: {}", user.getEmail());
            } else {
                logger.error("Failed to send password reset email to: {}", user.getEmail());
            }
            return emailSent;

        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", user.getEmail(), e);
            return false;
        }
    }

    /**
     * Send password reset confirmation email
     */
    private void sendPasswordResetConfirmationEmail(User user) {
        try {
            String subject = templateBuilder.getPasswordResetConfirmationSubject(user);
            String htmlBody = templateBuilder.buildPasswordResetConfirmationEmail(user);

            boolean emailSent = emailService.sendEmail(user.getEmail(), user.getFullName(), subject, htmlBody);
            if (!emailSent) {
                logger.warn("Failed to send password reset confirmation email to: {}", user.getEmail());
                // Don't throw exception for confirmation email as password reset was successful
            } else {
                logger.info("Password reset confirmation email sent to: {}", user.getEmail());
            }

        } catch (Exception e) {
            logger.error("Failed to send password reset confirmation email to: {}", user.getEmail(), e);
            // Don't throw exception here as password reset was successful
        }
    }

    /**
     * Build password reset URL
     */
    private String buildPasswordResetUrl(String token) {
        return emailConfig.getBaseUrl() + "/reset-password?token=" + token;
    }
}
