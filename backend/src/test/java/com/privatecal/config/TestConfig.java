package com.privatecal.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.privatecal.service.EmailService;
import com.privatecal.service.EmailTemplateBuilder;
import com.privatecal.entity.User;
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
        // Mock successful email sending by default (new signature with 4 parameters: to, toName, subject, body)
        Mockito.when(mockEmailService.sendEmail(anyString(), anyString(), anyString(), anyString()))
               .thenReturn(true);
        // Also mock the old 3-parameter version for backward compatibility
        Mockito.when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
               .thenReturn(true);
        return mockEmailService;
    }

    /**
     * Provide a mock EmailTemplateBuilder for testing
     * Returns mock subjects and HTML content to avoid Thymeleaf dependency in tests
     */
    @Bean
    @Primary
    public EmailTemplateBuilder testEmailTemplateBuilder() {
        EmailTemplateBuilder mockTemplateBuilder = Mockito.mock(EmailTemplateBuilder.class);

        // Mock password reset email
        Mockito.when(mockTemplateBuilder.getPasswordResetSubject(any(User.class)))
               .thenReturn("Password Reset - P-Cal");
        Mockito.when(mockTemplateBuilder.buildPasswordResetEmail(any(User.class), anyString()))
               .thenReturn("<html>Password Reset Email</html>");

        // Mock password reset confirmation email
        Mockito.when(mockTemplateBuilder.getPasswordResetConfirmationSubject(any(User.class)))
               .thenReturn("Password Reset Completed - P-Cal");
        Mockito.when(mockTemplateBuilder.buildPasswordResetConfirmationEmail(any(User.class)))
               .thenReturn("<html>Password Reset Confirmation</html>");

        // Mock email verification
        Mockito.when(mockTemplateBuilder.getEmailVerificationSubject(any(User.class)))
               .thenReturn("Email Verification - P-Cal");
        Mockito.when(mockTemplateBuilder.buildEmailVerificationEmail(any(User.class), anyString()))
               .thenReturn("<html>Email Verification</html>");

        // Mock welcome email
        Mockito.when(mockTemplateBuilder.getWelcomeSubject(any(User.class)))
               .thenReturn("Welcome to P-Cal!");
        Mockito.when(mockTemplateBuilder.buildWelcomeEmail(any(User.class)))
               .thenReturn("<html>Welcome Email</html>");

        // Mock task reminder email
        Mockito.when(mockTemplateBuilder.getTaskReminderSubject(any(User.class), anyString()))
               .thenReturn("Task Reminder - P-Cal");
        Mockito.when(mockTemplateBuilder.buildTaskReminderEmail(
                any(User.class), anyString(), anyString(), anyString(), anyString()))
               .thenReturn("<html>Task Reminder</html>");

        // Mock test email
        Mockito.when(mockTemplateBuilder.getTestEmailSubject(anyString()))
               .thenReturn("P-Cal Test Email");
        Mockito.when(mockTemplateBuilder.buildTestEmail(anyString(), anyString()))
               .thenReturn("<html>Test Email</html>");

        return mockTemplateBuilder;
    }
}