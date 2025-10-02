# 📧 Email Configuration Guide

This guide explains how to configure email functionality for P-Cal backend.

## 📋 Prerequisites

- P-Cal backend application
- SMTP server access (Gmail recommended for development)
- Java Spring Boot 3.2.0+ with `spring-boot-starter-mail`

## 🚀 Quick Setup

### 1. Configure Environment Variables

Copy `.env.example` to `.env` and configure the following email variables:

```bash
# Enable email functionality
EMAIL_ENABLED=true

# SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password

# Email Settings
EMAIL_FROM_ADDRESS=noreply@p-cal.me
EMAIL_FROM_NAME=P-Cal
APP_BASE_URL=http://localhost:3000
```

### 2. Gmail Setup (Recommended for Development)

1. **Enable 2-Factor Authentication** on your Gmail account
2. Go to: Google Account > Security > 2-Step Verification > App passwords
3. Generate an **app-specific password** for "Mail"
4. Use your Gmail address as `MAIL_USERNAME`
5. Use the 16-character app password as `MAIL_PASSWORD`

#### Example Gmail Configuration:
```bash
EMAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.email@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
EMAIL_FROM_ADDRESS=your.email@gmail.com
EMAIL_FROM_NAME=Your Name
```

## 🔧 Alternative SMTP Providers

### Outlook/Hotmail
```bash
MAIL_HOST=smtp-mail.outlook.com
MAIL_PORT=587
MAIL_USERNAME=your-email@outlook.com
MAIL_PASSWORD=your-password
```

### Yahoo Mail
```bash
MAIL_HOST=smtp.mail.yahoo.com
MAIL_PORT=587
MAIL_USERNAME=your-email@yahoo.com
MAIL_PASSWORD=your-app-password
```

### Custom SMTP Server
```bash
MAIL_HOST=mail.yourdomain.com
MAIL_PORT=587
MAIL_USERNAME=your-username
MAIL_PASSWORD=your-password
```

## 🧪 Testing Email Configuration

### Via API Endpoints

#### 1. Check Email Service Status
```bash
GET /api/auth/email-status
Authorization: Bearer <your-jwt-token>
```

Response:
```json
{
  "available": true,
  "configuration": "Email Service - Enabled: true, From: P-Cal <noreply@p-cal.me>, Base URL: http://localhost:3000",
  "message": "Email service is available"
}
```

#### 2. Send Test Email
```bash
POST /api/auth/test-email
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "message": "Custom test message"
}
```

Response:
```json
{
  "success": true,
  "message": "Test email sent successfully to user@example.com",
  "recipient": "user@example.com"
}
```

### Via Application

1. Start the application with email configuration
2. Login to get JWT token
3. Call the test endpoints above
4. Check your inbox for test emails

## 📧 Email Features

### Supported Email Types

1. **Task Reminder Emails**
   - Beautiful HTML templates
   - Task details (title, time, location, description)
   - Responsive design

2. **Welcome Emails**
   - Sent automatically on user registration
   - Branded P-Cal template

3. **Test Emails**
   - For configuration verification
   - Custom message support

### Email Templates

All emails use modern HTML templates with:
- Responsive design
- Professional styling
- P-Cal branding
- Call-to-action buttons

## ⚙️ Configuration Details

### Application Properties

The following properties are automatically configured from environment variables:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

app:
  email:
    enabled: ${EMAIL_ENABLED:false}
    from-address: ${EMAIL_FROM_ADDRESS:noreply@p-cal.me}
    from-name: ${EMAIL_FROM_NAME:P-Cal}
    base-url: ${APP_BASE_URL:http://localhost:3000}
```

### Java Configuration Classes

- **EmailConfig**: Configuration properties class
- **EmailService**: Core email sending service
- **EmailNotificationProvider**: Integrates with notification system

## 🔒 Security Considerations

1. **Never commit real credentials** to version control
2. Use **app-specific passwords** for Gmail (not your regular password)
3. Enable **2FA** on your email account
4. Use **environment variables** for all sensitive configuration
5. Consider using **OAuth2** for production Gmail integration

## 🐛 Troubleshooting

### Common Issues

#### "Email service is not available"
- Check `EMAIL_ENABLED=true` in your `.env` file
- Verify SMTP credentials are correct
- Test SMTP connection manually

#### "Authentication failed"
- For Gmail: Use app-specific password, not regular password
- Verify 2FA is enabled on Gmail account
- Check username/password are correct

#### "Connection timeout"
- Check SMTP host and port
- Verify firewall allows outbound connections on port 587
- Try port 465 (SSL) if 587 (TLS) doesn't work

#### "SSL/TLS errors"
- Ensure `starttls.enable=true` in configuration
- For port 465, use SSL instead of STARTTLS
- Check SMTP provider requirements

### Debug Mode

Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.privatecal.service.EmailService: DEBUG
    org.springframework.mail: DEBUG
```

## 📈 Production Setup

### Recommendations for Production

1. **Use dedicated email service** (SendGrid, AWS SES, etc.)
2. **Configure proper DNS records** (SPF, DKIM, DMARC)
3. **Set up email monitoring** and bounce handling
4. **Use secure email templates** stored externally
5. **Implement rate limiting** for email sending
6. **Configure proper error handling** and retry logic

### Environment-Specific Configuration

Create separate configuration files:
- `.env.development` - Development settings
- `.env.staging` - Staging environment
- `.env.production` - Production settings

## 🚀 Next Steps

1. Configure your `.env` file with SMTP settings
2. Test email functionality using the provided endpoints
3. Customize email templates as needed
4. Set up monitoring for email delivery
