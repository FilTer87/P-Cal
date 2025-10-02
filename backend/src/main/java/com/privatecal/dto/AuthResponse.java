package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for authentication response (JWT tokens and user info)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // Duration in seconds
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime accessTokenExpires;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime refreshTokenExpires;
    
    private UserResponse user;
    private String message;
    private boolean success = true;
    private boolean requiresTwoFactor = false;
    private boolean requiresEmailVerification = false;
    
    // Default constructor
    public AuthResponse() {}
    
    // Success response constructor
    public AuthResponse(String accessToken, String refreshToken, LocalDateTime accessTokenExpires, 
                       LocalDateTime refreshTokenExpires, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpires = accessTokenExpires;
        this.refreshTokenExpires = refreshTokenExpires;
        this.user = user;
        this.success = true;
        
        // Calculate expiresIn as duration between now and access token expiration in seconds
        if (accessTokenExpires != null) {
            this.expiresIn = java.time.Duration.between(LocalDateTime.now(), accessTokenExpires).getSeconds();
        }
    }
    
    // Error response constructor
    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
    
    // Factory methods for common responses
    
    /**
     * Create successful authentication response
     */
    public static AuthResponse success(String accessToken, String refreshToken, 
                                     LocalDateTime accessTokenExpires, LocalDateTime refreshTokenExpires,
                                     UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, accessTokenExpires, refreshTokenExpires, user);
    }
    
    /**
     * Create successful authentication response with message
     */
    public static AuthResponse success(String accessToken, String refreshToken, 
                                     LocalDateTime accessTokenExpires, LocalDateTime refreshTokenExpires,
                                     UserResponse user, String message) {
        AuthResponse response = new AuthResponse(accessToken, refreshToken, accessTokenExpires, refreshTokenExpires, user);
        response.setMessage(message);
        return response;
    }
    
    /**
     * Create successful token refresh response
     */
    public static AuthResponse refreshSuccess(String accessToken, LocalDateTime accessTokenExpires) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setAccessTokenExpires(accessTokenExpires);
        response.setSuccess(true);
        response.setMessage("Token refreshed successfully");
        
        // Calculate expiresIn for refresh response
        if (accessTokenExpires != null) {
            response.setExpiresIn(java.time.Duration.between(LocalDateTime.now(), accessTokenExpires).getSeconds());
        }
        
        return response;
    }
    
    /**
     * Create error response
     */
    public static AuthResponse error(String message) {
        return new AuthResponse(message, false);
    }
    
    /**
     * Create validation error response
     */
    public static AuthResponse validationError(String message) {
        AuthResponse response = new AuthResponse(message, false);
        response.setMessage("Validation failed: " + message);
        return response;
    }

    /**
     * Create 2FA required response
     */
    public static AuthResponse requireTwoFactor(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setRequiresTwoFactor(true);
        response.setMessage(message != null ? message : "Two-factor authentication required");
        return response;
    }

    /**
     * Create email verification required response
     */
    public static AuthResponse requireEmailVerification(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setRequiresEmailVerification(true);
        response.setMessage(message != null ? message : "Please verify your email address to complete registration");
        return response;
    }

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public LocalDateTime getAccessTokenExpires() {
        return accessTokenExpires;
    }
    
    public void setAccessTokenExpires(LocalDateTime accessTokenExpires) {
        this.accessTokenExpires = accessTokenExpires;
    }
    
    public LocalDateTime getRefreshTokenExpires() {
        return refreshTokenExpires;
    }
    
    public void setRefreshTokenExpires(LocalDateTime refreshTokenExpires) {
        this.refreshTokenExpires = refreshTokenExpires;
    }
    
    public UserResponse getUser() {
        return user;
    }
    
    public void setUser(UserResponse user) {
        this.user = user;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public boolean isRequiresTwoFactor() {
        return requiresTwoFactor;
    }

    public void setRequiresTwoFactor(boolean requiresTwoFactor) {
        this.requiresTwoFactor = requiresTwoFactor;
    }

    public boolean isRequiresEmailVerification() {
        return requiresEmailVerification;
    }

    public void setRequiresEmailVerification(boolean requiresEmailVerification) {
        this.requiresEmailVerification = requiresEmailVerification;
    }

    // Helper methods
    
    /**
     * Check if response contains valid tokens
     */
    public boolean hasValidTokens() {
        return accessToken != null && !accessToken.trim().isEmpty() &&
               refreshToken != null && !refreshToken.trim().isEmpty();
    }
    
    /**
     * Check if this is a token refresh response
     */
    public boolean isTokenRefresh() {
        return accessToken != null && refreshToken == null;
    }
    
    /**
     * Get token expiration summary
     */
    public String getTokenExpirationSummary() {
        if (accessTokenExpires != null) {
            return "Access token expires: " + accessTokenExpires;
        }
        return "No expiration information available";
    }
    
    @Override
    public String toString() {
        return "AuthResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", hasAccessToken=" + (accessToken != null) +
                ", hasRefreshToken=" + (refreshToken != null) +
                ", accessTokenExpires=" + accessTokenExpires +
                ", refreshTokenExpires=" + refreshTokenExpires +
                ", hasUser=" + (user != null) +
                ", message='" + message + '\'' +
                ", success=" + success +
                '}';
    }
}