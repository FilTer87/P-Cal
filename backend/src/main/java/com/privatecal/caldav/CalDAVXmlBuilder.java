package com.privatecal.caldav;

import com.privatecal.dto.CalendarResponse;
import com.privatecal.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builder for CalDAV XML responses
 * Handles WebDAV multistatus responses according to RFC 4918 and RFC 4791
 */
@Component
@RequiredArgsConstructor
public class CalDAVXmlBuilder {

    private final CalDAVValidator validator;

    /**
     * Build PROPFIND response for CalDAV root (/)
     */
    public String buildRootResponse(String username) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");
        xml.append("  <D:response>\n");
        xml.append("    <D:href>/caldav/</D:href>\n");
        xml.append("    <D:propstat>\n");
        xml.append("      <D:prop>\n");
        xml.append("        <D:resourcetype><D:collection/></D:resourcetype>\n");
        xml.append("        <D:displayname>CalDAV Root</D:displayname>\n");
        xml.append("        <D:current-user-principal>\n");
        xml.append("          <D:href>/caldav/").append(validator.escapeXml(username)).append("/</D:href>\n");
        xml.append("        </D:current-user-principal>\n");
        xml.append("      </D:prop>\n");
        xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
        xml.append("    </D:propstat>\n");
        xml.append("  </D:response>\n");
        xml.append("</D:multistatus>");
        return xml.toString();
    }

    /**
     * Build PROPFIND response for user principal (/caldav/{username}/)
     */
    public String buildUserPrincipalResponse(String username, String userEmail, List<CalendarResponse> calendars, String depth) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");

        // User principal collection
        xml.append("  <D:response>\n");
        xml.append("    <D:href>/caldav/").append(validator.escapeXml(username)).append("/</D:href>\n");
        xml.append("    <D:propstat>\n");
        xml.append("      <D:prop>\n");
        xml.append("        <D:resourcetype><D:collection/></D:resourcetype>\n");
        xml.append("        <D:displayname>").append(validator.escapeXml(username)).append("</D:displayname>\n");
        xml.append("        <C:calendar-home-set>\n");
        xml.append("          <D:href>/caldav/").append(validator.escapeXml(username)).append("/</D:href>\n");
        xml.append("        </C:calendar-home-set>\n");
        xml.append("        <C:calendar-user-address-set>\n");
        xml.append("          <D:href>mailto:").append(validator.escapeXml(userEmail)).append("</D:href>\n");
        xml.append("        </C:calendar-user-address-set>\n");
        xml.append("      </D:prop>\n");
        xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
        xml.append("    </D:propstat>\n");
        xml.append("  </D:response>\n");

        // List each calendar if depth > 0
        if (!"0".equals(depth)) {
            for (CalendarResponse calendar : calendars) {
                xml.append("  <D:response>\n");
                xml.append("    <D:href>/caldav/").append(validator.escapeXml(username)).append("/")
                   .append(validator.escapeXml(calendar.getSlug())).append("/</D:href>\n");
                xml.append("    <D:propstat>\n");
                xml.append("      <D:prop>\n");
                xml.append("        <D:resourcetype>\n");
                xml.append("          <D:collection/>\n");
                xml.append("          <C:calendar/>\n");
                xml.append("        </D:resourcetype>\n");
                xml.append("        <D:displayname>").append(validator.escapeXml(calendar.getName())).append("</D:displayname>\n");
                xml.append("        <C:calendar-description>").append(validator.escapeXml(calendar.getDescription() != null ? calendar.getDescription() : "")).append("</C:calendar-description>\n");
                xml.append("      </D:prop>\n");
                xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
                xml.append("    </D:propstat>\n");
                xml.append("  </D:response>\n");
            }
        }

        xml.append("</D:multistatus>");
        return xml.toString();
    }

    /**
     * Build PROPFIND/REPORT response for calendar collection
     *
     * @param username Username
     * @param calendarSlug Calendar slug
     * @param calendarName Calendar display name
     * @param tasks List of tasks to include
     * @param includeCalendarData Whether to include full calendar data (ICS content)
     * @param method HTTP method (PROPFIND or REPORT)
     * @param taskToIcsConverter Function to convert Task to ICS string
     * @param etagGenerator Function to generate ETag for a task UID
     */
    public String buildCalendarCollectionResponse(
            String username,
            String calendarSlug,
            String calendarName,
            List<Task> tasks,
            boolean includeCalendarData,
            String method,
            java.util.function.Function<String, String> taskToIcsConverter,
            java.util.function.Function<String, String> etagGenerator) {

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n");

        // Calendar collection resource
        xml.append("  <D:response>\n");
        xml.append("    <D:href>/caldav/").append(validator.escapeXml(username)).append("/").append(validator.escapeXml(calendarSlug)).append("/</D:href>\n");
        xml.append("    <D:propstat>\n");
        xml.append("      <D:prop>\n");
        xml.append("        <D:resourcetype><D:collection/><C:calendar/></D:resourcetype>\n");
        xml.append("        <D:displayname>").append(validator.escapeXml(calendarName)).append("</D:displayname>\n");
        xml.append("      </D:prop>\n");
        xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
        xml.append("    </D:propstat>\n");
        xml.append("  </D:response>\n");

        // Individual events
        for (Task task : tasks) {
            String etag = etagGenerator.apply(task.getUid());
            xml.append("  <D:response>\n");
            xml.append("    <D:href>/caldav/").append(validator.escapeXml(username)).append("/").append(validator.escapeXml(calendarSlug))
               .append("/").append(validator.escapeXml(task.getUid())).append(".ics</D:href>\n");
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            xml.append("        <D:getetag>\"").append(validator.escapeXml(etag)).append("\"</D:getetag>\n");

            // Only include getcontenttype for PROPFIND
            if ("PROPFIND".equalsIgnoreCase(method)) {
                xml.append("        <D:getcontenttype>text/calendar; component=VEVENT</D:getcontenttype>\n");
            }

            // Include calendar-data if requested
            if (includeCalendarData) {
                try {
                    String icsContent = taskToIcsConverter.apply(task.getUid());
                    xml.append("        <C:calendar-data>");
                    xml.append(validator.escapeXml(icsContent));
                    xml.append("</C:calendar-data>\n");
                } catch (Exception e) {
                    // Log error but continue with other tasks
                    org.slf4j.LoggerFactory.getLogger(CalDAVXmlBuilder.class)
                        .warn("Failed to export task {} as ICS: {}", task.getUid(), e.getMessage());
                }
            }

            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");
        }

        xml.append("</D:multistatus>");
        return xml.toString();
    }

}
