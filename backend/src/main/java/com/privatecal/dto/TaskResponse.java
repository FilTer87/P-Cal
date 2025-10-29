package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.Task;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for Task response
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskResponse {

    private String id; // Task UID (primary key)
    private String occurrenceId; // Unique identifier for recurring task occurrences (format: "taskUid-timestamp")
    private String title;
    private String description;

    /**
     * Local datetime without timezone (floating time).
     * Example: "2025-10-20T15:00:00" means "3 PM" regardless of DST.
     * Frontend should display this directly without conversion.
     */
    private LocalDateTime startDatetimeLocal;

    /**
     * Local datetime without timezone (floating time).
     * Example: "2025-10-20T16:00:00" means "4 PM" regardless of DST.
     * Frontend should display this directly without conversion.
     */
    private LocalDateTime endDatetimeLocal;

    /**
     * IANA timezone identifier (e.g., "Europe/Rome", "America/New_York").
     * Frontend can use this for timezone display but should NOT convert the local times.
     */
    private String timezone;

    private String color;
    private String location;
    private Boolean isAllDay;

    private String recurrenceRule;

    /**
     * Optional: End datetime for recurrence (local datetime string).
     * Converted from Instant in entity to LocalDateTime string for frontend.
     */
    private String recurrenceEnd;

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
    
    // Constructor from Task entity
    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();

        // Floating time fields
        this.startDatetimeLocal = task.getStartDatetimeLocal();
        this.endDatetimeLocal = task.getEndDatetimeLocal();
        this.timezone = task.getTaskTimezone();

        this.color = task.getColor();
        this.location = task.getLocation();
        this.isAllDay = task.getIsAllDay();
        this.recurrenceRule = task.getRecurrenceRule();

        // Convert recurrenceEnd from Instant to LocalDateTime string for frontend
        if (task.getRecurrenceEnd() != null && task.getTaskTimezone() != null) {
            this.recurrenceEnd = task.getRecurrenceEnd()
                .atZone(java.time.ZoneId.of(task.getTaskTimezone()))
                .toLocalDateTime()
                .toString();
        } else {
            this.recurrenceEnd = null;
        }

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
        response.setStartDatetimeLocal(task.getStartDatetimeLocal());
        response.setEndDatetimeLocal(task.getEndDatetimeLocal());
        response.setTimezone(task.getTaskTimezone());
        response.setColor(task.getColor());
        response.calculateComputedFields();
        return response;
    }
    
    // Calculate computed fields
    private void calculateComputedFields() {
        Instant now = Instant.now();

        if (startDatetimeLocal != null && endDatetimeLocal != null && timezone != null) {
            // Calculate duration from local times
            this.durationMinutes = java.time.Duration.between(startDatetimeLocal, endDatetimeLocal).toMinutes();

            // Convert to Instant for temporal comparisons
            java.time.ZoneId zoneId = java.time.ZoneId.of(timezone);
            Instant startInstant = startDatetimeLocal.atZone(zoneId).toInstant();
            Instant endInstant = endDatetimeLocal.atZone(zoneId).toInstant();

            // Use local dates for day comparison
            java.time.LocalDate startDate = startDatetimeLocal.toLocalDate();
            java.time.LocalDate endDate = endDatetimeLocal.toLocalDate();
            java.time.LocalDate todayDate = now.atZone(zoneId).toLocalDate();

            this.isMultiDay = !startDate.equals(endDate);
            this.isToday = startDate.equals(todayDate);
            this.isPast = endInstant.isBefore(now);
            this.isUpcoming = startInstant.isAfter(now);
        }

        this.reminderCount = (reminders != null) ? reminders.size() : 0;
        this.isRecurring = (recurrenceRule != null && !recurrenceRule.trim().isEmpty());
    }

    // Helper methods (Lombok generates all getters/setters)
    
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
     * Get task date string (local date)
     */
    public String getTaskDateString() {
        if (startDatetimeLocal != null) {
            return startDatetimeLocal.toLocalDate().toString();
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
                ", startDatetimeLocal=" + startDatetimeLocal +
                ", endDatetimeLocal=" + endDatetimeLocal +
                ", timezone='" + timezone + '\'' +
                ", color='" + color + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", reminderCount=" + reminderCount +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}