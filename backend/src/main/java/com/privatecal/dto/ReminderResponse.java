package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.Reminder;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Reminder response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReminderResponse {
    
    private Long id;
    private Long taskId;
    private String taskTitle;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime reminderTime;
    
    private Integer reminderOffsetMinutes;
    private Boolean isSent;
    private Reminder.NotificationType notificationType;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    // Computed fields
    private Boolean isDue;
    private Boolean isOverdue;
    private String description;
    private Long minutesUntilDue;
    
    // Default constructor
    public ReminderResponse() {}
    
    // Constructor from Reminder entity
    public ReminderResponse(Reminder reminder) {
        this.id = reminder.getId();
        this.reminderTime = reminder.getReminderTime();
        this.reminderOffsetMinutes = reminder.getReminderOffsetMinutes();
        this.isSent = reminder.getIsSent();
        this.notificationType = reminder.getNotificationType();
        this.createdAt = reminder.getCreatedAt();
        
        // Task information
        if (reminder.getTask() != null) {
            this.taskId = reminder.getTask().getId();
            this.taskTitle = reminder.getTask().getTitle();
        }
        
        // Calculate computed fields
        this.calculateComputedFields();
        this.description = this.generateDescription();
    }
    
    // Constructor with task info flag
    public ReminderResponse(Reminder reminder, boolean includeTaskInfo) {
        this(reminder);
        if (!includeTaskInfo) {
            this.taskTitle = null;
        }
    }
    
    // Factory methods
    
    /**
     * Create ReminderResponse from Reminder entity
     */
    public static ReminderResponse fromReminder(Reminder reminder) {
        return new ReminderResponse(reminder);
    }
    
    /**
     * Create ReminderResponse from Reminder entity without task info
     */
    public static ReminderResponse fromReminderWithoutTaskInfo(Reminder reminder) {
        return new ReminderResponse(reminder, false);
    }
    
    /**
     * Create minimal ReminderResponse
     */
    public static ReminderResponse minimal(Reminder reminder) {
        ReminderResponse response = new ReminderResponse();
        response.setId(reminder.getId());
        response.setReminderTime(reminder.getReminderTime());
        response.setReminderOffsetMinutes(reminder.getReminderOffsetMinutes());
        response.setIsSent(reminder.getIsSent());
        response.setNotificationType(reminder.getNotificationType());
        response.calculateComputedFields();
        return response;
    }
    
    // Calculate computed fields
    private void calculateComputedFields() {
        LocalDateTime now = LocalDateTime.now();
        
        if (reminderTime != null) {
            this.isDue = !Boolean.TRUE.equals(isSent) && reminderTime.isBefore(now);
            this.isOverdue = !Boolean.TRUE.equals(isSent) && reminderTime.isBefore(now.minusMinutes(5));
            
            if (reminderTime.isAfter(now)) {
                this.minutesUntilDue = java.time.Duration.between(now, reminderTime).toMinutes();
            } else {
                this.minutesUntilDue = null;
            }
        }
    }
    
    // Generate human-readable description
    private String generateDescription() {
        if (reminderOffsetMinutes == null) {
            return "No description";
        }
        
        if (reminderOffsetMinutes == 0) {
            return "At event time";
        } else if (reminderOffsetMinutes < 60) {
            return reminderOffsetMinutes + " minute" + (reminderOffsetMinutes == 1 ? "" : "s") + " before";
        } else if (reminderOffsetMinutes < 24 * 60) {
            int hours = reminderOffsetMinutes / 60;
            int remainingMinutes = reminderOffsetMinutes % 60;
            String result = hours + " hour" + (hours == 1 ? "" : "s");
            if (remainingMinutes > 0) {
                result += " " + remainingMinutes + " minute" + (remainingMinutes == 1 ? "" : "s");
            }
            return result + " before";
        } else {
            int days = reminderOffsetMinutes / (24 * 60);
            int remainingHours = (reminderOffsetMinutes % (24 * 60)) / 60;
            String result = days + " day" + (days == 1 ? "" : "s");
            if (remainingHours > 0) {
                result += " " + remainingHours + " hour" + (remainingHours == 1 ? "" : "s");
            }
            return result + " before";
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskTitle() {
        return taskTitle;
    }
    
    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }
    
    public LocalDateTime getReminderTime() {
        return reminderTime;
    }
    
    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime = reminderTime;
        calculateComputedFields();
    }
    
    public Integer getReminderOffsetMinutes() {
        return reminderOffsetMinutes;
    }
    
    public void setReminderOffsetMinutes(Integer reminderOffsetMinutes) {
        this.reminderOffsetMinutes = reminderOffsetMinutes;
        this.description = generateDescription();
    }
    
    public Boolean getIsSent() {
        return isSent;
    }
    
    public void setIsSent(Boolean sent) {
        isSent = sent;
        calculateComputedFields();
    }
    
    public Reminder.NotificationType getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(Reminder.NotificationType notificationType) {
        this.notificationType = notificationType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Computed field getters
    public Boolean getIsDue() {
        return isDue;
    }
    
    public Boolean getIsOverdue() {
        return isOverdue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Long getMinutesUntilDue() {
        return minutesUntilDue;
    }
    
    // Helper methods
    
    /**
     * Get reminder status
     */
    public String getStatus() {
        if (Boolean.TRUE.equals(isSent)) return "sent";
        if (Boolean.TRUE.equals(isOverdue)) return "overdue";
        if (Boolean.TRUE.equals(isDue)) return "due";
        return "pending";
    }
    
    /**
     * Get formatted time until due
     */
    public String getFormattedTimeUntilDue() {
        if (minutesUntilDue == null || minutesUntilDue <= 0) {
            return "Past due";
        }
        
        if (minutesUntilDue < 60) {
            return minutesUntilDue + " minute" + (minutesUntilDue == 1 ? "" : "s");
        } else if (minutesUntilDue < 24 * 60) {
            long hours = minutesUntilDue / 60;
            return hours + " hour" + (hours == 1 ? "" : "s");
        } else {
            long days = minutesUntilDue / (24 * 60);
            return days + " day" + (days == 1 ? "" : "s");
        }
    }
    
    /**
     * Check if notification type is push
     */
    public boolean isPushNotification() {
        return notificationType == Reminder.NotificationType.PUSH;
    }
    
    /**
     * Check if notification type is email
     */
    public boolean isEmailNotification() {
        return notificationType == Reminder.NotificationType.EMAIL;
    }
    
    /**
     * Check if reminder is active (not sent and not overdue)
     */
    public boolean isActive() {
        return !Boolean.TRUE.equals(isSent) && !Boolean.TRUE.equals(isOverdue);
    }
    
    @Override
    public String toString() {
        return "ReminderResponse{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", reminderTime=" + reminderTime +
                ", reminderOffsetMinutes=" + reminderOffsetMinutes +
                ", isSent=" + isSent +
                ", notificationType=" + notificationType +
                ", status='" + getStatus() + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}