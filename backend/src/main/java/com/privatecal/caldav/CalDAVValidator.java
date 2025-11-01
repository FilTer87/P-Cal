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
