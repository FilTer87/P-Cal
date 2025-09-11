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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@EntityListeners(AuditingEntityListener.class)
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;
    
    @NotBlank
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String title;
    
    @Size(max = 500)
    private String description;
    
    @NotNull
    @Column(name = "start_datetime", nullable = false)
    private Instant startDatetime;
    
    @NotNull
    @Column(name = "end_datetime", nullable = false)
    private Instant endDatetime;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    @Column(length = 7)
    private String color = "#3788d8";
    
    @Column(name = "is_all_day")
    private Boolean isAllDay = false;
    
    @Size(max = 200)
    @Column(length = 200)
    private String location;
    
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
        if (endDatetime != null && startDatetime != null && !endDatetime.isAfter(startDatetime)) {
            throw new IllegalArgumentException("End datetime must be after start datetime");
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return id != null && id.equals(task.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDatetime=" + startDatetime +
                ", endDatetime=" + endDatetime +
                ", color='" + color + '\'' +
                ", isAllDay=" + isAllDay +
                ", createdAt=" + createdAt +
                '}';
    }
}