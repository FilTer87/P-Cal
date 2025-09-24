package com.privatecal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for authentication requests (login and register)
 */
public class AuthRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;
    
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
    private String password;
    
    @Size(max = 50, message = "First name must be at most 50 characters")
    private String firstName;
    
    @Size(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;
    
    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone = "UTC";

    @Pattern(regexp = "\\d{6}", message = "2FA code must be 6 digits")
    private String twoFactorCode;

    // Default constructor
    public AuthRequest() {}
    
    // Login constructor
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Registration constructor
    public AuthRequest(String username, String email, String password, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Full constructor
    public AuthRequest(String username, String email, String password, String firstName, String lastName, String timezone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.timezone = timezone;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTwoFactorCode() {
        return twoFactorCode;
    }

    public void setTwoFactorCode(String twoFactorCode) {
        this.twoFactorCode = twoFactorCode;
    }

    // Helper methods
    
    /**
     * Check if this is a login request (only username and password provided)
     */
    public boolean isLoginRequest() {
        return email == null || email.trim().isEmpty();
    }
    
    /**
     * Check if this is a registration request (email is provided)
     */
    public boolean isRegistrationRequest() {
        return email != null && !email.trim().isEmpty();
    }
    
    /**
     * Get full name if available
     */
    public String getFullName() {
        if (firstName != null && lastName != null && !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            return firstName.trim() + " " + lastName.trim();
        } else if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim();
        } else if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "AuthRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", timezone='" + timezone + '\'' +
                ", isLogin=" + isLoginRequest() +
                '}';
    }
}