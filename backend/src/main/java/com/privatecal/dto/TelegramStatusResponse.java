package com.privatecal.dto;

/**
 * Response DTO for Telegram registration status
 */
public class TelegramStatusResponse {
    private boolean registered;
    private String chatId;

    public TelegramStatusResponse() {}

    public TelegramStatusResponse(boolean registered, String chatId) {
        this.registered = registered;
        this.chatId = chatId;
    }

    // Getters and Setters
    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}
