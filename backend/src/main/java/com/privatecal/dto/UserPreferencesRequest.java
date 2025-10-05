package com.privatecal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for User Preferences Request
 * Used for updating user preferences via API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {

    @Pattern(regexp = "^(light|dark|system)$", message = "Theme must be 'light', 'dark', or 'system'")
    private String theme;

    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "Language must be in BCP 47 format (e.g., it-IT, en-US)")
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
}