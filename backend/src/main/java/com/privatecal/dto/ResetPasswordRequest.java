package com.privatecal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for reset password functionality
 */
public class ResetPasswordRequest {

    @NotBlank(message = "Token è obbligatorio")
    private String token;

    @NotBlank(message = "Password è obbligatoria")
    @Size(min = 8, max = 128, message = "Password deve essere tra 8 e 128 caratteri")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password deve contenere almeno una lettera minuscola, una maiuscola e un numero"
    )
    private String newPassword;

    // Constructors
    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ResetPasswordRequest{" +
                "token='" + token + '\'' +
                ", newPassword='[PROTECTED]'" +
                '}';
    }
}