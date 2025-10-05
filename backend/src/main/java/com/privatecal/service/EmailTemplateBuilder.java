package com.privatecal.service;

import com.privatecal.entity.User;
import com.privatecal.config.EmailConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

/**
 * Service for building HTML email templates using Thymeleaf
 * Supports multilingual emails based on user locale
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateBuilder {

    private final SpringTemplateEngine emailTemplateEngine;
    private final MessageSource emailMessageSource;
    private final EmailConfig emailConfig;

    /**
     * Get user locale or fallback to en-US
     */
    private Locale getUserLocale(User user) {
        if (user != null && user.getLocale() != null) {
            return Locale.forLanguageTag(user.getLocale());
        }
        return Locale.forLanguageTag("en-US");
    }

    /**
     * Get message with user locale
     */
    private String getMessage(String key, Locale locale, Object... params) {
        return emailMessageSource.getMessage(key, params, locale);
    }

    // ============================================
    // PASSWORD RESET TEMPLATES
    // ============================================

    /**
     * Build password reset request email template
     */
    public String buildPasswordResetEmail(User user, String resetUrl) {
        Locale locale = getUserLocale(user);

        Context context = new Context(locale);
        context.setVariable("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        context.setVariable("resetUrl", resetUrl);

        return emailTemplateEngine.process("password-reset", context);
    }

    /**
     * Get password reset email subject
     */
    public String getPasswordResetSubject(User user) {
        return getMessage("email.passwordReset.title", getUserLocale(user));
    }

    /**
     * Build password reset confirmation email template
     */
    public String buildPasswordResetConfirmationEmail(User user) {
        Locale locale = getUserLocale(user);

        Context context = new Context(locale);
        context.setVariable("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        context.setVariable("loginUrl", emailConfig.getBaseUrl() + "/login");

        return emailTemplateEngine.process("password-reset-confirmation", context);
    }

    /**
     * Get password reset confirmation email subject
     */
    public String getPasswordResetConfirmationSubject(User user) {
        return getMessage("email.passwordResetConfirm.title", getUserLocale(user));
    }

    // ============================================
    // EMAIL VERIFICATION TEMPLATES
    // ============================================

    /**
     * Build email verification email template
     */
    public String buildEmailVerificationEmail(User user, String verificationUrl) {
        Locale locale = getUserLocale(user);

        Context context = new Context(locale);
        context.setVariable("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        context.setVariable("verificationUrl", verificationUrl);

        return emailTemplateEngine.process("email-verification", context);
    }

    /**
     * Get email verification subject
     */
    public String getEmailVerificationSubject(User user) {
        return getMessage("email.verification.title", getUserLocale(user));
    }

    // ============================================
    // WELCOME EMAIL TEMPLATES
    // ============================================

    /**
     * Build welcome email template
     */
    public String buildWelcomeEmail(User user) {
        Locale locale = getUserLocale(user);

        Context context = new Context(locale);
        context.setVariable("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        context.setVariable("appUrl", emailConfig.getBaseUrl());

        return emailTemplateEngine.process("welcome", context);
    }

    /**
     * Get welcome email subject
     */
    public String getWelcomeSubject(User user) {
        return getMessage("email.welcome.title", getUserLocale(user));
    }

    // ============================================
    // TASK REMINDER TEMPLATES
    // ============================================

    /**
     * Build task reminder email template
     */
    public String buildTaskReminderEmail(User user, String taskTitle, String taskDescription,
                                        String taskStartTime, String taskLocation) {
        Locale locale = getUserLocale(user);

        Context context = new Context(locale);
        context.setVariable("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        context.setVariable("taskTitle", taskTitle);
        context.setVariable("taskDescription", taskDescription);
        context.setVariable("taskStartTime", taskStartTime);
        context.setVariable("taskLocation", taskLocation);
        context.setVariable("appUrl", emailConfig.getBaseUrl());

        return emailTemplateEngine.process("task-reminder", context);
    }

    /**
     * Get task reminder email subject
     */
    public String getTaskReminderSubject(User user, String taskTitle) {
        return getMessage("email.taskReminder.title", getUserLocale(user), taskTitle);
    }

    // ============================================
    // TEST EMAIL TEMPLATES
    // ============================================

    /**
     * Build test email template
     */
    public String buildTestEmail(String customMessage, String userLocale) {
        Locale locale = userLocale != null
            ? Locale.forLanguageTag(userLocale)
            : Locale.forLanguageTag("en-US");

        Context context = new Context(locale);
        context.setVariable("customMessage", customMessage);

        return emailTemplateEngine.process("test-email", context);
    }

    /**
     * Get test email subject
     */
    public String getTestEmailSubject(String userLocale) {
        Locale locale = userLocale != null
            ? Locale.forLanguageTag(userLocale)
            : Locale.forLanguageTag("en-US");
        return getMessage("email.test.title", locale);
    }
}
