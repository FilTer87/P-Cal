package com.privatecal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Calendar creation and update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarRequest {

    @NotBlank(message = "Calendar name is required")
    @Size(min = 1, max = 100, message = "Calendar name must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Calendar slug is required")
    @Size(min = 1, max = 100, message = "Calendar slug must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #3788d8)")
    @Builder.Default
    private String color = "#3788d8";

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean isVisible = true;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;
}
