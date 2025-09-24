package com.privatecal.dto;

public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private String manualEntryKey;

    public TwoFactorSetupResponse() {}

    public TwoFactorSetupResponse(String secret, String qrCodeUrl, String manualEntryKey) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
        this.manualEntryKey = manualEntryKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getManualEntryKey() {
        return manualEntryKey;
    }

    public void setManualEntryKey(String manualEntryKey) {
        this.manualEntryKey = manualEntryKey;
    }
}