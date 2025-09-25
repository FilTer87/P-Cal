package com.privatecal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for email settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.email")
@Validated
public class EmailConfig {

    private boolean enabled = false;

    @Email
    @NotBlank
    private String fromAddress = "noreply@privatecal.com";

    @NotBlank
    private String fromName = "P-Cal";

    @NotBlank
    private String baseUrl = "http://localhost:3000";

    private Templates templates = new Templates();

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Templates getTemplates() {
        return templates;
    }

    public void setTemplates(Templates templates) {
        this.templates = templates;
    }

    public static class Templates {
        private String path = "classpath:/templates/email/";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}