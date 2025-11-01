package com.privatecal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@EntityListeners(AuditingEntityListener.class)
public class Task {

    /**
     * UID is the primary key (CalDAV RFC 4791 compliance)
     * Format: iCalendar UID (RFC 5545) - ensures stable CalDAV URLs
     * Example: "privatecal-user123-20241023T120000Z" or "uuid@domain.com"
     */
    @Id
    @NotBlank
    @Size(max = 255)
    @Column(name = "uid", length = 255, nullable = false)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    @NotNull(message = "Calendar is required")
    private Calendar calendar;

    @NotBlank
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String title;

    @Size(max = 2500)
    private String description;

    /**
     * DEPRECATED: Use startDatetimeLocal + taskTimezone instead
     * Kept for backward compatibility during migration
     */
    @Deprecated
    @NotNull
    @Column(name = "start_datetime", nullable = false, columnDefinition = "TIMESTAMP")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP_UTC)
    private Instant startDatetime;

    /**
     * DEPRECATED: Use endDatetimeLocal + taskTimezone instead
     * Kept for backward compatibility during migration
     */
    @Deprecated
    @NotNull
    @Column(name = "end_datetime", nullable = false, columnDefinition = "TIMESTAMP")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP_UTC)
    private Instant endDatetime;

    /**
     * Local datetime (floating time) - does not change with DST.
     * Example: 2025-10-20T15:00:00 means "3 PM" regardless of DST offset.
     */
    @Column(name = "start_datetime_local")
    private LocalDateTime startDatetimeLocal;

    /**
     * Local datetime (floating time) - does not change with DST.
     * Example: 2025-10-20T16:00:00 means "4 PM" regardless of DST offset.
     */
    @Column(name = "end_datetime_local")
    private LocalDateTime endDatetimeLocal;

    /**
     * IANA timezone identifier (e.g., "Europe/Rome", "America/New_York").
     * Used to convert local time to UTC when needed for notifications and CalDAV.
     */
    @Size(max = 50)
    @Column(name = "task_timezone", length = 50)
    private String taskTimezone;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    @Column(length = 7)
    private String color = "#3788d8";

    @Size(max = 200)
    @Column(length = 200)
    private String location;

    @Column(name = "is_all_day", nullable = false)
    private Boolean isAllDay = false;

    @Size(max = 500)
    @Column(name = "recurrence_rule", length = 500)
    private String recurrenceRule;

    @Column(name = "recurrence_end")
    private Instant recurrenceEnd;

    @Column(name = "recurrence_exceptions", columnDefinition = "TEXT")
    private String recurrenceExceptions;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reminder> reminders = new ArrayList<>();
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // Constructors
    public Task() {}
    
    public Task(User user, String title, Instant startDatetime, Instant endDatetime) {
        this.user = user;
        this.title = title;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
    }
    
    public Task(User user, String title, String description, Instant startDatetime, Instant endDatetime, String color) {
        this(user, title, startDatetime, endDatetime);
        this.description = description;
        this.color = color;
    }
    
    // Validation method
    @PrePersist
    @PreUpdate
    private void validate() {
        // Validate using new floating time fields if available
        if (startDatetimeLocal != null && endDatetimeLocal != null) {
            if (!endDatetimeLocal.isAfter(startDatetimeLocal)) {
                throw new IllegalArgumentException("End datetime must be after start datetime");
            }
        } else if (endDatetime != null && startDatetime != null && !endDatetime.isAfter(startDatetime)) {
            // Fallback to deprecated fields for backward compatibility
            throw new IllegalArgumentException("End datetime must be after start datetime");
        }

        if (recurrenceEnd != null && startDatetime != null && !recurrenceEnd.isAfter(startDatetime)) {
            throw new IllegalArgumentException("Recurrence end must be after start datetime");
        }

        // Sync deprecated UTC fields with new local fields when both are present
        if (startDatetimeLocal != null && endDatetimeLocal != null && taskTimezone != null) {
            startDatetime = startDatetimeLocal.atZone(ZoneId.of(taskTimezone)).toInstant();
            endDatetime = endDatetimeLocal.atZone(ZoneId.of(taskTimezone)).toInstant();
        }
    }
    
    // Getters and Setters

    /**
     * Get UID (Primary Key)
     * This replaces the old getId() method - UID is now the primary identifier
     */
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
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

    public String getRecurrenceExceptions() {
        return recurrenceExceptions;
    }

    public void setRecurrenceExceptions(String recurrenceExceptions) {
        this.recurrenceExceptions = recurrenceExceptions;
    }

    public boolean isRecurring() {
        return recurrenceRule != null && !recurrenceRule.trim().isEmpty();
    }

    public List<Reminder> getReminders() {
        return reminders;
    }
    
    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
    }
    
    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
        reminder.setTask(this);
    }
    
    public void removeReminder(Reminder reminder) {
        reminders.remove(reminder);
        reminder.setTask(null);
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

    public LocalDateTime getStartDatetimeLocal() {
        return startDatetimeLocal;
    }

    public void setStartDatetimeLocal(LocalDateTime startDatetimeLocal) {
        this.startDatetimeLocal = startDatetimeLocal;
    }

    public LocalDateTime getEndDatetimeLocal() {
        return endDatetimeLocal;
    }

    public void setEndDatetimeLocal(LocalDateTime endDatetimeLocal) {
        this.endDatetimeLocal = endDatetimeLocal;
    }

    public String getTaskTimezone() {
        return taskTimezone;
    }

    public void setTaskTimezone(String taskTimezone) {
        this.taskTimezone = taskTimezone;
    }

    /**
     * Helper method: Converts the local datetime to UTC Instant based on current timezone rules.
     * This accounts for DST - the same local time may map to different UTC times depending on the date.
     *
     * Example:
     * - Task at 15:00 Europe/Rome on Oct 20 (UTC+2) → 13:00 UTC
     * - Task at 15:00 Europe/Rome on Oct 28 (UTC+1) → 14:00 UTC
     *
     * @return UTC Instant for the task's local time considering current DST rules
     */
    public Instant getStartDatetimeAsInstant() {
        if (startDatetimeLocal == null || taskTimezone == null) {
            return startDatetime; // Fallback to deprecated field
        }
        return startDatetimeLocal.atZone(ZoneId.of(taskTimezone)).toInstant();
    }

    /**
     * Helper method: Converts the local datetime to UTC Instant based on current timezone rules.
     * @see #getStartDatetimeAsInstant()
     */
    public Instant getEndDatetimeAsInstant() {
        if (endDatetimeLocal == null || taskTimezone == null) {
            return endDatetime; // Fallback to deprecated field
        }
        return endDatetimeLocal.atZone(ZoneId.of(taskTimezone)).toInstant();
    }

    /**
     * Helper method: Get ZonedDateTime for display purposes
     */
    public ZonedDateTime getStartDatetimeZoned() {
        if (startDatetimeLocal == null || taskTimezone == null) {
            return startDatetime.atZone(ZoneId.of("UTC"));
        }
        return startDatetimeLocal.atZone(ZoneId.of(taskTimezone));
    }

    /**
     * Helper method: Get ZonedDateTime for display purposes
     */
    public ZonedDateTime getEndDatetimeZoned() {
        if (endDatetimeLocal == null || taskTimezone == null) {
            return endDatetime.atZone(ZoneId.of("UTC"));
        }
        return endDatetimeLocal.atZone(ZoneId.of(taskTimezone));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return uid != null && uid.equals(task.getUid());
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Task{" +
                "uid='" + uid + '\'' +
                ", title='" + title + '\'' +
                ", startDatetime=" + startDatetime +
                ", endDatetime=" + endDatetime +
                ", color='" + color + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}