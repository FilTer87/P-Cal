package com.privatecal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "reminders")
@EntityListeners(AuditingEntityListener.class)
public class Reminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull
    private Task task;
    
    @NotNull
    @Column(name = "reminder_time", nullable = false)
    private Instant reminderTime;
    
    @Min(0)
    @Column(name = "reminder_offset_minutes", nullable = false)
    private Integer reminderOffsetMinutes;
    
    @Column(name = "is_sent")
    private Boolean isSent = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20)
    private NotificationType notificationType = NotificationType.PUSH;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    public enum NotificationType {
        PUSH, EMAIL
    }
    
    // Constructors
    public Reminder() {}
    
    public Reminder(Task task, Integer reminderOffsetMinutes) {
        this.task = task;
        this.reminderOffsetMinutes = reminderOffsetMinutes;
        calculateReminderTime();
    }
    
    public Reminder(Task task, Integer reminderOffsetMinutes, NotificationType notificationType) {
        this(task, reminderOffsetMinutes);
        this.notificationType = notificationType;
    }
    
    // Calculate reminder time based on task start time and offset
    public void calculateReminderTime() {
        if (task != null && task.getStartDatetime() != null && reminderOffsetMinutes != null) {
            this.reminderTime = task.getStartDatetime().minus(java.time.Duration.ofMinutes(reminderOffsetMinutes));
        }
    }
    
    @PrePersist
    @PreUpdate
    private void updateReminderTime() {
        calculateReminderTime();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Task getTask() {
        return task;
    }
    
    public void setTask(Task task) {
        this.task = task;
        calculateReminderTime();
    }
    
    public Instant getReminderTime() {
        return reminderTime;
    }
    
    public void setReminderTime(Instant reminderTime) {
        this.reminderTime = reminderTime;
    }
    
    public Integer getReminderOffsetMinutes() {
        return reminderOffsetMinutes;
    }
    
    public void setReminderOffsetMinutes(Integer reminderOffsetMinutes) {
        this.reminderOffsetMinutes = reminderOffsetMinutes;
        calculateReminderTime();
    }
    
    public Boolean getIsSent() {
        return isSent;
    }
    
    public void setIsSent(Boolean sent) {
        isSent = sent;
    }
    
    public NotificationType getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isDue() {
        return reminderTime != null && !isSent && Instant.now().isAfter(reminderTime);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reminder reminder)) return false;
        return id != null && id.equals(reminder.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", reminderTime=" + reminderTime +
                ", reminderOffsetMinutes=" + reminderOffsetMinutes +
                ", isSent=" + isSent +
                ", notificationType=" + notificationType +
                ", createdAt=" + createdAt +
                '}';
    }
}