package com.privatecal.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for health indicators
 */
@Configuration
public class HealthConfig {

    /**
     * Custom mail health indicator that returns UP when email is disabled
     * This prevents the overall health check from failing when email is not configured
     */
    @Bean("mailHealthIndicator")
    @Primary
    @ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
    public HealthIndicator disabledMailHealthIndicator() {
        return () -> Health.up()
            .withDetail("status", "Email service is disabled")
            .withDetail("configured", false)
            .build();
    }
}