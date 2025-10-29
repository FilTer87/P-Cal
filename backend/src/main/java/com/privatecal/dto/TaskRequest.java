package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Task creation and update requests
 */
@Data
@NoArgsConstructor
public class TaskRequest {

    private String id;  // Task UID for updates during duplicate handling

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
    private String title;
    
    @Size(max = 2500, message = "Description must be at most 2500 characters")
    private String description;

    /**
     * Local datetime without timezone (floating time).
     * Example: "2025-10-20T15:00:00" means "3 PM" regardless of DST.
     */
    @NotNull(message = "Start datetime (local) is required")
    private LocalDateTime startDatetimeLocal;

    /**
     * Local datetime without timezone (floating time).
     * Example: "2025-10-20T16:00:00" means "4 PM" regardless of DST.
     */
    @NotNull(message = "End datetime (local) is required")
    private LocalDateTime endDatetimeLocal;

    /**
     * IANA timezone identifier (e.g., "Europe/Rome", "America/New_York").
     * Required to convert local time to UTC when needed.
     */
    @NotBlank(message = "Timezone is required")
    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #3788d8)")
    private String color = "#3788d8";

    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    private Boolean isAllDay = false;

    @Size(max = 255, message = "UID must be at most 255 characters")
    private String uid;

    @Size(max = 500, message = "Recurrence rule must be at most 500 characters")
    private String recurrenceRule;

    /**
     * Optional: End datetime for recurrence (local datetime).
     * Used when recurrence has a DATE end type.
     */
    private LocalDateTime recurrenceEnd;

    private List<ReminderRequest> reminders = new ArrayList<>();

    // Validation methods
    @AssertTrue(message = "End datetime must be after start datetime")
    @JsonIgnore
    public boolean isEndDatetimeAfterStartDatetime() {
        if (startDatetimeLocal != null && endDatetimeLocal != null) {
            return endDatetimeLocal.isAfter(startDatetimeLocal);
        }
        return true; // Let @NotNull handle null validation
    }

    @AssertTrue(message = "Recurrence end must be after start datetime")
    @JsonIgnore
    public boolean isRecurrenceEndAfterStartDatetime() {
        if (recurrenceEnd == null || startDatetimeLocal == null) {
            return true; // Recurrence end is optional
        }
        // Both are LocalDateTime now, direct comparison
        return recurrenceEnd.isAfter(startDatetimeLocal);
    }

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
        if (startDatetimeLocal != null && endDatetimeLocal != null) {
            return java.time.Duration.between(startDatetimeLocal, endDatetimeLocal).toMinutes();
        }
        return 0;
    }

    /**
     * Check if task spans multiple days (local time)
     */
    @JsonIgnore
    public boolean isMultiDay() {
        if (startDatetimeLocal != null && endDatetimeLocal != null) {
            return !startDatetimeLocal.toLocalDate().equals(endDatetimeLocal.toLocalDate());
        }
        return false;
    }

    /**
     * Get task date (start date in local time)
     */
    @JsonIgnore
    public java.time.LocalDate getTaskDate() {
        return startDatetimeLocal != null ? startDatetimeLocal.toLocalDate() : null;
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
               startDatetimeLocal != null && endDatetimeLocal != null &&
               endDatetimeLocal.isAfter(startDatetimeLocal);
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
                ", startDatetimeLocal=" + startDatetimeLocal +
                ", endDatetimeLocal=" + endDatetimeLocal +
                ", timezone='" + timezone + '\'' +
                ", color='" + color + '\'' +
                ", location='" + location + '\'' +
                ", reminderCount=" + (reminders != null ? reminders.size() : 0) +
                '}';
    }
}