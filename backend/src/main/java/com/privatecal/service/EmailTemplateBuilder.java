package com.privatecal.service;

import com.privatecal.entity.User;
import com.privatecal.config.EmailConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for building HTML email templates
 * Centralizes all email templates in one place for maintainability
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateBuilder {

    private final EmailConfig emailConfig;

    // ============================================
    // PASSWORD RESET TEMPLATES
    // ============================================

    /**
     * Build password reset request email template
     */
    public String buildPasswordResetEmail(User user, String resetUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">P-Cal - Reset Password</h1>
                    <p>Ciao %s,</p>
                    <p>Hai richiesto il reset della password per il tuo account P-Cal.</p>
                    <p>Clicca sul seguente link per reimpostare la tua password:</p>
                    <div style="margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Reimposta Password</a>
                    </div>
                    <p><strong>Questo link scadrà tra 1 ora.</strong></p>
                    <p>Se non hai richiesto il reset della password, ignora questa email.</p>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Questo è un messaggio automatico, non rispondere a questa email.
                    </p>
                </div>
            </body>
            </html>
            """,
            user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
            resetUrl
        );
    }

    /**
     * Build password reset confirmation email template
     */
    public String buildPasswordResetConfirmationEmail(User user) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset Completed - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">P-Cal - Password Reset Completato</h1>
                    <p>Ciao %s,</p>
                    <p>La tua password è stata reimpostata con successo.</p>
                    <p>Se non hai effettuato questa modifica, contatta immediatamente il supporto.</p>
                    <div style="margin: 30px 0;">
                        <a href="%s/login" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Vai al Login</a>
                    </div>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Questo è un messaggio automatico, non rispondere a questa email.
                    </p>
                </div>
            </body>
            </html>
            """,
            user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
            emailConfig.getBaseUrl()
        );
    }

    // ============================================
    // EMAIL VERIFICATION TEMPLATES
    // ============================================

    /**
     * Build email verification email template
     */
    public String buildEmailVerificationEmail(User user, String verificationUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verifica Email - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">P-Cal - Verifica il tuo indirizzo email</h1>
                    <p>Ciao %s,</p>
                    <p>Benvenuto su P-Cal! Per completare la registrazione, devi verificare il tuo indirizzo email.</p>
                    <p>Clicca sul seguente link per verificare la tua email:</p>
                    <div style="margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Verifica Email</a>
                    </div>
                    <p><strong>Questo link scadrà tra 48 ore.</strong></p>
                    <p>Se non hai richiesto questa registrazione, ignora questa email.</p>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Questo è un messaggio automatico, non rispondere a questa email.
                    </p>
                </div>
            </body>
            </html>
            """,
            user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
            verificationUrl
        );
    }

    // ============================================
    // WELCOME & GENERAL TEMPLATES
    // ============================================

    /**
     * Build welcome email template for new users
     */
    public String buildWelcomeEmail(String fullName) {
        String greeting = fullName != null && !fullName.trim().isEmpty() ? fullName : "there";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Welcome to P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">Welcome to P-Cal! 🎉</h1>
                    <p>Hi %s,</p>
                    <p>Thank you for signing up! We're excited to have you on board.</p>
                    <p>P-Cal is your personal calendar to help you organize your tasks and stay productive.</p>
                    <h2 style="color: #2563eb; font-size: 18px; margin-top: 30px;">Getting Started</h2>
                    <ul style="line-height: 2;">
                        <li>Create your first task</li>
                        <li>Set up reminders to stay on track</li>
                        <li>Organize your schedule efficiently</li>
                    </ul>
                    <div style="margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Go to Dashboard</a>
                    </div>
                    <p>If you have any questions or need help, feel free to reach out to our support team.</p>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Best regards,<br>
                        The P-Cal Team
                    </p>
                </div>
            </body>
            </html>
            """,
            greeting,
            emailConfig.getBaseUrl()
        );
    }

    /**
     * Build task reminder email template
     */
    public String buildTaskReminderEmail(String userName, String taskTitle, String taskDescription,
                                        String taskStartTime, String taskLocation) {
        String greeting = userName != null && !userName.trim().isEmpty() ? userName : "there";

        StringBuilder content = new StringBuilder(String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Task Reminder - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">📅 Task Reminder</h1>
                    <p>Hi %s,</p>
                    <p>This is a friendly reminder about your upcoming task:</p>
                    <div style="background-color: #f3f4f6; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0;">
                        <h2 style="color: #2563eb; margin-top: 0;">%s</h2>
            """, greeting, taskTitle));

        if (taskDescription != null && !taskDescription.trim().isEmpty()) {
            content.append(String.format("<p style=\"margin: 10px 0;\"><strong>Description:</strong> %s</p>", taskDescription));
        }

        if (taskStartTime != null && !taskStartTime.trim().isEmpty()) {
            content.append(String.format("<p style=\"margin: 10px 0;\"><strong>Start Time:</strong> %s</p>", taskStartTime));
        }

        if (taskLocation != null && !taskLocation.trim().isEmpty()) {
            content.append(String.format("<p style=\"margin: 10px 0;\"><strong>Location:</strong> %s</p>", taskLocation));
        }

        content.append(String.format("""
                    </div>
                    <div style="margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">View Task</a>
                    </div>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        This is an automated reminder from P-Cal.
                    </p>
                </div>
            </body>
            </html>
            """, emailConfig.getBaseUrl()));

        return content.toString();
    }

    /**
     * Build test email template
     */
    public String buildTestEmail(String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Test Email - P-Cal</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">P-Cal Test Email ✅</h1>
                    <p>This is a test email from your P-Cal instance.</p>
                    <div style="background-color: #f3f4f6; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Test Message:</strong></p>
                        <p style="margin: 10px 0 0 0;">%s</p>
                    </div>
                    <p>If you received this email, your email configuration is working correctly! 🎉</p>
                    <hr style="margin: 30px 0;">
                    <p style="font-size: 12px; color: #666;">
                        Sent at: %s
                    </p>
                </div>
            </body>
            </html>
            """,
            message != null ? message : "No message provided",
            java.time.LocalDateTime.now().toString()
        );
    }
}
