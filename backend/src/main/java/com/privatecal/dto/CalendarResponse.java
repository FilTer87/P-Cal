package com.privatecal.dto;

import com.privatecal.entity.Calendar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for Calendar responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String color;
    private Boolean isDefault;
    private Boolean isVisible;
    private String timezone;
    private Long taskCount;
    private String caldavPath;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Factory method to create CalendarResponse from Calendar entity
     */
    public static CalendarResponse from(Calendar calendar) {
        return CalendarResponse.builder()
                .id(calendar.getId())
                .name(calendar.getName())
                .slug(calendar.getSlug())
                .description(calendar.getDescription())
                .color(calendar.getColor())
                .isDefault(calendar.getIsDefault())
                .isVisible(calendar.getIsVisible())
                .timezone(calendar.getTimezone())
                .caldavPath(calendar.getCalDAVPath())
                .createdAt(calendar.getCreatedAt())
                .updatedAt(calendar.getUpdatedAt())
                .taskCount((long) calendar.getTasks().size())
                .build();
    }

    /**
     * Factory method to create CalendarResponse with explicit task count
     * (avoids loading tasks collection when count is already known)
     */
    public static CalendarResponse fromWithTaskCount(Calendar calendar, Long taskCount) {
        return CalendarResponse.builder()
                .id(calendar.getId())
                .name(calendar.getName())
                .slug(calendar.getSlug())
                .description(calendar.getDescription())
                .color(calendar.getColor())
                .isDefault(calendar.getIsDefault())
                .isVisible(calendar.getIsVisible())
                .timezone(calendar.getTimezone())
                .caldavPath(calendar.getCalDAVPath())
                .createdAt(calendar.getCreatedAt())
                .updatedAt(calendar.getUpdatedAt())
                .taskCount(taskCount)
                .build();
    }
}
