package com.privatecal.service;

import com.privatecal.config.EmailConfig;
import com.privatecal.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailConfig emailConfig;

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
     * Send task reminder email
     */
    public boolean sendTaskReminderEmail(User user, String taskTitle, String taskDescription,
                                       String taskStartTime, String taskLocation) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            logger.warn("Cannot send reminder email - user or email is null");
            return false;
        }

        String subject = "📅 Task Reminder: " + taskTitle;
        String htmlContent = createTaskReminderEmailTemplate(
            user.getFullName(),
            taskTitle,
            taskDescription,
            taskStartTime,
            taskLocation
        );

        return sendEmail(user.getEmail(), user.getFullName(), subject, htmlContent);
    }

    /**
     * Send welcome email to new users
     */
    public boolean sendWelcomeEmail(User user) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            logger.warn("Cannot send welcome email - user or email is null");
            return false;
        }

        String subject = "Welcome to P-Cal! 🎉";
        String htmlContent = createWelcomeEmailTemplate(user.getFullName());

        return sendEmail(user.getEmail(), user.getFullName(), subject, htmlContent);
    }

    /**
     * Send test email
     */
    public boolean sendTestEmail(String to, String message) {
        String subject = "P-Cal Test Email ✅";
        String htmlContent = createTestEmailTemplate(message);

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

    /**
     * Create task reminder email HTML template
     */
    private String createTaskReminderEmailTemplate(String userName, String taskTitle,
                                                 String taskDescription, String taskStartTime,
                                                 String taskLocation) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Task Reminder</title></head>");
        html.append("<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5;'>");

        // Container
        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px 20px; text-align: center;'>");
        html.append("<h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;'>📅 Task Reminder</h1>");
        html.append("</div>");

        // Content
        html.append("<div style='padding: 30px 20px;'>");

        html.append("<p style='font-size: 16px; margin: 0 0 20px 0;'>Hi ");
        html.append(StringUtils.hasText(userName) ? userName : "there");
        html.append(",</p>");

        html.append("<p style='font-size: 16px; margin: 0 0 25px 0;'>This is a friendly reminder about your upcoming task:</p>");

        // Task card
        html.append("<div style='background: #f8f9ff; border: 2px solid #e1e5fe; border-radius: 12px; padding: 25px; margin: 20px 0;'>");
        html.append("<h2 style='color: #3f51b5; margin: 0 0 15px 0; font-size: 20px; font-weight: 600;'>").append(taskTitle).append("</h2>");

        if (StringUtils.hasText(taskStartTime)) {
            html.append("<p style='margin: 8px 0; font-size: 14px; color: #666;'><strong>📅 When:</strong> ").append(taskStartTime).append("</p>");
        }

        if (StringUtils.hasText(taskLocation)) {
            html.append("<p style='margin: 8px 0; font-size: 14px; color: #666;'><strong>📍 Where:</strong> ").append(taskLocation).append("</p>");
        }

        if (StringUtils.hasText(taskDescription)) {
            html.append("<p style='margin: 8px 0; font-size: 14px; color: #666;'><strong>💭 Description:</strong> ").append(taskDescription).append("</p>");
        }
        html.append("</div>");

        // CTA Button
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='").append(emailConfig.getBaseUrl()).append("' ");
        html.append("style='display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 8px; font-weight: 600; font-size: 16px; transition: all 0.3s ease;'>Open P-Cal</a>");
        html.append("</div>");

        html.append("</div>");

        // Footer
        html.append("<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;'>");
        html.append("<p style='margin: 0; font-size: 12px; color: #6c757d;'>Best regards,<br/>Your P-Cal Team</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Create welcome email HTML template
     */
    private String createWelcomeEmailTemplate(String userName) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Welcome to P-Cal</title></head>");
        html.append("<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5;'>");

        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 20px; text-align: center;'>");
        html.append("<h1 style='color: #ffffff; margin: 0; font-size: 32px; font-weight: 600;'>🎉 Welcome to P-Cal!</h1>");
        html.append("</div>");

        // Content
        html.append("<div style='padding: 40px 20px;'>");

        html.append("<p style='font-size: 18px; margin: 0 0 20px 0;'>Hi ");
        html.append(StringUtils.hasText(userName) ? userName : "there");
        html.append(",</p>");

        html.append("<p style='font-size: 16px; margin: 0 0 20px 0;'>Welcome to P-Cal! We're excited to have you on board. 🚀</p>");

        html.append("<p style='font-size: 16px; margin: 0 0 20px 0;'>P-Cal is your personal calendar assistant that helps you:</p>");

        html.append("<ul style='font-size: 16px; margin: 20px 0; padding-left: 30px;'>");
        html.append("<li style='margin-bottom: 8px;'>📅 Organize your tasks and events</li>");
        html.append("<li style='margin-bottom: 8px;'>⏰ Get timely reminders</li>");
        html.append("<li style='margin-bottom: 8px;'>🔔 Receive notifications via email and push</li>");
        html.append("<li style='margin-bottom: 8px;'>🎨 Customize your experience</li>");
        html.append("</ul>");

        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='").append(emailConfig.getBaseUrl()).append("' ");
        html.append("style='display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 8px; font-weight: 600; font-size: 16px;'>Get Started</a>");
        html.append("</div>");

        html.append("</div>");

        // Footer
        html.append("<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;'>");
        html.append("<p style='margin: 0; font-size: 12px; color: #6c757d;'>Happy organizing!<br/>The P-Cal Team</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Create test email HTML template
     */
    private String createTestEmailTemplate(String message) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>P-Cal Test Email</title></head>");
        html.append("<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5;'>");

        html.append("<div style='max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #28a745 0%, #20c997 100%); padding: 30px 20px; text-align: center;'>");
        html.append("<h1 style='color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;'>✅ P-Cal Test Email</h1>");
        html.append("</div>");

        // Content
        html.append("<div style='padding: 30px 20px; text-align: center;'>");

        html.append("<p style='font-size: 18px; margin: 0 0 20px 0; color: #28a745;'>Email service is working correctly! 🎉</p>");

        if (StringUtils.hasText(message)) {
            html.append("<div style='background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;'>");
            html.append("<p style='margin: 0; font-size: 16px; font-style: italic;'>\"").append(message).append("\"</p>");
            html.append("</div>");
        }

        html.append("<p style='font-size: 14px; margin: 20px 0; color: #6c757d;'>This is a test email sent from P-Cal to verify that the email configuration is working properly.</p>");

        html.append("</div>");

        // Footer
        html.append("<div style='background-color: #f8f9fa; padding: 15px; text-align: center; border-top: 1px solid #e9ecef;'>");
        html.append("<p style='margin: 0; font-size: 12px; color: #6c757d;'>P-Cal Email Service</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }
}