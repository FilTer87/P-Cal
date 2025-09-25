package com.privatecal.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Test configuration to provide beans needed for testing
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provide a test auditor for JPA auditing
     */
    @Bean
    @Primary
    public AuditorAware<String> testAuditorAware() {
        return () -> {
            SecurityContext context = SecurityContextHolder.getContext();
            if (context != null) {
                Authentication authentication = context.getAuthentication();
                if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                    return Optional.of(authentication.getName());
                }
            }
            return Optional.of("test-user");
        };
    }
}