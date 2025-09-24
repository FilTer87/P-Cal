package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Data Transfer Object for User Preferences Request
 * Used for updating user preferences via API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferencesRequest {

    @Pattern(regexp = "^(light|dark|system)$", message = "Theme must be 'light', 'dark', or 'system'")
    private String theme;

    private String language;

    private String timezone;

    @Pattern(regexp = "^(12h|24h)$", message = "Time format must be '12h' or '24h'")
    private String timeFormat;

    @Pattern(regexp = "^(month|week|day|agenda)$", message = "Calendar view must be 'month', 'week', 'day', or 'agenda'")
    private String calendarView;

    private Boolean emailNotifications;

    private Boolean reminderNotifications;

    @Min(value = 0, message = "Week start day must be 0 (Sunday) or 1 (Monday)")
    @Max(value = 1, message = "Week start day must be 0 (Sunday) or 1 (Monday)")
    private Integer weekStartDay;

    // Default constructor
    public UserPreferencesRequest() {}

    // Full constructor
    public UserPreferencesRequest(String theme, String language, String timezone, String timeFormat,
                                 String calendarView, Boolean emailNotifications, Boolean reminderNotifications,
                                 Integer weekStartDay) {
        this.theme = theme;
        this.language = language;
        this.timezone = timezone;
        this.timeFormat = timeFormat;
        this.calendarView = calendarView;
        this.emailNotifications = emailNotifications;
        this.reminderNotifications = reminderNotifications;
        this.weekStartDay = weekStartDay;
    }

    // Getters and Setters
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getCalendarView() {
        return calendarView;
    }

    public void setCalendarView(String calendarView) {
        this.calendarView = calendarView;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getReminderNotifications() {
        return reminderNotifications;
    }

    public void setReminderNotifications(Boolean reminderNotifications) {
        this.reminderNotifications = reminderNotifications;
    }

    public Integer getWeekStartDay() {
        return weekStartDay;
    }

    public void setWeekStartDay(Integer weekStartDay) {
        this.weekStartDay = weekStartDay;
    }

    @Override
    public String toString() {
        return "UserPreferencesRequest{" +
                "theme='" + theme + '\'' +
                ", language='" + language + '\'' +
                ", timezone='" + timezone + '\'' +
                ", timeFormat='" + timeFormat + '\'' +
                ", calendarView='" + calendarView + '\'' +
                ", emailNotifications=" + emailNotifications +
                ", reminderNotifications=" + reminderNotifications +
                ", weekStartDay=" + weekStartDay +
                '}';
    }
}