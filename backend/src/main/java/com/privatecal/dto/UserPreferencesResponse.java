package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.User;

/**
 * Data Transfer Object for User Preferences Response
 * Used for returning user preferences via API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferencesResponse {

    private String theme;
    private String language;
    private String timezone;
    private String timeFormat;
    private String calendarView;
    private Boolean emailNotifications;
    private Boolean reminderNotifications;
    private Integer weekStartDay;

    // Default constructor
    public UserPreferencesResponse() {}

    // Constructor from User entity
    public UserPreferencesResponse(User user) {
        this.theme = user.getTheme();
        this.language = "it"; // Default language, can be extended later
        this.timezone = user.getTimezone();
        this.timeFormat = user.getTimeFormat();
        this.calendarView = user.getCalendarView();
        this.emailNotifications = user.getEmailNotifications();
        this.reminderNotifications = user.getReminderNotifications();
        this.weekStartDay = user.getWeekStartDay();
    }

    // Full constructor
    public UserPreferencesResponse(String theme, String language, String timezone, String timeFormat,
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

    // Factory method
    public static UserPreferencesResponse fromUser(User user) {
        return new UserPreferencesResponse(user);
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
        return "UserPreferencesResponse{" +
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