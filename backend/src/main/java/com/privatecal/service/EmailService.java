package com.privatecal.service;

import com.privatecal.config.EmailConfig;
import com.privatecal.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending emails
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final EmailTemplateBuilder emailTemplateBuilder;

    /**
     * Send a simple HTML email
     */
    public boolean sendEmail(String to, String subject, String htmlContent) {
        return sendEmail(to, null, subject, htmlContent);
    }

    /**
     * Send an HTML email with optional recipient name
     */
    public boolean sendEmail(String to, String toName, String subject, String htmlContent) {
        if (!emailConfig.isEnabled()) {
            logger.debug("Email service is disabled - skipping email to: {}", to);
            return false;
        }

        if (!StringUtils.hasText(to)) {
            logger.warn("Cannot send email - recipient address is empty");
            return false;
        }

        if (!StringUtils.hasText(subject)) {
            logger.warn("Cannot send email - subject is empty");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            );

            // Set sender
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());

            // Set recipient
            if (StringUtils.hasText(toName)) {
                // Create InternetAddress with personal name
                InternetAddress toAddress = new InternetAddress(to, toName);
                helper.setTo(toAddress);
            } else {
                helper.setTo(to);
            }

            // Set subject and content
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content

            // Send the email
            mailSender.send(message);

            logger.info("Email sent successfully to: {} with subject: {}", to, subject);
            return true;

        } catch (MessagingException e) {
            logger.error("Failed to create email message for: {} - {}", to, e.getMessage(), e);
            return false;
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to encode email address for: {} - {}", to, e.getMessage(), e);
            return false;
        } catch (MailException e) {
            logger.error("Failed to send email to: {} - {}", to, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending email to: {} - {}", to, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send task reminder email (multilingual)
     */
    public boolean sendTaskReminderEmail(User user, String taskTitle, String taskDescription,
                                       String taskStartTime, String taskLocation) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            logger.warn("Cannot send reminder email - user or email is null");
            return false;
        }

        String subject = emailTemplateBuilder.getTaskReminderSubject(user, taskTitle);
        String htmlContent = emailTemplateBuilder.buildTaskReminderEmail(
            user, taskTitle, taskDescription, taskStartTime, taskLocation
        );

        return sendEmail(user.getEmail(), user.getFullName(), subject, htmlContent);
    }

    /**
     * Send welcome email to new users (multilingual)
     */
    public boolean sendWelcomeEmail(User user) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            logger.warn("Cannot send welcome email - user or email is null");
            return false;
        }

        String subject = emailTemplateBuilder.getWelcomeSubject(user);
        String htmlContent = emailTemplateBuilder.buildWelcomeEmail(user);

        return sendEmail(user.getEmail(), user.getFullName(), subject, htmlContent);
    }

    /**
     * Send test email (multilingual)
     */
    public boolean sendTestEmail(String to, String message) {
        return sendTestEmail(to, message, "en-US");
    }

    /**
     * Send test email with specific locale (multilingual)
     */
    public boolean sendTestEmail(String to, String message, String userLocale) {
        String subject = emailTemplateBuilder.getTestEmailSubject(userLocale);
        String htmlContent = emailTemplateBuilder.buildTestEmail(message, userLocale);

        return sendEmail(to, null, subject, htmlContent);
    }

    /**
     * Check if email service is enabled and properly configured
     */
    public boolean isEmailServiceAvailable() {
        return emailConfig.isEnabled() &&
               StringUtils.hasText(emailConfig.getFromAddress()) &&
               mailSender != null;
    }

    /**
     * Get email configuration info for debugging
     */
    public String getEmailConfigInfo() {
        if (emailConfig == null) {
            return "Email configuration not available";
        }

        return String.format(
            "Email Service - Enabled: %s, From: %s <%s>, Base URL: %s",
            emailConfig.isEnabled(),
            emailConfig.getFromName() != null ? emailConfig.getFromName() : "N/A",
            emailConfig.getFromAddress() != null ? emailConfig.getFromAddress() : "N/A",
            emailConfig.getBaseUrl() != null ? emailConfig.getBaseUrl() : "N/A"
        );
    }
}