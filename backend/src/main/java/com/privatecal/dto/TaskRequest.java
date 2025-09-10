package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Task creation and update requests
 */
public class TaskRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
    private String title;
    
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
    
    @NotNull(message = "Start datetime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startDatetime;
    
    @NotNull(message = "End datetime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endDatetime;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #3788d8)")
    private String color = "#3788d8";
    
    private Boolean isAllDay = false;
    
    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;
    
    private List<ReminderRequest> reminders = new ArrayList<>();
    
    // Default constructor
    public TaskRequest() {}
    
    // Basic constructor
    public TaskRequest(String title, LocalDateTime startDatetime, LocalDateTime endDatetime) {
        this.title = title;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
    }
    
    // Full constructor
    public TaskRequest(String title, String description, LocalDateTime startDatetime, LocalDateTime endDatetime,
                      String color, Boolean isAllDay, String location) {
        this.title = title;
        this.description = description;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.color = color;
        this.isAllDay = isAllDay;
        this.location = location;
    }
    
    // Validation method
    @AssertTrue(message = "End datetime must be after start datetime")
    public boolean isEndDatetimeAfterStartDatetime() {
        if (startDatetime == null || endDatetime == null) {
            return true; // Let @NotNull handle null validation
        }
        return endDatetime.isAfter(startDatetime);
    }
    
    @AssertTrue(message = "All-day tasks must start and end on the same date")
    public boolean isValidAllDayTask() {
        if (!Boolean.TRUE.equals(isAllDay) || startDatetime == null || endDatetime == null) {
            return true;
        }
        return startDatetime.toLocalDate().equals(endDatetime.toLocalDate());
    }
    
    // Getters and Setters
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
    }
    
    public LocalDateTime getEndDatetime() {
        return endDatetime;
    }
    
    public void setEndDatetime(LocalDateTime endDatetime) {
        this.endDatetime = endDatetime;
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
    
    public List<ReminderRequest> getReminders() {
        return reminders;
    }
    
    public void setReminders(List<ReminderRequest> reminders) {
        this.reminders = reminders != null ? reminders : new ArrayList<>();
    }
    
    // Helper methods
    
    /**
     * Add a reminder to the task
     */
    public void addReminder(ReminderRequest reminder) {
        if (this.reminders == null) {
            this.reminders = new ArrayList<>();
        }
        this.reminders.add(reminder);
    }
    
    /**
     * Get task duration in minutes
     */
    public long getDurationInMinutes() {
        if (startDatetime != null && endDatetime != null) {
            return java.time.Duration.between(startDatetime, endDatetime).toMinutes();
        }
        return 0;
    }
    
    /**
     * Check if task spans multiple days
     */
    public boolean isMultiDay() {
        if (startDatetime != null && endDatetime != null) {
            return !startDatetime.toLocalDate().equals(endDatetime.toLocalDate());
        }
        return false;
    }
    
    /**
     * Get task date (start date)
     */
    public java.time.LocalDate getTaskDate() {
        return startDatetime != null ? startDatetime.toLocalDate() : null;
    }
    
    /**
     * Check if task has reminders
     */
    public boolean hasReminders() {
        return reminders != null && !reminders.isEmpty();
    }
    
    /**
     * Check if this is a valid task request
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() &&
               startDatetime != null && endDatetime != null &&
               endDatetime.isAfter(startDatetime);
    }
    
    /**
     * Clean up the request (trim strings, validate color, etc.)
     */
    public void clean() {
        if (title != null) {
            title = title.trim();
        }
        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            }
        }
        if (location != null) {
            location = location.trim();
            if (location.isEmpty()) {
                location = null;
            }
        }
        if (color != null) {
            color = color.trim().toLowerCase();
        }
        if (isAllDay == null) {
            isAllDay = false;
        }
    }
    
    @Override
    public String toString() {
        return "TaskRequest{" +
                "title='" + title + '\'' +
                ", description='" + (description != null && description.length() > 50 ? 
                    description.substring(0, 50) + "..." : description) + '\'' +
                ", startDatetime=" + startDatetime +
                ", endDatetime=" + endDatetime +
                ", color='" + color + '\'' +
                ", isAllDay=" + isAllDay +
                ", location='" + location + '\'' +
                ", reminderCount=" + (reminders != null ? reminders.size() : 0) +
                '}';
    }
}