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

/**
 * Calendar entity representing a user's calendar collection
 * Supports multiple calendars per user for CalDAV integration
 *
 * CalDAV URL structure: /caldav/{username}/{calendar.slug}/
 * Example: /caldav/johndoe/default/, /caldav/johndoe/work/
 */
@Entity
@Table(name = "calendars", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "slug"})
})
@EntityListeners(AuditingEntityListener.class)
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @NotBlank(message = "Calendar name is required")
    @Size(min = 1, max = 100, message = "Calendar name must be between 1 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Calendar slug is required")
    @Size(min = 1, max = 100, message = "Calendar slug must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Column(nullable = false, length = 100)
    private String slug;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    @Column(length = 7)
    private String color = "#3788d8";

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Size(max = 50)
    @Column(length = 50)
    private String timezone;

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Task> tasks = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors
    public Calendar() {}

    public Calendar(User user, String name, String slug) {
        this.user = user;
        this.name = name;
        this.slug = slug;
    }

    public Calendar(User user, String name, String slug, String color, Boolean isDefault) {
        this(user, name, slug);
        this.color = color;
        this.isDefault = isDefault;
    }

    // Validation method
    @PrePersist
    @PreUpdate
    private void validate() {
        // Ensure only one default calendar per user is enforced at application level
        if (Boolean.TRUE.equals(isDefault) && user != null) {
            // This will be validated in service layer
        }

        // Ensure timezone is set (fallback to user's timezone)
        if (timezone == null && user != null) {
            timezone = user.getTimezone();
        }
    }

    // Business methods
    public boolean isOwnedBy(User user) {
        return this.user != null && this.user.equals(user);
    }

    public String getCalDAVPath() {
        return String.format("/caldav/%s/%s/", user.getUsername(), slug);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
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
        if (!(o instanceof Calendar calendar)) return false;
        return id != null && id.equals(calendar.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Calendar{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", isDefault=" + isDefault +
                ", createdAt=" + createdAt +
                '}';
    }
}
