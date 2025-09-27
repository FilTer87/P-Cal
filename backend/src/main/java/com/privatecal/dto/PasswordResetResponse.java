package com.privatecal.dto;

/**
 * Response DTO for password reset operations
 */
public class PasswordResetResponse {

    private String message;
    private boolean success;

    // Constructors
    public PasswordResetResponse() {}

    public PasswordResetResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "PasswordResetResponse{" +
                "message='" + message + '\'' +
                ", success=" + success +
                '}';
    }
}