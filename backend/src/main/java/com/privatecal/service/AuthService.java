package com.privatecal.service;

import com.privatecal.dto.AuthRequest;
import com.privatecal.dto.AuthResponse;
import com.privatecal.dto.UserResponse;
import com.privatecal.entity.User;
import com.privatecal.repository.UserRepository;
import com.privatecal.security.JwtUtils;
import com.privatecal.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.privatecal.entity.PasswordResetToken;
import com.privatecal.repository.PasswordResetTokenRepository;
import com.privatecal.dto.ForgotPasswordRequest;
import com.privatecal.dto.ResetPasswordRequest;
import com.privatecal.dto.PasswordResetResponse;
import com.privatecal.config.EmailConfig;

/**
 * Authentication service for user login, registration, and token management
 */
@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailConfig emailConfig;

    /**
     * Authenticate user and generate JWT tokens
     */
    public AuthResponse login(AuthRequest loginRequest) {
        try {
            logger.debug("Attempting login for user: {}", loginRequest.getUsername());
            
            // Validate input
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                return AuthResponse.error("Username is required");
            }
            
            if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
                return AuthResponse.error("Password is required");
            }
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername().trim(),
                    loginRequest.getPassword()
                )
            );
            
            UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

            // Get user entity to check 2FA status
            User user = getUserFromUserDetails(userPrincipal);

            // Check if 2FA is enabled for this user
            if (user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled()) {
                // If 2FA code is provided, verify it
                if (loginRequest.getTwoFactorCode() != null && !loginRequest.getTwoFactorCode().trim().isEmpty()) {
                    logger.info("Verifying 2FA code for user {}", userPrincipal.getUsername());

                    boolean isValidCode = twoFactorService.verifyCode(user.getTwoFactorSecret(), loginRequest.getTwoFactorCode());

                    if (!isValidCode) {
                        logger.warn("Invalid 2FA code for user {}", userPrincipal.getUsername());
                        return AuthResponse.error("Invalid two-factor authentication code");
                    }

                    logger.info("2FA code verified successfully for user {}", userPrincipal.getUsername());
                } else {
                    logger.info("User {} requires 2FA verification", userPrincipal.getUsername());
                    return AuthResponse.requireTwoFactor("Two-factor authentication required");
                }
            }

            // Generate JWT tokens (only if 2FA not required)
            String accessToken = jwtUtils.generateAccessToken(userPrincipal, userPrincipal.getId(), userPrincipal.getFullName());
            String refreshToken = jwtUtils.generateRefreshToken(userPrincipal, userPrincipal.getId());

            // Get token expiration times
            LocalDateTime accessTokenExpires = jwtUtils.getAccessTokenExpirationTime();
            LocalDateTime refreshTokenExpires = jwtUtils.getRefreshTokenExpirationTime();

            // Create user response
            UserResponse userResponse = UserResponse.minimal(user);

            logger.info("User {} logged in successfully", userPrincipal.getUsername());

            return AuthResponse.success(
                accessToken,
                refreshToken,
                accessTokenExpires,
                refreshTokenExpires,
                userResponse,
                "Login successful"
            );
            
        } catch (BadCredentialsException e) {
            logger.warn("Login failed - bad credentials for user: {}", loginRequest.getUsername());
            return AuthResponse.error("Invalid username or password");
        } catch (AuthenticationException e) {
            logger.warn("Login failed - authentication error for user: {}", loginRequest.getUsername(), e);
            return AuthResponse.error("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Login failed - unexpected error for user: {}", loginRequest.getUsername(), e);
            return AuthResponse.error("Login failed due to server error");
        }
    }
    
    /**
     * Register a new user
     */
    public AuthResponse register(AuthRequest registerRequest) {
        try {
            logger.debug("Attempting registration for user: {}", registerRequest.getUsername());
            
            // Validate registration request
            AuthResponse validationResult = validateRegistrationRequest(registerRequest);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            // Check if user already exists
            if (userRepository.existsByUsername(registerRequest.getUsername().trim())) {
                logger.warn("Registration failed - username already exists: {}", registerRequest.getUsername());
                return AuthResponse.error("Username is already taken");
            }
            
            if (userRepository.existsByEmail(registerRequest.getEmail().trim())) {
                logger.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
                return AuthResponse.error("Email is already registered");
            }
            
            // Create new user
            User newUser = new User();
            newUser.setUsername(registerRequest.getUsername().trim());
            newUser.setEmail(registerRequest.getEmail().trim());
            newUser.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            
            if (registerRequest.getFirstName() != null && !registerRequest.getFirstName().trim().isEmpty()) {
                newUser.setFirstName(registerRequest.getFirstName().trim());
            }
            
            if (registerRequest.getLastName() != null && !registerRequest.getLastName().trim().isEmpty()) {
                newUser.setLastName(registerRequest.getLastName().trim());
            }
            
            if (registerRequest.getTimezone() != null && !registerRequest.getTimezone().trim().isEmpty()) {
                newUser.setTimezone(registerRequest.getTimezone().trim());
            }
            
            // Save user
            User savedUser = userRepository.save(newUser);

            // Generate and assign NTFY topic for the new user
            try {
                String ntfyTopic = notificationService.generateNtfyTopicForUser(savedUser.getId());
                savedUser.setNtfyTopic(ntfyTopic);
                savedUser = userRepository.save(savedUser);
                logger.info("Generated NTFY topic for new user {}: {}", savedUser.getUsername(), ntfyTopic);
            } catch (Exception e) {
                logger.error("Failed to generate NTFY topic for new user {}: {}", savedUser.getUsername(), e.getMessage());
                // Continue with registration even if NTFY topic generation fails
            }

            // Create UserDetails for token generation
            UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);
            
            // Generate JWT tokens
            String accessToken = jwtUtils.generateAccessToken(userDetails, savedUser.getId(), savedUser.getFullName());
            String refreshToken = jwtUtils.generateRefreshToken(userDetails, savedUser.getId());
            
            // Get token expiration times
            LocalDateTime accessTokenExpires = jwtUtils.getAccessTokenExpirationTime();
            LocalDateTime refreshTokenExpires = jwtUtils.getRefreshTokenExpirationTime();
            
            // Create user response
            UserResponse userResponse = UserResponse.minimal(savedUser);
            
            logger.info("User {} registered successfully", savedUser.getUsername());
            
            return AuthResponse.success(
                accessToken, 
                refreshToken, 
                accessTokenExpires, 
                refreshTokenExpires, 
                userResponse,
                "Registration successful"
            );
            
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", registerRequest.getUsername(), e);
            return AuthResponse.error("Registration failed due to server error");
        }
    }
    
    /**
     * Refresh JWT access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            logger.debug("Attempting token refresh");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return AuthResponse.error("Refresh token is required");
            }
            
            // Validate refresh token
            if (!jwtUtils.validateRefreshToken(refreshToken)) {
                logger.warn("Token refresh failed - invalid refresh token");
                return AuthResponse.error("Invalid or expired refresh token");
            }
            
            // Get username from refresh token
            String username = jwtUtils.getUsernameFromToken(refreshToken);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;
            
            // Generate new access token
            String newAccessToken = jwtUtils.generateAccessToken(userDetailsImpl, userDetailsImpl.getId(), userDetailsImpl.getFullName());
            LocalDateTime accessTokenExpires = jwtUtils.getAccessTokenExpirationTime();
            
            logger.debug("Token refreshed successfully for user: {}", username);
            
            return AuthResponse.refreshSuccess(newAccessToken, accessTokenExpires);
            
        } catch (UsernameNotFoundException e) {
            logger.warn("Token refresh failed - user not found");
            return AuthResponse.error("User not found");
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return AuthResponse.error("Token refresh failed");
        }
    }
    
    /**
     * Validate user credentials without generating tokens
     */
    public boolean validateCredentials(String username, String password) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            return true;
        } catch (AuthenticationException e) {
            logger.debug("Credential validation failed for user: {}", username);
            return false;
        }
    }
    
    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByUsername(username.trim());
    }
    
    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByEmail(email.trim());
    }
    
    /**
     * Get user information from JWT token
     */
    public Optional<UserResponse> getUserFromToken(String token) {
        try {
            if (!jwtUtils.validateAccessToken(token)) {
                return Optional.empty();
            }
            
            String username = jwtUtils.getUsernameFromToken(token);
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            return userOpt.map(UserResponse::fromUser);
            
        } catch (Exception e) {
            logger.error("Error getting user from token", e);
            return Optional.empty();
        }
    }
    
    /**
     * Validate registration request
     */
    private AuthResponse validateRegistrationRequest(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return AuthResponse.validationError("Username is required");
        }
        
        if (request.getUsername().trim().length() < 3 || request.getUsername().trim().length() > 50) {
            return AuthResponse.validationError("Username must be between 3 and 50 characters");
        }
        
        if (!request.getUsername().matches("^[a-zA-Z0-9_]+$")) {
            return AuthResponse.validationError("Username can only contain letters, numbers, and underscores");
        }
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return AuthResponse.validationError("Email is required");
        }
        
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            return AuthResponse.validationError("Invalid email format");
        }
        
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return AuthResponse.validationError("Password must be at least 6 characters long");
        }
        
        return AuthResponse.success(null, null, null, null, null);
    }
    
    /**
     * Verify password for a user
     */
    public boolean verifyPassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    /**
     * Get User entity from UserDetails
     */
    private User getUserFromUserDetails(UserDetailsImpl userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
    }

    /**
     * Initiate password reset process
     */
    @Transactional
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
            sendPasswordResetEmail(user, token);

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
    @Transactional
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
     * Send password reset email to user
     */
    private void sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = buildPasswordResetUrl(token);
            String subject = "P-Cal - Reset Password";
            String htmlBody = buildPasswordResetEmailTemplate(user, resetUrl);

            boolean emailSent = emailService.sendEmail(user.getEmail(), subject, htmlBody);
            if (!emailSent) {
                throw new RuntimeException("Failed to send password reset email");
            }
            logger.info("Password reset email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send password reset confirmation email
     */
    private void sendPasswordResetConfirmationEmail(User user) {
        try {
            String subject = "P-Cal - Password Reset Confermato";
            String htmlBody = buildPasswordResetConfirmationTemplate(user);

            boolean emailSent = emailService.sendEmail(user.getEmail(), subject, htmlBody);
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

    /**
     * Build password reset email template
     */
    private String buildPasswordResetEmailTemplate(User user, String resetUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Reset Password - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">P-Cal - Reset Password</h1>
                    <p>Ciao %s,</p>
                    <p>Hai richiesto il reset della password per il tuo account P-Cal.</p>
                    <p>Clicca sul seguente link per reimpostare la password:</p>
                    <div style="margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Reset Password</a>
                    </div>
                    <p><strong>Questo link scadrà tra 1 ora.</strong></p>
                    <p>Se non hai richiesto il reset della password, ignora questa email.</p>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Questo è un messaggio automatico, non rispondere a questa email.
                    </p>
                </div>
            </body>
            </html>
            """,
            user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
            resetUrl
        );
    }

    /**
     * Build password reset confirmation email template
     */
    private String buildPasswordResetConfirmationTemplate(User user) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset Confermato - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #16a34a;">P-Cal - Password Reset Confermato</h1>
                    <p>Ciao %s,</p>
                    <p>La tua password è stata reimpostata con successo.</p>
                    <p>Ora puoi effettuare il login con la nuova password.</p>
                    <p>Se non hai effettuato tu questa operazione, contatta immediatamente il tuo amministratore di sistema.</p>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Questo è un messaggio automatico, non rispondere a questa email.
                    </p>
                </div>
            </body>
            </html>
            """,
            user.getFirstName() != null ? user.getFirstName() : user.getUsername()
        );
    }

    /**
     * Cleanup expired password reset tokens (scheduled job)
     */
    @Transactional
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
}