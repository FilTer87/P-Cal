package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.Task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for Task response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskResponse {
    
    private Long id;
    private String title;
    private String description;
    
    private Instant startDatetime;
    
    private Instant endDatetime;

    private String color;
    private String location;

    private String recurrenceRule;
    private Instant recurrenceEnd;

    private List<ReminderResponse> reminders = new ArrayList<>();
    
    private Long userId;
    private String userFullName;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    // Computed fields
    private Long durationMinutes;
    private Boolean isMultiDay;
    private Boolean isToday;
    private Boolean isPast;
    private Boolean isUpcoming;
    private Integer reminderCount;
    private Boolean isRecurring;
    
    // Default constructor
    public TaskResponse() {}
    
    // Constructor from Task entity
    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.startDatetime = task.getStartDatetime();
        this.endDatetime = task.getEndDatetime();
        this.color = task.getColor();
        this.location = task.getLocation();
        this.recurrenceRule = task.getRecurrenceRule();
        this.recurrenceEnd = task.getRecurrenceEnd();
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
        
        // User information
        if (task.getUser() != null) {
            this.userId = task.getUser().getId();
            this.userFullName = task.getUser().getFullName();
        }
        
        // Convert reminders
        if (task.getReminders() != null) {
            this.reminders = task.getReminders().stream()
                    .map(ReminderResponse::new)
                    .collect(Collectors.toList());
        }
        
        // Calculate computed fields
        this.calculateComputedFields();
    }
    
    // Constructor with reminders flag
    public TaskResponse(Task task, boolean includeReminders) {
        this(task);
        if (!includeReminders) {
            this.reminders = null;
        }
    }
    
    // Factory methods
    
    /**
     * Create TaskResponse from Task entity
     */
    public static TaskResponse fromTask(Task task) {
        return new TaskResponse(task);
    }
    
    /**
     * Create TaskResponse from Task entity without reminders
     */
    public static TaskResponse fromTaskWithoutReminders(Task task) {
        return new TaskResponse(task, false);
    }
    
    /**
     * Create minimal TaskResponse (for lists)
     */
    public static TaskResponse minimal(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setStartDatetime(task.getStartDatetime());
        response.setEndDatetime(task.getEndDatetime());
        response.setColor(task.getColor());
        response.calculateComputedFields();
        return response;
    }
    
    // Calculate computed fields
    private void calculateComputedFields() {
        Instant now = Instant.now();
        
        if (startDatetime != null && endDatetime != null) {
            this.durationMinutes = java.time.Duration.between(startDatetime, endDatetime).toMinutes();
            // Convert to UTC date for comparison
            java.time.LocalDate startDate = startDatetime.atZone(java.time.ZoneOffset.UTC).toLocalDate();
            java.time.LocalDate endDate = endDatetime.atZone(java.time.ZoneOffset.UTC).toLocalDate();
            java.time.LocalDate todayDate = now.atZone(java.time.ZoneOffset.UTC).toLocalDate();
            
            this.isMultiDay = !startDate.equals(endDate);
            this.isToday = startDate.equals(todayDate);
            this.isPast = endDatetime.isBefore(now);
            this.isUpcoming = startDatetime.isAfter(now);
        }

        this.reminderCount = (reminders != null) ? reminders.size() : 0;
        this.isRecurring = (recurrenceRule != null && !recurrenceRule.trim().isEmpty());
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getStartDatetime() {
        return startDatetime;
    }
    
    public void setStartDatetime(Instant startDatetime) {
        this.startDatetime = startDatetime;
        calculateComputedFields();
    }
    
    public Instant getEndDatetime() {
        return endDatetime;
    }
    
    public void setEndDatetime(Instant endDatetime) {
        this.endDatetime = endDatetime;
        calculateComputedFields();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }

    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }

    public String getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public Instant getRecurrenceEnd() {
        return recurrenceEnd;
    }

    public void setRecurrenceEnd(Instant recurrenceEnd) {
        this.recurrenceEnd = recurrenceEnd;
    }

    public List<ReminderResponse> getReminders() {
        return reminders;
    }
    
    public void setReminders(List<ReminderResponse> reminders) {
        this.reminders = reminders;
        this.reminderCount = (reminders != null) ? reminders.size() : 0;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserFullName() {
        return userFullName;
    }
    
    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Computed field getters
    public Long getDurationMinutes() {
        return durationMinutes;
    }
    
    public Boolean getIsMultiDay() {
        return isMultiDay;
    }
    
    public Boolean getIsToday() {
        return isToday;
    }
    
    public Boolean getIsPast() {
        return isPast;
    }
    
    public Boolean getIsUpcoming() {
        return isUpcoming;
    }
    
    public Integer getReminderCount() {
        return reminderCount;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    // Helper methods
    
    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        if (durationMinutes == null) return "Unknown";
        
        long hours = durationMinutes / 60;
        long minutes = durationMinutes % 60;
        
        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "m";
        }
    }
    
    /**
     * Get task status
     */
    public String getStatus() {
        if (Boolean.TRUE.equals(isPast)) return "past";
        if (Boolean.TRUE.equals(isToday)) return "today";
        if (Boolean.TRUE.equals(isUpcoming)) return "upcoming";
        return "unknown";
    }
    
    /**
     * Get task date string (UTC)
     */
    public String getTaskDateString() {
        if (startDatetime != null) {
            return startDatetime.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString();
        }
        return null;
    }
    
    /**
     * Check if task has description
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
    
    /**
     * Check if task has location
     */
    public boolean hasLocation() {
        return location != null && !location.trim().isEmpty();
    }
    
    /**
     * Check if task has reminders
     */
    public boolean hasReminders() {
        return reminders != null && !reminders.isEmpty();
    }
    
    @Override
    public String toString() {
        return "TaskResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDatetime=" + startDatetime +
                ", endDatetime=" + endDatetime +
                ", color='" + color + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", reminderCount=" + reminderCount +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}