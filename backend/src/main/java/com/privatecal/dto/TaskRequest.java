package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.time.Instant;
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
    private Instant startDatetime;
    
    @NotNull(message = "End datetime is required")
    private Instant endDatetime;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #3788d8)")
    private String color = "#3788d8";

    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    private Boolean isAllDay = false;

    @Size(max = 500, message = "Recurrence rule must be at most 500 characters")
    private String recurrenceRule;

    private Instant recurrenceEnd;

    private List<ReminderRequest> reminders = new ArrayList<>();
    
    // Default constructor
    public TaskRequest() {}
    
    // Basic constructor
    public TaskRequest(String title, Instant startDatetime, Instant endDatetime) {
        this.title = title;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
    }
    
    // Full constructor
    public TaskRequest(String title, String description, Instant startDatetime, Instant endDatetime,
                      String color, String location) {
        this.title = title;
        this.description = description;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.color = color;
        this.location = location;
    }
    
    // Validation method
    @AssertTrue(message = "End datetime must be after start datetime")
    @JsonIgnore
    public boolean isEndDatetimeAfterStartDatetime() {
        if (startDatetime == null || endDatetime == null) {
            return true; // Let @NotNull handle null validation
        }
        return endDatetime.isAfter(startDatetime);
    }

    @AssertTrue(message = "Recurrence end must be after start datetime")
    @JsonIgnore
    public boolean isRecurrenceEndAfterStartDatetime() {
        if (recurrenceEnd == null || startDatetime == null) {
            return true; // Recurrence end is optional
        }
        return recurrenceEnd.isAfter(startDatetime);
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
    
    public Instant getStartDatetime() {
        return startDatetime;
    }
    
    public void setStartDatetime(Instant startDatetime) {
        this.startDatetime = startDatetime;
    }
    
    public Instant getEndDatetime() {
        return endDatetime;
    }
    
    public void setEndDatetime(Instant endDatetime) {
        this.endDatetime = endDatetime;
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

    public Boolean getIsAllDay() {
        return isAllDay;
    }

    public void setIsAllDay(Boolean isAllDay) {
        this.isAllDay = isAllDay;
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
    @JsonIgnore
    public long getDurationInMinutes() {
        if (startDatetime != null && endDatetime != null) {
            return java.time.Duration.between(startDatetime, endDatetime).toMinutes();
        }
        return 0;
    }
    
    /**
     * Check if task spans multiple days (in UTC)
     */
    @JsonIgnore
    public boolean isMultiDay() {
        if (startDatetime != null && endDatetime != null) {
            return !startDatetime.atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    .equals(endDatetime.atZone(java.time.ZoneOffset.UTC).toLocalDate());
        }
        return false;
    }
    
    /**
     * Get task date (start date in UTC)
     */
    @JsonIgnore
    public java.time.LocalDate getTaskDate() {
        return startDatetime != null ? startDatetime.atZone(java.time.ZoneOffset.UTC).toLocalDate() : null;
    }
    
    /**
     * Check if task has reminders
     */
    @JsonIgnore
    public boolean hasReminders() {
        return reminders != null && !reminders.isEmpty();
    }
    
    /**
     * Check if this is a valid task request
     */
    @JsonIgnore
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
                ", location='" + location + '\'' +
                ", reminderCount=" + (reminders != null ? reminders.size() : 0) +
                '}';
    }
}