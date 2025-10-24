package com.privatecal.service.notification;

import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.entity.Reminder;

import java.time.Instant;

/**
 * Data transfer object containing all information needed to send a notification
 * This class aggregates data from Task, User, and Reminder entities
 */
public class NotificationData {

    private final Long userId;
    private final String userEmail;
    private final String userFullName;
    private final String userNtfyTopic;
    private final String userTimezone;

    private final String taskId; // Task UID
    private final String taskTitle;
    private final String taskDescription;
    private final String taskLocation;
    private final Instant taskStartTime;
    private final Instant taskEndTime;

    private final Long reminderId;
    private final Instant reminderTime;
    private final Integer reminderOffsetMinutes;

    private NotificationData(Builder builder) {
        this.userId = builder.userId;
        this.userEmail = builder.userEmail;
        this.userFullName = builder.userFullName;
        this.userNtfyTopic = builder.userNtfyTopic;
        this.userTimezone = builder.userTimezone;
        this.taskId = builder.taskId;
        this.taskTitle = builder.taskTitle;
        this.taskDescription = builder.taskDescription;
        this.taskLocation = builder.taskLocation;
        this.taskStartTime = builder.taskStartTime;
        this.taskEndTime = builder.taskEndTime;
        this.reminderId = builder.reminderId;
        this.reminderTime = builder.reminderTime;
        this.reminderOffsetMinutes = builder.reminderOffsetMinutes;
    }

    /**
     * Create NotificationData from Reminder entity
     * This method extracts all necessary data from the reminder and its related entities
     * For recurring tasks, calculates the actual occurrence time from reminderTime + offset
     */
    public static NotificationData fromReminder(Reminder reminder) {
        Task task = reminder.getTask();
        User user = task.getUser();

        // Calculate actual occurrence start time
        // For recurring tasks, reminderTime is set to the next occurrence minus offset
        // So: reminderTime + offset = actual occurrence start time
        Instant actualStartTime = reminder.getReminderTime()
                .plus(java.time.Duration.ofMinutes(reminder.getReminderOffsetMinutes()));

        // Calculate actual occurrence end time based on original task duration
        long taskDurationMillis = java.time.Duration.between(
                task.getStartDatetime(),
                task.getEndDatetime()
        ).toMillis();
        Instant actualEndTime = actualStartTime.plusMillis(taskDurationMillis);

        return new Builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getFullName())
                .userNtfyTopic(user.getNtfyTopic())
                .userTimezone(user.getTimezone())
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .taskDescription(task.getDescription())
                .taskLocation(task.getLocation())
                .taskStartTime(actualStartTime)
                .taskEndTime(actualEndTime)
                .reminderId(reminder.getId())
                .reminderTime(reminder.getReminderTime())
                .reminderOffsetMinutes(reminder.getReminderOffsetMinutes())
                .build();
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getUserFullName() { return userFullName; }
    public String getUserNtfyTopic() { return userNtfyTopic; }
    public String getUserTimezone() { return userTimezone; }
    public String getTaskId() { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public String getTaskDescription() { return taskDescription; }
    public String getTaskLocation() { return taskLocation; }
    public Instant getTaskStartTime() { return taskStartTime; }
    public Instant getTaskEndTime() { return taskEndTime; }
    public Long getReminderId() { return reminderId; }
    public Instant getReminderTime() { return reminderTime; }
    public Integer getReminderOffsetMinutes() { return reminderOffsetMinutes; }

    /**
     * Calculate minutes until task starts
     */
    public long getMinutesUntilTaskStart() {
        if (taskStartTime == null) return 0;
        return java.time.Duration.between(Instant.now(), taskStartTime).toMinutes();
    }

    /**
     * Get formatted time until task starts (e.g., "15 minutes", "2 hours")
     */
    public String getFormattedTimeUntilStart() {
        long minutes = getMinutesUntilTaskStart();

        if (minutes <= 0) {
            return "now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        } else if (minutes < 24 * 60) {
            long hours = minutes / 60;
            return hours + " hour" + (hours == 1 ? "" : "s");
        } else {
            long days = minutes / (24 * 60);
            return days + " day" + (days == 1 ? "" : "s");
        }
    }

    // Builder pattern for flexible construction
    public static class Builder {
        private Long userId;
        private String userEmail;
        private String userFullName;
        private String userNtfyTopic;
        private String userTimezone;
        private String taskId;
        private String taskTitle;
        private String taskDescription;
        private String taskLocation;
        private Instant taskStartTime;
        private Instant taskEndTime;
        private Long reminderId;
        private Instant reminderTime;
        private Integer reminderOffsetMinutes;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder userEmail(String userEmail) { this.userEmail = userEmail; return this; }
        public Builder userFullName(String userFullName) { this.userFullName = userFullName; return this; }
        public Builder userNtfyTopic(String userNtfyTopic) { this.userNtfyTopic = userNtfyTopic; return this; }
        public Builder userTimezone(String userTimezone) { this.userTimezone = userTimezone; return this; }
        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder taskTitle(String taskTitle) { this.taskTitle = taskTitle; return this; }
        public Builder taskDescription(String taskDescription) { this.taskDescription = taskDescription; return this; }
        public Builder taskLocation(String taskLocation) { this.taskLocation = taskLocation; return this; }
        public Builder taskStartTime(Instant taskStartTime) { this.taskStartTime = taskStartTime; return this; }
        public Builder taskEndTime(Instant taskEndTime) { this.taskEndTime = taskEndTime; return this; }
        public Builder reminderId(Long reminderId) { this.reminderId = reminderId; return this; }
        public Builder reminderTime(Instant reminderTime) { this.reminderTime = reminderTime; return this; }
        public Builder reminderOffsetMinutes(Integer reminderOffsetMinutes) { this.reminderOffsetMinutes = reminderOffsetMinutes; return this; }

        public NotificationData build() {
            return new NotificationData(this);
        }
    }
}