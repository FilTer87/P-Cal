package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privatecal.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for User Preferences Response
 * Used for returning user preferences via API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
public class UserPreferencesResponse {

    private String theme;
    private String language;
    private String timezone;
    private String timeFormat;
    private String calendarView;
    private Boolean emailNotifications;
    private Boolean reminderNotifications;
    private Integer weekStartDay;

    // Constructor from User entity
    public UserPreferencesResponse(User user) {
        this.theme = user.getTheme();
        this.language = user.getLocale(); // User's locale preference (e.g., it-IT, en-US)
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
}