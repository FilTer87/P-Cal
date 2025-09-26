package com.privatecal.util;

import com.privatecal.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for timezone conversions in notifications
 */
public class TimezoneUtils {

    private static final Logger logger = LoggerFactory.getLogger(TimezoneUtils.class);

    // Default timezone fallback
    private static final ZoneId DEFAULT_TIMEZONE = ZoneOffset.UTC;

    // Common date/time formatters for notifications
    public static final DateTimeFormatter NOTIFICATION_DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm");

    public static final DateTimeFormatter NOTIFICATION_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm");

    public static final DateTimeFormatter NOTIFICATION_DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");

    /**
     * Convert an Instant to the specified timezone and format it for notifications
     *
     * @param instant The UTC instant to convert
     * @param timezone The timezone string (e.g., "America/New_York", "UTC")
     * @param formatter The formatter to use for the output
     * @return Formatted datetime string in specified timezone, or UTC if timezone is invalid
     */
    public static String formatInstantInTimezone(Instant instant, String timezone, DateTimeFormatter formatter) {
        if (instant == null) {
            return null;
        }

        ZoneId zoneId = getZoneIdFromString(timezone);

        try {
            ZonedDateTime zonedDateTime = instant.atZone(zoneId);
            String formattedTime = zonedDateTime.format(formatter);

            logger.debug("Converted {} from UTC to {} timezone: {}",
                        instant, zoneId.getId(), formattedTime);

            return formattedTime;

        } catch (Exception e) {
            logger.warn("Error formatting instant {} in timezone {}: {}",
                       instant, zoneId.getId(), e.getMessage());

            // Fallback to UTC
            return instant.atZone(DEFAULT_TIMEZONE).format(formatter);
        }
    }

    /**
     * Convert an Instant to the user's timezone and format it for notifications
     *
     * @param instant The UTC instant to convert
     * @param user The user whose timezone should be used
     * @param formatter The formatter to use for the output
     * @return Formatted datetime string in user's timezone, or UTC if timezone is invalid
     */
    public static String formatInstantInUserTimezone(Instant instant, User user, DateTimeFormatter formatter) {
        if (instant == null) {
            return null;
        }

        ZoneId userZoneId = getUserZoneId(user);

        try {
            ZonedDateTime zonedDateTime = instant.atZone(userZoneId);
            String formattedTime = zonedDateTime.format(formatter);

            logger.debug("Converted {} from UTC to {} timezone: {}",
                        instant, userZoneId.getId(), formattedTime);

            return formattedTime;

        } catch (Exception e) {
            logger.warn("Error formatting instant {} in timezone {}: {}",
                       instant, userZoneId.getId(), e.getMessage());

            // Fallback to UTC
            return instant.atZone(DEFAULT_TIMEZONE).format(formatter);
        }
    }

    /**
     * Convert an Instant to specified timezone with default notification format
     * Format: "Friday, December 25, 2024 at 14:30"
     */
    public static String formatInstantInTimezone(Instant instant, String timezone) {
        return formatInstantInTimezone(instant, timezone, NOTIFICATION_DATE_TIME_FORMAT);
    }

    /**
     * Convert an Instant to user's timezone with default notification format
     * Format: "Friday, December 25, 2024 at 14:30"
     */
    public static String formatInstantInUserTimezone(Instant instant, User user) {
        return formatInstantInUserTimezone(instant, user, NOTIFICATION_DATE_TIME_FORMAT);
    }

    /**
     * Convert an Instant to user's timezone - time only
     * Format: "14:30"
     */
    public static String formatTimeInUserTimezone(Instant instant, User user) {
        return formatInstantInUserTimezone(instant, user, NOTIFICATION_TIME_FORMAT);
    }

    /**
     * Convert an Instant to user's timezone - date only
     * Format: "Friday, December 25, 2024"
     */
    public static String formatDateInUserTimezone(Instant instant, User user) {
        return formatInstantInUserTimezone(instant, user, NOTIFICATION_DATE_FORMAT);
    }

    /**
     * Get ZoneId from timezone string, with fallback to UTC
     *
     * @param timezone The timezone string to parse
     * @return ZoneId for the timezone, or UTC if invalid/null
     */
    public static ZoneId getZoneIdFromString(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            logger.debug("Timezone is null/empty, using default timezone: {}", DEFAULT_TIMEZONE);
            return DEFAULT_TIMEZONE;
        }

        String cleanTimezone = timezone.trim();

        try {
            ZoneId zoneId = ZoneId.of(cleanTimezone);
            logger.debug("Using timezone: {}", cleanTimezone);
            return zoneId;

        } catch (Exception e) {
            logger.warn("Invalid timezone '{}', falling back to UTC: {}",
                       cleanTimezone, e.getMessage());
            return DEFAULT_TIMEZONE;
        }
    }

    /**
     * Get the user's ZoneId, with fallback to UTC
     *
     * @param user The user whose timezone should be retrieved
     * @return ZoneId for the user's timezone, or UTC if invalid/null
     */
    public static ZoneId getUserZoneId(User user) {
        if (user == null) {
            logger.debug("User is null, using default timezone: {}", DEFAULT_TIMEZONE);
            return DEFAULT_TIMEZONE;
        }

        return getZoneIdFromString(user.getTimezone());
    }

    /**
     * Get the timezone display name for the user
     *
     * @param user The user whose timezone display name should be retrieved
     * @return Human-readable timezone name
     */
    public static String getTimezoneDisplayName(User user) {
        ZoneId zoneId = getUserZoneId(user);

        try {
            return zoneId.getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH
            );
        } catch (Exception e) {
            logger.debug("Could not get display name for timezone {}, using ID", zoneId.getId());
            return zoneId.getId();
        }
    }

    /**
     * Check if the user has a valid custom timezone set
     *
     * @param user The user to check
     * @return true if user has a valid timezone different from UTC
     */
    public static boolean hasCustomTimezone(User user) {
        if (user == null || user.getTimezone() == null || user.getTimezone().trim().isEmpty()) {
            return false;
        }

        String timezone = user.getTimezone().trim();
        return !timezone.equalsIgnoreCase("UTC") && !timezone.equalsIgnoreCase("GMT");
    }

    /**
     * Convert an Instant to user timezone with timezone information included
     * Format: "Friday, December 25, 2024 at 14:30 (CET)"
     *
     * @param instant The instant to convert
     * @param user The user whose timezone should be used
     * @return Formatted string with timezone info
     */
    public static String formatInstantWithTimezone(Instant instant, User user) {
        if (instant == null) {
            return null;
        }

        ZoneId userZoneId = getUserZoneId(user);

        try {
            ZonedDateTime zonedDateTime = instant.atZone(userZoneId);
            String formattedTime = zonedDateTime.format(NOTIFICATION_DATE_TIME_FORMAT);
            String timezoneAbbr = zonedDateTime.format(DateTimeFormatter.ofPattern("z"));

            return formattedTime + " (" + timezoneAbbr + ")";

        } catch (Exception e) {
            logger.warn("Error formatting instant with timezone: {}", e.getMessage());
            return formatInstantInUserTimezone(instant, user);
        }
    }
}