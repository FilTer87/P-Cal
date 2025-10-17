package com.privatecal.dto;

/**
 * Response DTO for Telegram registration token generation
 */
public class TelegramRegistrationResponse {
    private String token;
    private String botUsername;
    private String command;
    private long expiresInSeconds;

    public TelegramRegistrationResponse() {}

    public TelegramRegistrationResponse(String token, String botUsername, long expiresInSeconds) {
        this.token = token;
        this.botUsername = botUsername;
        this.command = "/start " + token;
        this.expiresInSeconds = expiresInSeconds;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
