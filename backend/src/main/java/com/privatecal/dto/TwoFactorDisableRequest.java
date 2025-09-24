package com.privatecal.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorDisableRequest {
    @NotBlank(message = "Password is required")
    private String password;

    public TwoFactorDisableRequest() {}

    public TwoFactorDisableRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}