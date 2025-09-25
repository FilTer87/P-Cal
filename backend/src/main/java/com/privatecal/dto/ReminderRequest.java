package com.privatecal.dto;

import com.privatecal.entity.Reminder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for Reminder creation and update requests
 */
public class ReminderRequest {
    
    @NotNull(message = "Reminder offset minutes is required")
    @Min(value = 0, message = "Reminder offset minutes must be non-negative")
    private Integer reminderOffsetMinutes;
    
    private NotificationType notificationType = NotificationType.PUSH;
    
    // Default constructor
    public ReminderRequest() {}
    
    // Constructor with offset
    public ReminderRequest(Integer reminderOffsetMinutes) {
        this.reminderOffsetMinutes = reminderOffsetMinutes;
    }
    
    // Constructor with offset and notification type
    public ReminderRequest(Integer reminderOffsetMinutes, NotificationType notificationType) {
        this.reminderOffsetMinutes = reminderOffsetMinutes;
        this.notificationType = notificationType;
    }
    
    // Factory methods for common reminder times
    
    /**
     * Create reminder for 5 minutes before
     */
    public static ReminderRequest fiveMinutesBefore() {
        return new ReminderRequest(5);
    }
    
    /**
     * Create reminder for 10 minutes before
     */
    public static ReminderRequest tenMinutesBefore() {
        return new ReminderRequest(10);
    }
    
    /**
     * Create reminder for 15 minutes before
     */
    public static ReminderRequest fifteenMinutesBefore() {
        return new ReminderRequest(15);
    }
    
    /**
     * Create reminder for 30 minutes before
     */
    public static ReminderRequest thirtyMinutesBefore() {
        return new ReminderRequest(30);
    }
    
    /**
     * Create reminder for 1 hour before
     */
    public static ReminderRequest oneHourBefore() {
        return new ReminderRequest(60);
    }
    
    /**
     * Create reminder for 1 day before
     */
    public static ReminderRequest oneDayBefore() {
        return new ReminderRequest(24 * 60);
    }
    
    /**
     * Create reminder for 1 week before
     */
    public static ReminderRequest oneWeekBefore() {
        return new ReminderRequest(7 * 24 * 60);
    }
    
    // Getters and Setters
    public Integer getReminderOffsetMinutes() {
        return reminderOffsetMinutes;
    }
    
    public void setReminderOffsetMinutes(Integer reminderOffsetMinutes) {
        this.reminderOffsetMinutes = reminderOffsetMinutes;
    }
    
    public NotificationType getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
    
    // Helper methods
    
    /**
     * Get reminder offset in hours
     */
    public double getReminderOffsetHours() {
        return reminderOffsetMinutes != null ? reminderOffsetMinutes / 60.0 : 0;
    }
    
    /**
     * Get reminder offset in days
     */
    public double getReminderOffsetDays() {
        return reminderOffsetMinutes != null ? reminderOffsetMinutes / (60.0 * 24) : 0;
    }
    
    /**
     * Get human-readable reminder description
     */
    public String getDescription() {
        if (reminderOffsetMinutes == null) {
            return "No reminder";
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
    
    /**
     * Check if this is a valid reminder request
     */
    public boolean isValid() {
        return reminderOffsetMinutes != null && reminderOffsetMinutes >= 0;
    }
    
    /**
     * Check if notification type is push
     */
    public boolean isPushNotification() {
        return notificationType == NotificationType.PUSH;
    }
    
    /**
     * Check if notification type is email
     */
    public boolean isEmailNotification() {
        return notificationType == NotificationType.EMAIL;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReminderRequest that)) return false;
        
        if (!reminderOffsetMinutes.equals(that.reminderOffsetMinutes)) return false;
        return notificationType == that.notificationType;
    }
    
    @Override
    public int hashCode() {
        int result = reminderOffsetMinutes.hashCode();
        result = 31 * result + (notificationType != null ? notificationType.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "ReminderRequest{" +
                "reminderOffsetMinutes=" + reminderOffsetMinutes +
                ", notificationType=" + notificationType +
                ", description='" + getDescription() + '\'' +
                '}';
    }
}