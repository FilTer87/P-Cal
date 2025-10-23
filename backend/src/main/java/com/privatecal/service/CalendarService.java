package com.privatecal.service;

import com.privatecal.dto.CalendarRequest;
import com.privatecal.dto.CalendarResponse;
import com.privatecal.entity.Calendar;
import com.privatecal.entity.User;
import com.privatecal.repository.CalendarRepository;
import com.privatecal.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calendar CRUD operations and calendar-related business logic
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CalendarService {

    private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);

    private final CalendarRepository calendarRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;

    /**
     * Get all calendars for current user
     */
    @Transactional(readOnly = true)
    public List<CalendarResponse> getAllCalendars() {
        User currentUser = userService.getCurrentUser();
        logger.debug("Fetching all calendars for user: {}", currentUser.getUsername());

        List<Calendar> calendars = calendarRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(currentUser);

        return calendars.stream()
                .map(calendar -> {
                    Long taskCount = taskRepository.countByUser_IdAndCalendar_Id(currentUser.getId(), calendar.getId());
                    return CalendarResponse.fromWithTaskCount(calendar, taskCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get calendar by ID
     */
    @Transactional(readOnly = true)
    public CalendarResponse getCalendarById(Long id) {
        User currentUser = userService.getCurrentUser();
        logger.debug("Fetching calendar {} for user: {}", id, currentUser.getUsername());

        Calendar calendar = calendarRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Calendar not found"));

        Long taskCount = taskRepository.countByUser_IdAndCalendar_Id(currentUser.getId(), calendar.getId());
        return CalendarResponse.fromWithTaskCount(calendar, taskCount);
    }

    /**
     * Get calendar by slug
     */
    @Transactional(readOnly = true)
    public CalendarResponse getCalendarBySlug(String slug) {
        User currentUser = userService.getCurrentUser();
        logger.debug("Fetching calendar with slug '{}' for user: {}", slug, currentUser.getUsername());

        Calendar calendar = calendarRepository.findBySlugAndUser(slug, currentUser)
                .orElseThrow(() -> new RuntimeException("Calendar not found"));

        Long taskCount = taskRepository.countByUser_IdAndCalendar_Id(currentUser.getId(), calendar.getId());
        return CalendarResponse.fromWithTaskCount(calendar, taskCount);
    }

    /**
     * Get default calendar for current user
     */
    @Transactional(readOnly = true)
    public CalendarResponse getDefaultCalendar() {
        User currentUser = userService.getCurrentUser();
        logger.debug("Fetching default calendar for user: {}", currentUser.getUsername());

        Calendar calendar = calendarRepository.findByUserAndIsDefaultTrue(currentUser)
                .orElseThrow(() -> new RuntimeException("Default calendar not found"));

        Long taskCount = taskRepository.countByUser_IdAndCalendar_Id(currentUser.getId(), calendar.getId());
        return CalendarResponse.fromWithTaskCount(calendar, taskCount);
    }

    /**
     * Get default calendar entity (for internal use)
     */
    @Transactional(readOnly = true)
    public Calendar getDefaultCalendarEntity(User user) {
        return calendarRepository.findByUserAndIsDefaultTrue(user)
                .orElseThrow(() -> new RuntimeException("Default calendar not found for user: " + user.getUsername()));
    }

    /**
     * Create a new calendar
     */
    public CalendarResponse createCalendar(CalendarRequest request) {
        User currentUser = userService.getCurrentUser();
        logger.debug("Creating calendar '{}' for user: {}", request.getName(), currentUser.getUsername());

        // Validate slug uniqueness
        if (calendarRepository.existsByUserAndSlug(currentUser, request.getSlug())) {
            throw new RuntimeException("Calendar with slug '" + request.getSlug() + "' already exists");
        }

        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaults(currentUser);
        }

        // Create calendar
        Calendar calendar = new Calendar();
        calendar.setUser(currentUser);
        calendar.setName(request.getName().trim());
        calendar.setSlug(request.getSlug().toLowerCase().trim());
        calendar.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        calendar.setColor(request.getColor() != null ? request.getColor() : "#3788d8");
        calendar.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        calendar.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        calendar.setTimezone(request.getTimezone() != null ? request.getTimezone() : currentUser.getTimezone());

        Calendar savedCalendar = calendarRepository.save(calendar);
        logger.info("Calendar '{}' created successfully for user: {}", savedCalendar.getName(), currentUser.getUsername());

        return CalendarResponse.fromWithTaskCount(savedCalendar, 0L);
    }

    /**
     * Update an existing calendar
     */
    public CalendarResponse updateCalendar(Long id, CalendarRequest request) {
        User currentUser = userService.getCurrentUser();
        logger.debug("Updating calendar {} for user: {}", id, currentUser.getUsername());

        Calendar calendar = calendarRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Calendar not found"));

        // Validate slug uniqueness if changed
        if (!calendar.getSlug().equals(request.getSlug())) {
            if (calendarRepository.existsByUserAndSlug(currentUser, request.getSlug())) {
                throw new RuntimeException("Calendar with slug '" + request.getSlug() + "' already exists");
            }
        }

        // If this is being set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(calendar.getIsDefault())) {
            unsetOtherDefaults(currentUser);
        }

        // Update fields
        calendar.setName(request.getName().trim());
        calendar.setSlug(request.getSlug().toLowerCase().trim());
        calendar.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        calendar.setColor(request.getColor() != null ? request.getColor() : "#3788d8");
        calendar.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        calendar.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        calendar.setTimezone(request.getTimezone() != null ? request.getTimezone() : currentUser.getTimezone());

        Calendar updatedCalendar = calendarRepository.save(calendar);
        logger.info("Calendar '{}' updated successfully", updatedCalendar.getName());

        Long taskCount = taskRepository.countByUser_IdAndCalendar_Id(currentUser.getId(), calendar.getId());
        return CalendarResponse.fromWithTaskCount(updatedCalendar, taskCount);
    }

    /**
     * Delete a calendar
     * Note: Cannot delete default calendar if it has tasks
     */
    public void deleteCalendar(Long id) {
        User currentUser = userService.getCurrentUser();
        logger.debug("Deleting calendar {} for user: {}", id, currentUser.getUsername());

        Calendar calendar = calendarRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Calendar not found"));

        // Prevent deletion of default calendar if it has tasks
        if (Boolean.TRUE.equals(calendar.getIsDefault())) {
            long taskCount = taskRepository.countByUser_IdAndCalendar_Id(currentUser.getId(), calendar.getId());
            if (taskCount > 0) {
                throw new RuntimeException("Cannot delete default calendar with existing tasks. Move or delete tasks first.");
            }
        }

        calendarRepository.delete(calendar);
        logger.info("Calendar '{}' deleted successfully", calendar.getName());
    }

    /**
     * Unset isDefault flag for all other calendars of the user
     */
    private void unsetOtherDefaults(User user) {
        List<Calendar> calendars = calendarRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user);
        calendars.stream()
                .filter(cal -> Boolean.TRUE.equals(cal.getIsDefault()))
                .forEach(cal -> {
                    cal.setIsDefault(false);
                    calendarRepository.save(cal);
                });
    }

    /**
     * Get calendar by slug and username (for CalDAV path resolution)
     */
    @Transactional(readOnly = true)
    public Calendar getCalendarBySlugAndUsername(String slug, String username) {
        return calendarRepository.findBySlugAndUsername(slug, username)
                .orElseThrow(() -> new RuntimeException("Calendar not found: " + username + "/" + slug));
    }

    /**
     * Ensure user has a default calendar (creates one if missing)
     * Used during user registration or migration
     */
    public Calendar ensureDefaultCalendar(User user) {
        return calendarRepository.findByUserAndIsDefaultTrue(user)
                .orElseGet(() -> {
                    logger.warn("No default calendar found for user {}, creating one", user.getUsername());
                    Calendar defaultCalendar = new Calendar();
                    defaultCalendar.setUser(user);
                    defaultCalendar.setName("Default Calendar");
                    defaultCalendar.setSlug("default");
                    defaultCalendar.setDescription("Your default calendar");
                    defaultCalendar.setColor("#3788d8");
                    defaultCalendar.setIsDefault(true);
                    defaultCalendar.setIsVisible(true);
                    defaultCalendar.setTimezone(user.getTimezone());
                    return calendarRepository.save(defaultCalendar);
                });
    }
}
