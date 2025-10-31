package com.privatecal.service;

import com.privatecal.dto.*;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for task CRUD operations and task-related business logic
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ReminderRepository reminderRepository;
    private final UserService userService;
    private final ReminderService reminderService;
    private final RecurrenceService recurrenceService;
    private final CalendarService calendarService;
    
    /**
     * Create a new task
     */
    public TaskResponse createTask(TaskRequest taskRequest) {
        logger.debug("Creating new task: {}", taskRequest.getTitle());
        
        // Get current user
        User currentUser = userService.getCurrentUser();
        
        // Validate task request
        validateTaskRequest(taskRequest);
        
        // Get default calendar for user
        com.privatecal.entity.Calendar defaultCalendar = calendarService.getDefaultCalendarEntity(currentUser);

        // Create task entity
        Task task = new Task();
        task.setUser(currentUser);
        task.setCalendar(defaultCalendar);
        task.setTitle(taskRequest.getTitle().trim());
        task.setDescription(taskRequest.getDescription() != null ? taskRequest.getDescription().trim() : null);

        // Floating time fields (DST-safe)
        task.setStartDatetimeLocal(taskRequest.getStartDatetimeLocal());
        task.setEndDatetimeLocal(taskRequest.getEndDatetimeLocal());
        task.setTaskTimezone(taskRequest.getTimezone());
        // Deprecated UTC fields are auto-synced in Task entity's @PrePersist

        task.setColor(taskRequest.getColor() != null ? taskRequest.getColor() : "#3788d8");
        task.setLocation(taskRequest.getLocation() != null ? taskRequest.getLocation().trim() : null);
        task.setIsAllDay(taskRequest.getIsAllDay() != null ? taskRequest.getIsAllDay() : false);
        // Generate UID if not provided (required for CalDAV compliance)
        task.setUid(StringUtils.hasText(taskRequest.getUid()) ? taskRequest.getUid() : java.util.UUID.randomUUID().toString());
        task.setRecurrenceRule(taskRequest.getRecurrenceRule());
        task.setRecurrenceExceptions(taskRequest.getRecurrenceExceptions());

        // Convert recurrenceEnd from LocalDateTime to Instant
        if (taskRequest.getRecurrenceEnd() != null) {
            task.setRecurrenceEnd(taskRequest.getRecurrenceEnd()
                .atZone(ZoneId.of(taskRequest.getTimezone()))
                .toInstant());
        } else {
            task.setRecurrenceEnd(null);
        }

        // Save task
        Task savedTask = taskRepository.save(task);

        // Create reminders if provided
        if (taskRequest.getReminders() != null && !taskRequest.getReminders().isEmpty()) {
            for (ReminderRequest reminderRequest : taskRequest.getReminders()) {
                reminderService.createReminderForTask(savedTask.getUid(), reminderRequest);
            }
        }
        
        // Reload task with reminders
        savedTask = taskRepository.findById(savedTask.getUid()).orElse(savedTask);
        // savedTask = taskRepository.findById(savedTask.getUid()).orElseThrow( () -> new NullPointerException("Task not found after creation") );
        
        logger.info("Task created successfully: {} for user: {}", savedTask.getTitle(), currentUser.getUsername());
        
        return TaskResponse.fromTask(savedTask);
    }
    
    /**
     * Get task by UID (with user ownership validation)
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(String taskUid) {
        User currentUser = userService.getCurrentUser();

        Task task = taskRepository.findByUidAndUser(taskUid, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        return TaskResponse.fromTask(task);
    }
    
    /**
     * Get all tasks for current user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllUserTasks() {
        User currentUser = userService.getCurrentUser();
        List<Task> tasks = taskRepository.findByUserOrderByStartDatetimeAsc(currentUser);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
    }
    
    /**
     * Get tasks in date range for current user (with recurring task expansion)
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksInDateRange(Instant startDate, Instant endDate) {
        User currentUser = userService.getCurrentUser();
        List<Task> tasks = taskRepository.findTasksInDateRangeForUser(currentUser, startDate, endDate);

        // Expand recurring tasks into occurrences
        List<TaskResponse> expandedTasks = new java.util.ArrayList<>();

        for (Task task : tasks) {
            if (task.isRecurring()) {
                // Expand recurring task
                List<RecurrenceService.TaskOccurrence> occurrences =
                    recurrenceService.expandRecurrences(task, startDate, endDate);

                // Convert occurrences to TaskResponse with adjusted dates
                for (RecurrenceService.TaskOccurrence occurrence : occurrences) {
                    TaskResponse response = TaskResponse.fromTask(task);
                    // Convert occurrence UTC times to local time using task's timezone
                    ZoneId taskZone = ZoneId.of(task.getTaskTimezone());
                    response.setStartDatetimeLocal(occurrence.getOccurrenceStart().atZone(taskZone).toLocalDateTime());
                    response.setEndDatetimeLocal(occurrence.getOccurrenceEnd().atZone(taskZone).toLocalDateTime());
                    response.setTimezone(task.getTaskTimezone());
                    // Generate unique occurrence ID: taskId-epochMillis
                    response.setOccurrenceId(task.getUid() + "-" + occurrence.getOccurrenceStart().toEpochMilli());
                    expandedTasks.add(response);
                }
            } else {
                // Non-recurring task - use task ID as occurrence ID
                TaskResponse response = TaskResponse.fromTask(task);
                response.setOccurrenceId(String.valueOf(task.getUid()));
                expandedTasks.add(response);
            }
        }

        return expandedTasks;
    }
    
    /**
     * Get tasks in date range with pagination
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksInDateRange(Instant startDate, Instant endDate, 
                                                 int page, int size, String sortBy, String sortDir) {
        User currentUser = userService.getCurrentUser();
        
        // Create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // For now, we'll get all tasks and then paginate (could be optimized with custom query)
        List<Task> tasks = taskRepository.findTasksInDateRangeForUser(currentUser, startDate, endDate);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList())
                .stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> new org.springframework.data.domain.PageImpl<>(list, pageable, tasks.size())
                ));
    }
    
    /**
     * Get today's tasks for current user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTodayTasks() {
        Long currentUserId = userService.getCurrentUserId();

        // Calculate start and end of day in UTC
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        List<Task> tasks = taskRepository.findTodayTasksForUser(currentUserId, startOfDay, endOfDay);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
    }
    
    /**
     * Get upcoming tasks for current user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getUpcomingTasks(int limit) {
        User currentUser = userService.getCurrentUser();
        Instant now = Instant.now();
        List<Task> tasks = taskRepository.findUpcomingTasksForUser(currentUser, now);
        
        return tasks.stream()
                .limit(limit)
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
    }
    
    /**
     * Get overdue tasks for current user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks() {
        User currentUser = userService.getCurrentUser();
        Instant now = Instant.now();
        List<Task> tasks = taskRepository.findOverdueTasksForUser(currentUser, now);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
    }
    
    /**
     * Search tasks for current user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> searchTasks(String searchTerm) {
        User currentUser = userService.getCurrentUser();
        List<Task> tasks = taskRepository.findTasksBySearchTermForUser(currentUser, searchTerm);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
    }
    
    /**
     * Update task
     */
    public TaskResponse updateTask(String taskUid, TaskRequest taskRequest) {
        logger.debug("Updating task UID: {}", taskUid);

        User currentUser = userService.getCurrentUser();

        // Find task and validate ownership
        Task task = taskRepository.findByUidAndUser(taskUid, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Validate task request
        validateTaskRequest(taskRequest);


        // Update task fields
        task.setTitle(taskRequest.getTitle().trim());
        task.setDescription(taskRequest.getDescription() != null ? taskRequest.getDescription().trim() : null);

        // For recurring tasks, preserve original dates to avoid shifting the series
        // Only update dates if the task is not recurring OR if recurrence rule is being changed
        boolean isRecurring = task.getRecurrenceRule() != null && !task.getRecurrenceRule().trim().isEmpty();
        boolean recurrenceRuleChanged = !Objects.equals(taskRequest.getRecurrenceRule(), task.getRecurrenceRule());
        if (!isRecurring || recurrenceRuleChanged) {
            // Update dates only for non-recurring tasks or when recurrence rule changes
            task.setStartDatetimeLocal(taskRequest.getStartDatetimeLocal());
            task.setEndDatetimeLocal(taskRequest.getEndDatetimeLocal());
            // Deprecated UTC fields are auto-synced in Task entity's @PrePersist
        } else {
            // For recurring tasks with unchanged recurrence rule, keep original dates
            logger.info("Preserving original dates for recurring task {} (start: {}, end: {})",
                       taskUid, task.getStartDatetimeLocal(), task.getEndDatetimeLocal());
        }

        // timezone should be updated despite of single occurrence rule update (update reminder time fix on changed timezone)
        task.setTaskTimezone(taskRequest.getTimezone());

        task.setColor(taskRequest.getColor() != null ? taskRequest.getColor() : "#3788d8");
        task.setLocation(taskRequest.getLocation() != null ? taskRequest.getLocation().trim() : null);
        task.setIsAllDay(taskRequest.getIsAllDay() != null ? taskRequest.getIsAllDay() : false);
        // DO NOT update UID - it's the primary key and should never change
        // task.setUid(taskRequest.getUid());
        task.setRecurrenceRule(taskRequest.getRecurrenceRule());
        task.setRecurrenceExceptions(taskRequest.getRecurrenceExceptions());

        // Convert recurrenceEnd from LocalDateTime to Instant
        if (taskRequest.getRecurrenceEnd() != null) {
            task.setRecurrenceEnd(taskRequest.getRecurrenceEnd()
                .atZone(ZoneId.of(taskRequest.getTimezone()))
                .toInstant());
        } else {
            task.setRecurrenceEnd(null);
        }

        // Save task
        Task savedTask = taskRepository.save(task);

        // Update reminders if provided
        if (taskRequest.getReminders() != null) {
            // Get existing reminders for comparison and explicit deletion
            List<Reminder> existingReminders = reminderRepository.findByTask_UidOrderByReminderTimeAsc(taskUid);

            // Delete existing reminders explicitly
            if (!existingReminders.isEmpty()) {
                reminderRepository.deleteAll(existingReminders);
                reminderRepository.flush(); // Force immediate execution of delete in PostgreSQL
            }

            // Add new reminders
            for (ReminderRequest reminderRequest : taskRequest.getReminders()) {
                reminderService.createReminderForTask(taskUid, reminderRequest);
            }

            // Force flush of inserts as well to ensure consistency
            reminderRepository.flush();
        }
        
        // Reload task with reminders
        savedTask = taskRepository.findById(savedTask.getUid()).orElse(savedTask);
        
        logger.info("Task updated successfully: {} for user: {}", savedTask.getTitle(), currentUser.getUsername());


        return TaskResponse.fromTask(savedTask);
    }

    /**
     * Update a single occurrence of a recurring task
     * Creates a new non-recurring task and adds an exception to the master task
     *
     * @param taskUid The UID of the recurring master task
     * @param occurrenceStart The start datetime of the occurrence to edit
     * @param taskRequest The updated task data
     * @return The newly created task for the single occurrence
     */
    public TaskResponse updateSingleOccurrence(String taskUid, LocalDateTime occurrenceStartLocal, TaskRequest taskRequest) {
        logger.info("Updating single occurrence of task UID {} at {} (local time)", taskUid, occurrenceStartLocal);

        User currentUser = userService.getCurrentUser();

        // Find master task and validate ownership
        Task masterTask = taskRepository.findByUidAndUser(taskUid, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Verify it's a recurring task
        if (masterTask.getRecurrenceRule() == null || masterTask.getRecurrenceRule().trim().isEmpty()) {
            throw new RuntimeException("Task is not recurring");
        }

        // Validate task request
        validateTaskRequest(taskRequest);

        // Add exception date to master task (EXDATE) using local datetime
        recurrenceService.addExceptionDate(masterTask, occurrenceStartLocal);
        taskRepository.save(masterTask);

        logger.info("Added EXDATE {} to master task {}", occurrenceStartLocal, masterTask.getUid());

        // Create new non-recurring task for this specific occurrence
        Task newTask = new Task();
        newTask.setUser(currentUser);
        newTask.setCalendar(masterTask.getCalendar()); // Use same calendar as master task
        newTask.setTitle(taskRequest.getTitle().trim());
        newTask.setDescription(taskRequest.getDescription() != null ? taskRequest.getDescription().trim() : null);
        newTask.setStartDatetimeLocal(taskRequest.getStartDatetimeLocal());
        newTask.setEndDatetimeLocal(taskRequest.getEndDatetimeLocal());
        newTask.setTaskTimezone(taskRequest.getTimezone());
        newTask.setColor(taskRequest.getColor() != null ? taskRequest.getColor() : masterTask.getColor());
        newTask.setLocation(taskRequest.getLocation() != null ? taskRequest.getLocation().trim() : null);
        newTask.setIsAllDay(taskRequest.getIsAllDay() != null ? taskRequest.getIsAllDay() : false);
        // Generate new UID for single occurrence (different from master task, required for CalDAV)
        newTask.setUid(java.util.UUID.randomUUID().toString());
        // No recurrence for single occurrence
        newTask.setRecurrenceRule(null);
        newTask.setRecurrenceEnd(null);

        Task savedTask = taskRepository.save(newTask);

        // Add reminders if provided
        if (taskRequest.getReminders() != null) {
            for (ReminderRequest reminderRequest : taskRequest.getReminders()) {
                reminderService.createReminderForTask(savedTask.getUid(), reminderRequest);
            }
        }

        // Reload task with reminders
        savedTask = taskRepository.findById(savedTask.getUid()).orElse(savedTask);

        logger.info("Created new task {} for single occurrence edit", savedTask.getUid());

        return TaskResponse.fromTask(savedTask);
    }

    /**
     * Delete task
     */
    public void deleteTask(String taskUid) {
        logger.debug("Deleting task UID: {}", taskUid);

        User currentUser = userService.getCurrentUser();

        // Find task and validate ownership
        Task task = taskRepository.findByUidAndUser(taskUid, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Delete task (cascade will delete reminders)
        taskRepository.delete(task);

        logger.info("Task deleted successfully: {} for user: {}", task.getTitle(), currentUser.getUsername());
    }
    
    /**
     * Get task count for user
     */
    @Transactional(readOnly = true)
    public long getTaskCountForUser() {
        User currentUser = userService.getCurrentUser();
        return taskRepository.countByUser(currentUser);
    }
    
    /**
     * Get tasks with due reminders (for notification processing)
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksWithDueReminders() {
        Instant currentTime = Instant.now();
        List<Task> tasks = taskRepository.findTasksWithDueReminders(currentTime);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate task request
     */
    private void validateTaskRequest(TaskRequest taskRequest) {
        if (taskRequest.getTitle() == null || taskRequest.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Task title is required");
        }

        if (taskRequest.getStartDatetimeLocal() == null) {
            throw new RuntimeException("Start datetime (local) is required");
        }

        if (taskRequest.getEndDatetimeLocal() == null) {
            throw new RuntimeException("End datetime (local) is required");
        }

        if (taskRequest.getTimezone() == null || taskRequest.getTimezone().trim().isEmpty()) {
            throw new RuntimeException("Timezone is required");
        }

        if (!taskRequest.getEndDatetimeLocal().isAfter(taskRequest.getStartDatetimeLocal())) {
            throw new RuntimeException("End datetime must be after start datetime");
        }

        if (taskRequest.getColor() != null && !taskRequest.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new RuntimeException("Color must be a valid hex color (e.g., #3788d8)");
        }

        // Validate recurrence rule if provided
        if (taskRequest.getRecurrenceRule() != null && !taskRequest.getRecurrenceRule().trim().isEmpty()) {
            if (!recurrenceService.isValidRecurrenceRule(taskRequest.getRecurrenceRule())) {
                throw new RuntimeException("Invalid recurrence rule format (must be RFC 5545 RRULE)");
            }
        }

        // Validate recurrence end if provided (both are LocalDateTime now)
        if (taskRequest.getRecurrenceEnd() != null && taskRequest.getStartDatetimeLocal() != null) {
            if (!taskRequest.getRecurrenceEnd().isAfter(taskRequest.getStartDatetimeLocal())) {
                throw new RuntimeException("Recurrence end must be after start datetime");
            }
        }
    }
    
    /**
     * Get task statistics for user
     */
    @Transactional(readOnly = true)
    public TaskStatistics getTaskStatistics() {
        User currentUser = userService.getCurrentUser();
        Instant now = Instant.now();
        
        long totalTasks = taskRepository.countByUser(currentUser);
        
        // Count today's tasks
        // Calculate start and end of day in UTC for today's tasks
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        List<Task> todayTasks = taskRepository.findTodayTasksForUser(currentUser.getId(), startOfDay, endOfDay);
        
        // Count upcoming tasks
        List<Task> upcomingTasks = taskRepository.findUpcomingTasksForUser(currentUser, now);
        
        // Count overdue tasks (tasks that ended before now)
        long overdueTasks = taskRepository.findByUserOrderByStartDatetimeAsc(currentUser)
                .stream()
                .filter(task -> task.getEndDatetime().isBefore(now))
                .count();
        
        return new TaskStatistics(totalTasks, todayTasks.size(), upcomingTasks.size(), overdueTasks);
    }
    
    /**
     * Task statistics inner class
     */
    public static class TaskStatistics {
        private final long totalTasks;
        private final long todayTasks;
        private final long upcomingTasks;
        private final long overdueTasks;
        
        public TaskStatistics(long totalTasks, long todayTasks, long upcomingTasks, long overdueTasks) {
            this.totalTasks = totalTasks;
            this.todayTasks = todayTasks;
            this.upcomingTasks = upcomingTasks;
            this.overdueTasks = overdueTasks;
        }
        
        // Getters
        public long getTotalTasks() { return totalTasks; }
        public long getTodayTasks() { return todayTasks; }
        public long getUpcomingTasks() { return upcomingTasks; }
        public long getOverdueTasks() { return overdueTasks; }
    }
    
    /**
     * Clone/duplicate a task
     */
    public TaskResponse cloneTask(String taskUid, Instant newStartTime) {
        User currentUser = userService.getCurrentUser();

        // Find original task
        Task originalTask = taskRepository.findByUidAndUser(taskUid, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Calculate duration and new end time
        long durationMinutes = java.time.Duration.between(
            originalTask.getStartDatetimeLocal(),
            originalTask.getEndDatetimeLocal()
        ).toMinutes();
        LocalDateTime newEndTimeLocal = newStartTime.atZone(ZoneId.of(originalTask.getTaskTimezone()))
                .toLocalDateTime()
                .plus(java.time.Duration.ofMinutes(durationMinutes));

        // Create task request for the clone
        TaskRequest cloneRequest = new TaskRequest();
        cloneRequest.setTitle(originalTask.getTitle() + " (Copy)");
        cloneRequest.setDescription(originalTask.getDescription());
        cloneRequest.setStartDatetimeLocal(newStartTime.atZone(ZoneId.of(originalTask.getTaskTimezone())).toLocalDateTime());
        cloneRequest.setEndDatetimeLocal(newEndTimeLocal);
        cloneRequest.setTimezone(originalTask.getTaskTimezone());
        cloneRequest.setColor(originalTask.getColor());
        cloneRequest.setLocation(originalTask.getLocation());
        
        // Copy reminders
        List<ReminderRequest> reminderRequests = originalTask.getReminders().stream()
                .map(reminder -> new ReminderRequest(
                    reminder.getReminderOffsetMinutes(),
                    reminder.getNotificationType()
                ))
                .collect(Collectors.toList());
        cloneRequest.setReminders(reminderRequests);
        
        return createTask(cloneRequest);
    }
}