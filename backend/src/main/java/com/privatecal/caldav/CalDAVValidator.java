package com.privatecal.caldav;

import com.privatecal.dto.TaskRequest;
import com.privatecal.entity.Task;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Validation and utility methods for CalDAV operations
 */
@Component
public class CalDAVValidator {

    /**
     * Check if task content has changed (for duplicate detection)
     */
    public boolean hasContentChanged(Task existingTask, TaskRequest taskRequest) {
        // Compare title
        if (!Objects.equals(existingTask.getTitle(), taskRequest.getTitle())) {
            return true;
        }

        // Compare description
        if (!Objects.equals(existingTask.getDescription(), taskRequest.getDescription())) {
            return true;
        }

        // Compare location
        if (!Objects.equals(existingTask.getLocation(), taskRequest.getLocation())) {
            return true;
        }

        // Compare start/end times
        if (!Objects.equals(existingTask.getStartDatetimeLocal(), taskRequest.getStartDatetimeLocal())) {
            return true;
        }
        if (!Objects.equals(existingTask.getEndDatetimeLocal(), taskRequest.getEndDatetimeLocal())) {
            return true;
        }

        // Compare timezone
        if (!Objects.equals(existingTask.getTaskTimezone(), taskRequest.getTimezone())) {
            return true;
        }

        // Compare all-day flag
        if (!Objects.equals(existingTask.getIsAllDay(), taskRequest.getIsAllDay())) {
            return true;
        }

        // Compare recurrence rule
        if (!Objects.equals(existingTask.getRecurrenceRule(), taskRequest.getRecurrenceRule())) {
            return true;
        }

        // Compare color
        if (!Objects.equals(existingTask.getColor(), taskRequest.getColor())) {
            return true;
        }

        return false;
    }

    /**
     * Escape XML special characters to prevent XML injection
     */
    public String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Validate username specifically (allows email format)
     * Prevents path traversal and injection attacks
     *
     * @param username The username to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (username.length() > 100) {
            throw new IllegalArgumentException("Username exceeds maximum length of 100");
        }

        // Prevent path traversal
        if (username.contains("..") || username.contains("/") || username.contains("\\")) {
            throw new IllegalArgumentException("Username contains invalid characters");
        }

        // Username can be email or simple username: alphanumeric + @.-_
        if (!username.matches("^[a-zA-Z0-9._@-]+$")) {
            throw new IllegalArgumentException("Username contains invalid characters");
        }
    }

    /**
     * Validate calendar slug specifically (more restrictive than username)
     * Prevents path traversal and injection attacks
     *
     * @param slug The calendar slug to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCalendarSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            throw new IllegalArgumentException("Calendar slug cannot be empty");
        }

        if (slug.length() > 100) {
            throw new IllegalArgumentException("Calendar slug exceeds maximum length of 100");
        }

        // Prevent path traversal
        if (slug.contains("..") || slug.contains("/") || slug.contains("\\")) {
            throw new IllegalArgumentException("Calendar slug contains invalid path characters");
        }

        // Calendar slugs: lowercase alphanumeric with hyphens/underscores
        // Allow single character slugs
        if (!slug.matches("^[a-z0-9][a-z0-9_-]*[a-z0-9]$") && !slug.matches("^[a-z0-9]$")) {
            throw new IllegalArgumentException("Calendar slug must be lowercase alphanumeric with hyphens/underscores");
        }
    }

    /**
     * Validate event UID specifically
     * Prevents path traversal and injection attacks
     *
     * @param eventUid The event UID to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateEventUid(String eventUid) {
        if (eventUid == null || eventUid.isEmpty()) {
            throw new IllegalArgumentException("Event UID cannot be empty");
        }

        if (eventUid.length() > 255) {
            throw new IllegalArgumentException("Event UID exceeds maximum length of 255");
        }

        // Prevent path traversal
        if (eventUid.contains("..") || eventUid.contains("/") || eventUid.contains("\\")) {
            throw new IllegalArgumentException("Event UID contains invalid path characters");
        }

        // Event UIDs are flexible: alphanumeric + .-_@
        if (!eventUid.matches("^[a-zA-Z0-9._@-]+$")) {
            throw new IllegalArgumentException("Event UID contains invalid characters");
        }
    }

    /**
     * Sanitize header value to prevent HTTP response splitting attacks
     * Removes all control characters including CR, LF, and other dangerous chars
     *
     * @param value The header value to sanitize
     * @return Sanitized value safe for HTTP headers
     */
    public String sanitizeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        // Remove all control characters (0x00-0x1F) including CR/LF, and DEL (0x7F)
        return value.replaceAll("[\\r\\n\\x00-\\x1F\\x7F]", "");
    }

    /**
     * Parse calendar-multiget request to extract requested UIDs
     * Returns null if not a calendar-multiget request
     */
    public java.util.List<String> parseCalendarMultigetUids(String requestXML) {
        if (requestXML == null || !requestXML.contains("calendar-multiget")) {
            return null;
        }

        java.util.List<String> requestedUids = new java.util.ArrayList<>();
        // Simple XML parsing to extract href values
        // Example: <D:href>/caldav/user/calendar/EVENT-UID.ics</D:href>
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<D:href>([^<]+)</D:href>");
        java.util.regex.Matcher matcher = pattern.matcher(requestXML);
        while (matcher.find()) {
            String href = matcher.group(1);
            // Extract UID from href (remove .ics extension)
            if (href.endsWith(".ics")) {
                String[] parts = href.split("/");
                String uidWithExt = parts[parts.length - 1];
                String uid = uidWithExt.substring(0, uidWithExt.length() - 4);
                requestedUids.add(uid);
            }
        }

        return requestedUids.isEmpty() ? null : requestedUids;
    }
}
