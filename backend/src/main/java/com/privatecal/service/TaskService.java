package com.privatecal.service;

import com.privatecal.dto.*;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for task CRUD operations and task-related business logic
 */
@Service
@Transactional
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ReminderRepository reminderRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ReminderService reminderService;
    
    /**
     * Create a new task
     */
    public TaskResponse createTask(TaskRequest taskRequest) {
        logger.debug("Creating new task: {}", taskRequest.getTitle());
        
        // Get current user
        User currentUser = userService.getCurrentUser();
        
        // Validate task request
        validateTaskRequest(taskRequest);
        
        // Check for time conflicts
        if (hasTimeConflict(null, currentUser.getId(), taskRequest.getStartDatetime(), taskRequest.getEndDatetime())) {
            logger.warn("Task creation failed - time conflict for user: {}", currentUser.getUsername());
            throw new RuntimeException("Time conflict: You already have a task scheduled during this time");
        }
        
        // Create task entity
        Task task = new Task();
        task.setUser(currentUser);
        task.setTitle(taskRequest.getTitle().trim());
        task.setDescription(taskRequest.getDescription() != null ? taskRequest.getDescription().trim() : null);
        task.setStartDatetime(taskRequest.getStartDatetime());
        task.setEndDatetime(taskRequest.getEndDatetime());
        task.setColor(taskRequest.getColor() != null ? taskRequest.getColor() : "#3788d8");
        task.setLocation(taskRequest.getLocation() != null ? taskRequest.getLocation().trim() : null);

        // Save task
        Task savedTask = taskRepository.save(task);

        // Create reminders if provided
        if (taskRequest.getReminders() != null && !taskRequest.getReminders().isEmpty()) {
            for (ReminderRequest reminderRequest : taskRequest.getReminders()) {
                reminderService.createReminderForTask(savedTask.getId(), reminderRequest);
            }
        }
        
        // Reload task with reminders
        savedTask = taskRepository.findById(savedTask.getId()).orElse(savedTask);
        // savedTask = taskRepository.findById(savedTask.getId()).orElseThrow( () -> new NullPointerException("Task not found after creation") );
        
        logger.info("Task created successfully: {} for user: {}", savedTask.getTitle(), currentUser.getUsername());
        
        return TaskResponse.fromTask(savedTask);
    }
    
    /**
     * Get task by ID (with user ownership validation)
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        User currentUser = userService.getCurrentUser();
        
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
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
     * Get tasks in date range for current user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksInDateRange(Instant startDate, Instant endDate) {
        User currentUser = userService.getCurrentUser();
        List<Task> tasks = taskRepository.findTasksInDateRangeForUser(currentUser, startDate, endDate);
        
        return tasks.stream()
                .map(TaskResponse::fromTask)
                .collect(Collectors.toList());
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
        Instant today = Instant.now();
        List<Task> tasks = taskRepository.findTodayTasksForUser(currentUserId, today);
        
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
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest) {
        logger.debug("Updating task ID: {}", taskId);
        
        User currentUser = userService.getCurrentUser();
        
        // Find task and validate ownership
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Validate task request
        validateTaskRequest(taskRequest);
        
        // Check for time conflicts (excluding current task)
        if (hasTimeConflict(taskId, currentUser.getId(), taskRequest.getStartDatetime(), taskRequest.getEndDatetime())) {
            logger.warn("Task update failed - time conflict for user: {}", currentUser.getUsername());
            throw new RuntimeException("Time conflict: You already have another task scheduled during this time");
        }
        
        // Update task fields
        task.setTitle(taskRequest.getTitle().trim());
        task.setDescription(taskRequest.getDescription() != null ? taskRequest.getDescription().trim() : null);
        task.setStartDatetime(taskRequest.getStartDatetime());
        task.setEndDatetime(taskRequest.getEndDatetime());
        task.setColor(taskRequest.getColor() != null ? taskRequest.getColor() : "#3788d8");
        task.setLocation(taskRequest.getLocation() != null ? taskRequest.getLocation().trim() : null);

        // Save task
        Task savedTask = taskRepository.save(task);

        // Update reminders if provided
        if (taskRequest.getReminders() != null) {
            // Remove existing reminders
            reminderRepository.deleteByTask_Id(taskId);
            
            // Add new reminders
            for (ReminderRequest reminderRequest : taskRequest.getReminders()) {
                reminderService.createReminderForTask(taskId, reminderRequest);
            }
        }
        
        // Reload task with reminders
        savedTask = taskRepository.findById(savedTask.getId()).orElse(savedTask);
        
        logger.info("Task updated successfully: {} for user: {}", savedTask.getTitle(), currentUser.getUsername());
        
        return TaskResponse.fromTask(savedTask);
    }
    
    /**
     * Delete task
     */
    public void deleteTask(Long taskId) {
        logger.debug("Deleting task ID: {}", taskId);
        
        User currentUser = userService.getCurrentUser();
        
        // Find task and validate ownership
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Delete task (cascade will delete reminders)
        taskRepository.delete(task);
        
        logger.info("Task deleted successfully: {} for user: {}", task.getTitle(), currentUser.getUsername());
    }
    
    /**
     * Delete multiple tasks
     */
    public void deleteTasks(List<Long> taskIds) {
        logger.debug("Deleting {} tasks", taskIds.size());
        
        User currentUser = userService.getCurrentUser();
        
        for (Long taskId : taskIds) {
            try {
                deleteTask(taskId);
            } catch (Exception e) {
                logger.warn("Failed to delete task ID {}: {}", taskId, e.getMessage());
            }
        }
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
     * Check if user has tasks in date range
     */
    @Transactional(readOnly = true)
    public boolean hasTasksInDateRange(Instant startDate, Instant endDate) {
        User currentUser = userService.getCurrentUser();
        List<Task> tasks = taskRepository.findTasksInDateRangeForUser(currentUser, startDate, endDate);
        return !tasks.isEmpty();
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
        
        if (taskRequest.getStartDatetime() == null) {
            throw new RuntimeException("Start datetime is required");
        }
        
        if (taskRequest.getEndDatetime() == null) {
            throw new RuntimeException("End datetime is required");
        }
        
        if (!taskRequest.getEndDatetime().isAfter(taskRequest.getStartDatetime())) {
            throw new RuntimeException("End datetime must be after start datetime");
        }

        if (taskRequest.getColor() != null && !taskRequest.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new RuntimeException("Color must be a valid hex color (e.g., #3788d8)");
        }
    }
    
    /**
     * Check for time conflicts
     */
    private boolean hasTimeConflict(Long excludeTaskId, Long userId, Instant startTime, Instant endTime) {
        if (excludeTaskId != null) {
            return taskRepository.countConflictingTasksExcludingTask(userId, excludeTaskId, startTime, endTime) > 0;
        } else {
            return taskRepository.countConflictingTasks(userId, startTime, endTime) > 0;
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
        List<Task> todayTasks = taskRepository.findTodayTasksForUser(currentUser.getId(), now);
        
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
    public TaskResponse cloneTask(Long taskId, Instant newStartTime) {
        User currentUser = userService.getCurrentUser();
        
        // Find original task
        Task originalTask = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Calculate duration and new end time
        long durationMinutes = java.time.Duration.between(
            originalTask.getStartDatetime(), 
            originalTask.getEndDatetime()
        ).toMinutes();
        Instant newEndTime = newStartTime.plus(java.time.Duration.ofMinutes(durationMinutes));
        
        // Create task request for the clone
        TaskRequest cloneRequest = new TaskRequest();
        cloneRequest.setTitle(originalTask.getTitle() + " (Copy)");
        cloneRequest.setDescription(originalTask.getDescription());
        cloneRequest.setStartDatetime(newStartTime);
        cloneRequest.setEndDatetime(newEndTime);
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