package com.privatecal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for password reset operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {

    private String message;
    private boolean success;
}