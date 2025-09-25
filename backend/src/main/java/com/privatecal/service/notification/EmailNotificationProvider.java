package com.privatecal.service.notification;

import com.privatecal.entity.Reminder;
import com.privatecal.dto.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Email notification provider implementation (placeholder)
 * Currently logs email notifications instead of sending them
 * TODO: Implement actual email sending using JavaMailSender and spring-boot-starter-mail
 */
@Component
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationProvider.class);

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    // TODO: Add email configuration properties
    // @Value("${spring.mail.host:}")
    // private String mailHost;

    // @Value("${spring.mail.port:587}")
    // private int mailPort;

    // @Value("${spring.mail.username:}")
    // private String mailUsername;

    // @Value("${spring.mail.password:}")
    // private String mailPassword;

    // @Value("${app.mail.from:noreply@privatecal.com}")
    // private String mailFrom;

    // TODO: Inject JavaMailSender
    // private final JavaMailSender mailSender;

    @Override
    public boolean sendReminderNotification(Reminder reminder) {
        if (!isEnabled()) {
            logger.debug("Email provider is disabled");
            return false;
        }

        try {
            NotificationData data = NotificationData.fromReminder(reminder);

            if (data.getUserEmail() == null || data.getUserEmail().trim().isEmpty()) {
                logger.warn("User {} has no email address configured", data.getUserId());
                return false;
            }

            // TODO: Replace with actual email sending
            String subject = createEmailSubject(data);
            String body = createEmailBody(data);

            logger.info("EMAIL NOTIFICATION (PLACEHOLDER):");
            logger.info("To: {}", data.getUserEmail());
            logger.info("Subject: {}", subject);
            logger.info("Body: {}", body);

            // TODO: Implement actual email sending
            // MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // helper.setFrom(mailFrom);
            // helper.setTo(data.getUserEmail());
            // helper.setSubject(subject);
            // helper.setText(body, true); // true for HTML content
            // mailSender.send(message);

            // Simulate processing time
            Thread.sleep(100);

            logger.info("Email notification processed (placeholder) for task: {} to user: {}",
                    data.getTaskTitle(), data.getUserEmail());
            return true;

        } catch (Exception e) {
            logger.error("Error processing email notification for reminder ID: {}",
                    reminder.getId(), e);
            return false;
        }
    }

    @Override
    public boolean sendTestNotification(Long userId, String message) {
        if (!isEnabled()) {
            logger.debug("Email provider is disabled - cannot send test notification");
            return false;
        }

        try {
            logger.info("TEST EMAIL NOTIFICATION (PLACEHOLDER):");
            logger.info("User ID: {}", userId);
            logger.info("Message: {}", message);

            // TODO: Implement actual test email sending
            // This would require getting the user's email from the database

            return true;

        } catch (Exception e) {
            logger.error("Error sending email test notification to user ID: {}", userId, e);
            return false;
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return NotificationType.EMAIL.equals(type);
    }

    @Override
    public boolean isEnabled() {
        // TODO: Check email configuration validity
        // return emailEnabled && mailHost != null && !mailHost.trim().isEmpty() &&
        //        mailUsername != null && !mailUsername.trim().isEmpty();
        return emailEnabled;
    }

    @Override
    public String getProviderName() {
        return "Email";
    }

    /**
     * Create email subject for task reminder
     */
    private String createEmailSubject(NotificationData data) {
        return "📅 Task Reminder: " + data.getTaskTitle();
    }

    /**
     * Create email body for task reminder
     */
    private String createEmailBody(NotificationData data) {
        StringBuilder body = new StringBuilder();

        // HTML email template
        body.append("<!DOCTYPE html>");
        body.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");

        body.append("<h2 style='color: #2563eb;'>📅 Task Reminder</h2>");

        body.append("<p>Hi ");
        body.append(data.getUserFullName() != null ? data.getUserFullName() : "there");
        body.append(",</p>");

        body.append("<p>This is a reminder for your upcoming task:</p>");

        body.append("<div style='background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 16px 0;'>");
        body.append("<h3 style='margin: 0 0 12px 0; color: #1f2937;'>").append(data.getTaskTitle()).append("</h3>");

        if (data.getTaskStartTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm");
            String startTime = data.getTaskStartTime().atZone(ZoneOffset.UTC).format(formatter);
            body.append("<p><strong>📅 Scheduled:</strong> ").append(startTime).append("</p>");
        }

        if (data.getTaskLocation() != null && !data.getTaskLocation().trim().isEmpty()) {
            body.append("<p><strong>📍 Location:</strong> ").append(data.getTaskLocation()).append("</p>");
        }

        if (data.getTaskDescription() != null && !data.getTaskDescription().trim().isEmpty()) {
            body.append("<p><strong>💭 Description:</strong> ").append(data.getTaskDescription()).append("</p>");
        }
        body.append("</div>");

        long minutesUntil = data.getMinutesUntilTaskStart();
        if (minutesUntil > 0) {
            body.append("<p><strong>⏰ This task starts in ").append(data.getFormattedTimeUntilStart()).append("</strong></p>");
        } else if (minutesUntil == 0) {
            body.append("<p><strong>🚀 This task is starting now!</strong></p>");
        } else {
            body.append("<p><strong>⚠️ This task has already started</strong></p>");
        }

        // TODO: Add action button to view task
        if (data.getTaskId() != null) {
            body.append("<div style='margin: 20px 0;'>");
            body.append("<a href='").append(getBaseUrl()).append("/tasks/").append(data.getTaskId()).append("' ");
            body.append("style='background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>");
            body.append("View Task");
            body.append("</a>");
            body.append("</div>");
        }

        body.append("<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;' />");
        body.append("<p style='color: #6b7280; font-size: 14px;'>Best regards,<br/>Your P-Cal Team</p>");

        body.append("</body></html>");

        return body.toString();
    }

    /**
     * Get base URL for email links
     * TODO: This should come from configuration
     */
    private String getBaseUrl() {
        return "https://localhost:3000"; // Frontend URL
    }
}