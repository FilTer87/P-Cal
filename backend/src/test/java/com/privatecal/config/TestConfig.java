package com.privatecal.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.privatecal.service.EmailService;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;

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

    /**
     * Provide a mock EmailService for testing
     */
    @Bean
    @Primary
    public EmailService testEmailService() {
        EmailService mockEmailService = Mockito.mock(EmailService.class);
        // Mock successful email sending by default
        Mockito.when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
               .thenReturn(true);
        return mockEmailService;
    }
}