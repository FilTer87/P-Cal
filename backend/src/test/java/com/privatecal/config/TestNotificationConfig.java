package com.privatecal.config;

import com.privatecal.service.notification.EmailNotificationProvider;
import com.privatecal.service.notification.NTFYNotificationProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to exclude real notification providers and use only mocks
 */
@TestConfiguration
@Profile("test")
public class TestNotificationConfig {

    /**
     * Exclude real NTFY provider in test environment
     * The MockNTFYNotificationProvider will be used instead
     */
    @Bean
    @Primary
    public NTFYNotificationProvider excludeRealNTFYProvider() {
        return null; // This prevents the real provider from being loaded
    }

    /**
     * Exclude real Email provider in test environment
     * The MockEmailNotificationProvider will be used instead
     */
    @Bean
    @Primary
    public EmailNotificationProvider excludeRealEmailProvider() {
        return null; // This prevents the real provider from being loaded
    }
}