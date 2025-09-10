package com.privatecal.service;

import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending notifications via NTFY and email
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Value("${app.ntfy.server-url}")
    private String ntfyServerUrl;
    
    @Value("${app.ntfy.topic-prefix}")
    private String topicPrefix;
    
    @Value("${app.ntfy.enabled}")
    private boolean ntfyEnabled;
    
    private final RestTemplate restTemplate;
    
    public NotificationService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Send push notification via NTFY
     */
    @Async
    public void sendPushNotification(Reminder reminder) {
        if (!ntfyEnabled) {
            logger.debug("NTFY notifications are disabled");
            return;
        }
        
        try {
            Task task = reminder.getTask();
            User user = task.getUser();
            
            logger.debug("Sending push notification for task: {} to user: {}", 
                        task.getTitle(), user.getUsername());
            
            // Create NTFY topic for user
            String topic = topicPrefix + user.getId();
            String ntfyUrl = ntfyServerUrl + "/" + topic;
            
            // Prepare notification content
            String title = "Task Reminder";
            String message = createNotificationMessage(task, reminder);
            String priority = determinePriority(reminder);
            
            // Create NTFY payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("topic", topic);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("priority", priority);
            payload.put("tags", "calendar,reminder");
            
            // Add action buttons
            payload.put("actions", createNotificationActions(task));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Title", title);
            headers.set("X-Priority", priority);
            headers.set("X-Tags", "calendar,reminder");
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // Send notification
            ResponseEntity<String> response = restTemplate.exchange(
                ntfyUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Push notification sent successfully for task: {} to user: {}", 
                           task.getTitle(), user.getUsername());
            } else {
                logger.warn("Failed to send push notification. Status: {}, Response: {}", 
                           response.getStatusCode(), response.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Error sending push notification for reminder ID: {}", 
                        reminder.getId(), e);
        }
    }
    
    /**
     * Send email notification (placeholder - would integrate with email service)
     */
    @Async
    public void sendEmailNotification(Reminder reminder) {
        try {
            Task task = reminder.getTask();
            User user = task.getUser();
            
            logger.info("Email notification requested for task: {} to user: {}", 
                       task.getTitle(), user.getEmail());
            
            // TODO: Implement email sending using JavaMailSender or external email service
            // For now, we'll just log it
            String subject = "Task Reminder: " + task.getTitle();
            String body = createEmailBody(task, reminder, user);
            
            logger.info("Would send email to: {} with subject: {}", user.getEmail(), subject);
            logger.debug("Email body: {}", body);
            
            // Simulate email sending delay
            Thread.sleep(1000);
            
            logger.info("Email notification processed for task: {}", task.getTitle());
            
        } catch (Exception e) {
            logger.error("Error sending email notification for reminder ID: {}", 
                        reminder.getId(), e);
        }
    }
    
    /**
     * Send test notification to user
     */
    public void sendTestNotification(Long userId, String message) {
        if (!ntfyEnabled) {
            logger.debug("NTFY notifications are disabled - cannot send test notification");
            return;
        }
        
        try {
            String topic = topicPrefix + userId;
            String ntfyUrl = ntfyServerUrl + "/" + topic;
            
            // Create simple test payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("topic", topic);
            payload.put("title", "PrivateCal Test");
            payload.put("message", message);
            payload.put("priority", "default");
            payload.put("tags", "test");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                ntfyUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Test notification sent successfully to user ID: {}", userId);
            } else {
                logger.warn("Failed to send test notification. Status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error sending test notification to user ID: {}", userId, e);
        }
    }
    
    /**
     * Get NTFY topic for user (so they can subscribe)
     */
    public String getNtfyTopicForUser(Long userId) {
        return topicPrefix + userId;
    }
    
    /**
     * Get NTFY subscription URL for user
     */
    public String getNtfySubscriptionUrl(Long userId) {
        return ntfyServerUrl + "/" + getNtfyTopicForUser(userId);
    }
    
    /**
     * Check if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        return ntfyEnabled;
    }
    
    /**
     * Create notification message for task reminder
     */
    private String createNotificationMessage(Task task, Reminder reminder) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
        String startTime = task.getStartDatetime().format(formatter);
        
        StringBuilder message = new StringBuilder();
        message.append("Reminder: ").append(task.getTitle());
        message.append("\n\nScheduled for: ").append(startTime);
        
        if (task.getLocation() != null && !task.getLocation().trim().isEmpty()) {
            message.append("\nLocation: ").append(task.getLocation());
        }
        
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            String description = task.getDescription();
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            message.append("\nDescription: ").append(description);
        }
        
        long minutesUntil = java.time.Duration.between(
            java.time.LocalDateTime.now(), 
            task.getStartDatetime()
        ).toMinutes();
        
        if (minutesUntil > 0) {
            if (minutesUntil < 60) {
                message.append("\n\nStarts in ").append(minutesUntil).append(" minute").append(minutesUntil == 1 ? "" : "s");
            } else {
                long hours = minutesUntil / 60;
                message.append("\n\nStarts in ").append(hours).append(" hour").append(hours == 1 ? "" : "s");
            }
        } else {
            message.append("\n\nStarting now!");
        }
        
        return message.toString();
    }
    
    /**
     * Create email body for task reminder
     */
    private String createEmailBody(Task task, Reminder reminder, User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm");
        String startTime = task.getStartDatetime().format(formatter);
        
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(user.getFullName() != null ? user.getFullName() : user.getUsername()).append(",\n\n");
        body.append("This is a reminder for your upcoming task:\n\n");
        body.append("Task: ").append(task.getTitle()).append("\n");
        body.append("Scheduled: ").append(startTime).append("\n");
        
        if (task.getLocation() != null && !task.getLocation().trim().isEmpty()) {
            body.append("Location: ").append(task.getLocation()).append("\n");
        }
        
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            body.append("Description: ").append(task.getDescription()).append("\n");
        }
        
        long minutesUntil = java.time.Duration.between(
            java.time.LocalDateTime.now(), 
            task.getStartDatetime()
        ).toMinutes();
        
        if (minutesUntil > 0) {
            body.append("\nThis task starts in ").append(minutesUntil).append(" minutes.\n");
        } else {
            body.append("\nThis task is starting now!\n");
        }
        
        body.append("\nBest regards,\n");
        body.append("PrivateCal Team");
        
        return body.toString();
    }
    
    /**
     * Determine notification priority based on how soon the task starts
     */
    private String determinePriority(Reminder reminder) {
        Task task = reminder.getTask();
        long minutesUntil = java.time.Duration.between(
            java.time.LocalDateTime.now(), 
            task.getStartDatetime()
        ).toMinutes();
        
        if (minutesUntil <= 5) {
            return "urgent";
        } else if (minutesUntil <= 15) {
            return "high";
        } else if (minutesUntil <= 60) {
            return "default";
        } else {
            return "low";
        }
    }
    
    /**
     * Create notification action buttons
     */
    private Object[] createNotificationActions(Task task) {
        return new Object[]{
            Map.of(
                "action", "view",
                "label", "View Task",
                "url", "https://privatecal.example.com/tasks/" + task.getId() // Update with actual frontend URL
            ),
            Map.of(
                "action", "http",
                "label", "Mark as Done",
                "url", "https://api.privatecal.example.com/api/tasks/" + task.getId() + "/complete", // Update with actual API URL
                "method", "POST"
            )
        };
    }
    
    /**
     * Send notification for task creation
     */
    @Async
    public void sendTaskCreatedNotification(Task task) {
        if (!ntfyEnabled) return;
        
        try {
            User user = task.getUser();
            String topic = topicPrefix + user.getId();
            String ntfyUrl = ntfyServerUrl + "/" + topic;
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm");
            String startTime = task.getStartDatetime().format(formatter);
            
            String message = "New task created: " + task.getTitle() + "\nScheduled for: " + startTime;
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("topic", topic);
            payload.put("title", "Task Created");
            payload.put("message", message);
            payload.put("priority", "low");
            payload.put("tags", "calendar,created");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            restTemplate.exchange(ntfyUrl, HttpMethod.POST, requestEntity, String.class);
            
            logger.debug("Task creation notification sent for: {}", task.getTitle());
            
        } catch (Exception e) {
            logger.error("Error sending task creation notification", e);
        }
    }
    
    /**
     * Send notification for task updates
     */
    @Async
    public void sendTaskUpdatedNotification(Task task) {
        if (!ntfyEnabled) return;
        
        try {
            User user = task.getUser();
            String topic = topicPrefix + user.getId();
            String ntfyUrl = ntfyServerUrl + "/" + topic;
            
            String message = "Task updated: " + task.getTitle();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("topic", topic);
            payload.put("title", "Task Updated");
            payload.put("message", message);
            payload.put("priority", "low");
            payload.put("tags", "calendar,updated");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            restTemplate.exchange(ntfyUrl, HttpMethod.POST, requestEntity, String.class);
            
            logger.debug("Task update notification sent for: {}", task.getTitle());
            
        } catch (Exception e) {
            logger.error("Error sending task update notification", e);
        }
    }
}