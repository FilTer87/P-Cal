package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.Task;

import java.time.LocalDateTime;
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
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startDatetime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endDatetime;
    
    private String color;
    private Boolean isAllDay;
    private String location;
    
    private List<ReminderResponse> reminders = new ArrayList<>();
    
    private Long userId;
    private String userFullName;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Long durationMinutes;
    private Boolean isMultiDay;
    private Boolean isToday;
    private Boolean isPast;
    private Boolean isUpcoming;
    private Integer reminderCount;
    
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
        this.isAllDay = task.getIsAllDay();
        this.location = task.getLocation();
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
        response.setIsAllDay(task.getIsAllDay());
        response.calculateComputedFields();
        return response;
    }
    
    // Calculate computed fields
    private void calculateComputedFields() {
        LocalDateTime now = LocalDateTime.now();
        
        if (startDatetime != null && endDatetime != null) {
            this.durationMinutes = java.time.Duration.between(startDatetime, endDatetime).toMinutes();
            this.isMultiDay = !startDatetime.toLocalDate().equals(endDatetime.toLocalDate());
            this.isToday = startDatetime.toLocalDate().equals(now.toLocalDate());
            this.isPast = endDatetime.isBefore(now);
            this.isUpcoming = startDatetime.isAfter(now);
        }
        
        this.reminderCount = (reminders != null) ? reminders.size() : 0;
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
    
    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }
    
    public void setStartDatetime(LocalDateTime startDatetime) {
        this.startDatetime = startDatetime;
        calculateComputedFields();
    }
    
    public LocalDateTime getEndDatetime() {
        return endDatetime;
    }
    
    public void setEndDatetime(LocalDateTime endDatetime) {
        this.endDatetime = endDatetime;
        calculateComputedFields();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public Boolean getIsAllDay() {
        return isAllDay;
    }
    
    public void setIsAllDay(Boolean allDay) {
        isAllDay = allDay;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
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
     * Get task date string
     */
    public String getTaskDateString() {
        if (startDatetime != null) {
            return startDatetime.toLocalDate().toString();
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
                ", isAllDay=" + isAllDay +
                ", durationMinutes=" + durationMinutes +
                ", reminderCount=" + reminderCount +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}