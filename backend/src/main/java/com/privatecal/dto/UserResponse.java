package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.User;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User response (user profile data)
 * Excludes sensitive information like password hash
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private Boolean emailVerified;
    private String firstName;
    private String lastName;
    private String fullName;
    private String displayName;
    private String timezone;
    private Boolean twoFactorEnabled;
    private long taskCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public UserResponse() {}
    
    // Constructor from User entity
    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.emailVerified = user.getEmailVerified();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFullName();
        this.displayName = user.getFullName() != null && !user.getFullName().equals(user.getUsername())
            ? user.getFullName() : user.getUsername();
        this.timezone = user.getTimezone();
        this.twoFactorEnabled = user.getTwoFactorEnabled();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.taskCount = user.getTasks() != null ? user.getTasks().size() : 0;
    }
    
    // Constructor with task count
    public UserResponse(User user, long taskCount) {
        this(user);
        this.taskCount = taskCount;
    }
    
    // Full constructor
    public UserResponse(Long id, String username, String email, String firstName, String lastName, 
                       String timezone, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.timezone = timezone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fullName = generateFullName();
    }
    
    // Factory methods
    
    /**
     * Create UserResponse from User entity
     */
    public static UserResponse fromUser(User user) {
        return new UserResponse(user);
    }
    
    /**
     * Create UserResponse from User entity with task count
     */
    public static UserResponse fromUser(User user, long taskCount) {
        return new UserResponse(user, taskCount);
    }
    
    /**
     * Create minimal UserResponse (for auth responses)
     */
    public static UserResponse minimal(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setEmailVerified(user.getEmailVerified());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setFullName(user.getFullName());
        response.setDisplayName(user.getFullName() != null && !user.getFullName().equals(user.getUsername())
            ? user.getFullName() : user.getUsername());
        response.setTimezone(user.getTimezone());
        response.setTwoFactorEnabled(user.getTwoFactorEnabled());
        return response;
    }
    
    // Helper method to generate full name
    private String generateFullName() {
        if (firstName != null && lastName != null && !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            return firstName.trim() + " " + lastName.trim();
        } else if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim();
        } else if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim();
        }
        return username;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.fullName = generateFullName(); // Update full name when first name changes
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.fullName = generateFullName(); // Update full name when last name changes
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.displayName = fullName != null && !fullName.equals(username) ? fullName : username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public long getTaskCount() {
        return taskCount;
    }
    
    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods
    
    /**
     * Check if user has complete profile information
     */
    public boolean hasCompleteProfile() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               timezone != null && !timezone.trim().isEmpty();
    }
    
    
    /**
     * Get user initials
     */
    public String getInitials() {
        if (firstName != null && lastName != null && !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            return (firstName.trim().substring(0, 1) + lastName.trim().substring(0, 1)).toUpperCase();
        } else if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim().substring(0, 1).toUpperCase();
        } else if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim().substring(0, 1).toUpperCase();
        } else if (username != null && !username.trim().isEmpty()) {
            return username.trim().substring(0, 1).toUpperCase();
        }
        return "U";
    }
    
    @Override
    public String toString() {
        return "UserResponse{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", timezone='" + timezone + '\'' +
                ", taskCount=" + taskCount +
                ", createdAt=" + createdAt +
                '}';
    }
}