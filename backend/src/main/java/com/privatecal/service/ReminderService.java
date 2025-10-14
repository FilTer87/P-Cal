package com.privatecal.service;

import com.privatecal.dto.ReminderRequest;
import com.privatecal.dto.ReminderResponse;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for reminder management and notification processing
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RecurrenceService recurrenceService;
    
    /**
     * Create reminder for a task
     */
    public ReminderResponse createReminderForTask(Long taskId, ReminderRequest reminderRequest) {
        logger.debug("Creating reminder for task ID: {}", taskId);

        User currentUser = userService.getCurrentUser();

        // Find task and validate ownership
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (logger.isDebugEnabled()) {
            logger.debug("📅 ReminderService.createReminderForTask: task retrieved from DB");
            logger.debug("  task.getId() = {}", task.getId());
            logger.debug("  task.getStartDatetime() = {}", task.getStartDatetime());
            logger.debug("  task.getRecurrenceRule() = {}", task.getRecurrenceRule());
        }

        // Validate reminder request
        validateReminderRequest(reminderRequest);

        // Create reminder entity
        Reminder reminder = new Reminder();
        reminder.setTask(task);
        reminder.setReminderOffsetMinutes(reminderRequest.getReminderOffsetMinutes());
        reminder.setNotificationType(reminderRequest.getNotificationType());

        // For recurring tasks, calculate reminder time based on next occurrence
        if (task.getRecurrenceRule() != null && !task.getRecurrenceRule().trim().isEmpty()) {
            Instant now = Instant.now();
            Instant occurrenceStart;

            // If task hasn't started yet, use the original task start time
            if (task.getStartDatetime().isAfter(now)) {
                occurrenceStart = task.getStartDatetime();
                logger.info("Recurring task hasn't started yet, using original start time: {}", occurrenceStart);
            } else {
                // Task has already started - find the next occurrence from now
                RecurrenceService.TaskOccurrence nextOccurrence =
                    recurrenceService.getNextOccurrence(task, now.minusSeconds(2));

                if (nextOccurrence != null) {
                    occurrenceStart = nextOccurrence.getOccurrenceStart();
                    logger.info("Recurring task started in past, next occurrence at: {}", occurrenceStart);

                    // Set lastSentOccurrence to indicate this is a recurring reminder
                    // This is important for the findDueReminders query to work correctly
                    // even when the original task endDatetime is in the past
                    // We set it to the task's original start date as a marker
                    reminder.setLastSentOccurrence(task.getStartDatetime());
                } else {
                    // No future occurrences - mark reminder as sent so it won't trigger
                    reminder.setReminderTime(task.getStartDatetime());
                    reminder.setIsSent(true);
                    logger.warn("No future occurrences found for recurring task {}, marking reminder as sent",
                               task.getId());
                    Reminder savedReminder = reminderRepository.save(reminder);
                    return ReminderResponse.fromReminder(savedReminder);
                }
            }

            // Calculate reminder time: occurrence start - offset
            Instant reminderTime = occurrenceStart
                .minus(java.time.Duration.ofMinutes(reminderRequest.getReminderOffsetMinutes()));

            reminder.setReminderTime(reminderTime);

            logger.info("Recurring task reminder: occurrence at {}, reminder time set to {}",
                       occurrenceStart, reminderTime);
        } else {
            // Non-recurring task: use normal calculation
            reminder.calculateReminderTime();

            // Validate reminder time
            if (reminder.getReminderTime() != null && reminder.getReminderTime().isAfter(task.getStartDatetime())) {
                throw new RuntimeException("Reminder time cannot be after task start time");
            }
        }

        // Save reminder
        Reminder savedReminder = reminderRepository.save(reminder);

        logger.info("Reminder created for task: {} at {} minutes before ({})",
                   task.getTitle(), reminderRequest.getReminderOffsetMinutes(), savedReminder.getReminderTime());

        return ReminderResponse.fromReminder(savedReminder);
    }
    
    /**
     * Get reminder by ID (with user ownership validation)
     */
    @Transactional(readOnly = true)
    public ReminderResponse getReminderById(Long reminderId) {
        Long currentUserId = userService.getCurrentUserId();
        
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        
        return ReminderResponse.fromReminder(reminder);
    }
    
    /**
     * Get all reminders for a task
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersForTask(Long taskId) {
        User currentUser = userService.getCurrentUser();
        
        // Validate task ownership
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        List<Reminder> reminders = reminderRepository.findByTaskOrderByReminderTimeAsc(task);
        
        return reminders.stream()
                .map(ReminderResponse::fromReminder)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all upcoming reminders for current user
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getUpcomingReminders() {
        Long currentUserId = userService.getCurrentUserId();
        Instant now = Instant.now();
        
        List<Reminder> reminders = reminderRepository.findUpcomingRemindersForUser(currentUserId, now);
        
        return reminders.stream()
                .map(ReminderResponse::fromReminder)
                .collect(Collectors.toList());
    }
    
    /**
     * Get reminders in date range for current user
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersInDateRange(Instant startDate, Instant endDate) {
        Long currentUserId = userService.getCurrentUserId();
        
        List<Reminder> reminders = reminderRepository.findRemindersInDateRangeForUser(
                currentUserId, startDate, endDate);
        
        return reminders.stream()
                .map(ReminderResponse::fromReminder)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all reminders for current user
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getAllUserReminders() {
        Long currentUserId = userService.getCurrentUserId();
        
        List<Reminder> reminders = reminderRepository.findAllByUserId(currentUserId);
        
        return reminders.stream()
                .map(ReminderResponse::fromReminder)
                .collect(Collectors.toList());
    }
    
    /**
     * Update reminder
     */
    public ReminderResponse updateReminder(Long reminderId, ReminderRequest reminderRequest) {
        logger.debug("Updating reminder ID: {}", reminderId);
        
        Long currentUserId = userService.getCurrentUserId();
        
        // Find reminder and validate ownership
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        
        // Validate reminder request
        validateReminderRequest(reminderRequest);
        
        // Update reminder
        reminder.setReminderOffsetMinutes(reminderRequest.getReminderOffsetMinutes());
        reminder.setNotificationType(reminderRequest.getNotificationType());
        reminder.setIsSent(false); // Reset sent status
        reminder.calculateReminderTime();
        
        // Validate reminder time
        if (reminder.getReminderTime() != null && 
            reminder.getTask() != null && 
            reminder.getReminderTime().isAfter(reminder.getTask().getStartDatetime())) {
            throw new RuntimeException("Reminder time cannot be after task start time");
        }
        
        // Save reminder
        Reminder savedReminder = reminderRepository.save(reminder);
        
        logger.info("Reminder updated: {} minutes before task", 
                   reminderRequest.getReminderOffsetMinutes());
        
        return ReminderResponse.fromReminder(savedReminder);
    }
    
    /**
     * Delete reminder
     */
    public void deleteReminder(Long reminderId) {
        logger.debug("Deleting reminder ID: {}", reminderId);
        
        Long currentUserId = userService.getCurrentUserId();
        
        // Find reminder and validate ownership
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        
        // Delete reminder
        reminderRepository.delete(reminder);
        
        logger.info("Reminder deleted successfully");
    }
    
    /**
     * Delete all reminders for a task
     */
    public void deleteRemindersForTask(Long taskId) {
        logger.debug("Deleting all reminders for task ID: {}", taskId);
        
        User currentUser = userService.getCurrentUser();
        
        // Validate task ownership
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Delete reminders
        reminderRepository.deleteByTask(task);
        
        logger.info("All reminders deleted for task: {}", task.getTitle());
    }
    
    /**
     * Mark reminder as sent
     */
    public void markReminderAsSent(Long reminderId) {
        reminderRepository.markReminderAsSent(reminderId);
        logger.debug("Reminder {} marked as sent", reminderId);
    }
    
    /**
     * Mark multiple reminders as sent
     */
    public void markRemindersAsSent(List<Long> reminderIds) {
        reminderRepository.markRemindersAsSent(reminderIds);
        logger.debug("Marked {} reminders as sent", reminderIds.size());
    }
    
    
    /**
     * Get due reminders (ready to be sent)
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getDueReminders() {
        Instant currentTime = Instant.now();
        List<Reminder> reminders = reminderRepository.findDueReminders(currentTime);
        
        return reminders.stream()
                .map(ReminderResponse::fromReminder)
                .collect(Collectors.toList());
    }
    
    /**
     * Process due reminders - scheduled task that runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    @Async
    public void processDueReminders() {
        try {
            Instant currentTime = Instant.now();
            List<Reminder> dueReminders = reminderRepository.findDueReminders(currentTime);
            
            if (!dueReminders.isEmpty()) {
                logger.info("Processing {} due reminders", dueReminders.size());
                
                for (Reminder reminder : dueReminders) {
                    try {
                        processReminder(reminder);
                    } catch (Exception e) {
                        logger.error("Failed to process reminder ID {}: {}", 
                                   reminder.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing due reminders", e);
        }
    }
    
    /**
     * Process individual reminder
     */
    private void processReminder(Reminder reminder) {
        try {
            Task task = reminder.getTask();
            logger.debug("Processing reminder ID: {} for task: {}",
                        reminder.getId(), task.getTitle());

            // Send notification using unified service
            notificationService.sendReminderNotification(reminder);

            // Handle recurring task reminders
            if (task.getRecurrenceRule() != null && !task.getRecurrenceRule().trim().isEmpty()) {
                handleRecurringReminder(reminder, task);
            } else {
                // Non-recurring task: mark as sent
                markReminderAsSent(reminder.getId());
            }

            logger.info("Reminder processed successfully for task: {}", task.getTitle());

        } catch (Exception e) {
            logger.error("Failed to process reminder ID {}: {}",
                        reminder.getId(), e.getMessage());
        }
    }

    /**
     * Handle reminder for recurring task
     * Updates reminder time to next occurrence instead of marking as sent
     */
    private void handleRecurringReminder(Reminder reminder, Task task) {
        try {
            // Calculate which occurrence we just processed
            // reminderTime + offset = occurrence start time
            Instant currentOccurrenceStart = reminder.getReminderTime()
                .plus(java.time.Duration.ofMinutes(reminder.getReminderOffsetMinutes()));

            logger.info("Processing recurring reminder {}: current occurrence at {}, reminderTime was {}, offset {}min",
                        reminder.getId(), currentOccurrenceStart, reminder.getReminderTime(),
                        reminder.getReminderOffsetMinutes());

            // Get the next occurrence after the one we just processed
            RecurrenceService.TaskOccurrence nextOccurrence =
                recurrenceService.getNextOccurrence(task, currentOccurrenceStart);

            if (nextOccurrence != null) {
                // Calculate new reminder time: next occurrence start - offset
                Instant newReminderTime = nextOccurrence.getOccurrenceStart()
                    .minus(java.time.Duration.ofMinutes(reminder.getReminderOffsetMinutes()));

                logger.info("Next occurrence found at {}. New reminder time will be {}",
                           nextOccurrence.getOccurrenceStart(), newReminderTime);

                // Update reminder with new time and track which occurrence was processed
                reminder.setReminderTime(newReminderTime);
                reminder.setLastSentOccurrence(currentOccurrenceStart);
                reminder.setIsSent(false); // Keep as unsent so it will be processed again

                Reminder saved = reminderRepository.save(reminder);

                logger.info("Saved recurring reminder {}: reminderTime={}, lastSentOccurrence={}, isSent={}",
                           saved.getId(), saved.getReminderTime(), saved.getLastSentOccurrence(),
                           saved.getIsSent());
            } else {
                // No more occurrences - mark as sent (series is complete)
                reminder.setLastSentOccurrence(currentOccurrenceStart);
                markReminderAsSent(reminder.getId());
                logger.info("No more occurrences for recurring reminder {}, marked as sent",
                           reminder.getId());
            }

        } catch (Exception e) {
            logger.error("Error handling recurring reminder {}: {}",
                        reminder.getId(), e.getMessage());
            // Fallback: mark as sent to prevent infinite loop
            markReminderAsSent(reminder.getId());
        }
    }
    
    /**
     * Get reminder statistics for current user
     */
    @Transactional(readOnly = true)
    public ReminderStatistics getReminderStatistics() {
        Long currentUserId = userService.getCurrentUserId();
        Instant now = Instant.now();
        
        long totalReminders = reminderRepository.findUnsentRemindersForUser(currentUserId).size();
        long dueReminders = reminderRepository.findDueReminders(now)
                .stream()
                .filter(r -> r.getTask().getUser().getId().equals(currentUserId))
                .count();
        long overdueReminders = reminderRepository.countOverdueReminders(now);
        
        return new ReminderStatistics(totalReminders, dueReminders, overdueReminders);
    }
    
    /**
     * Validate reminder request
     */
    private void validateReminderRequest(ReminderRequest reminderRequest) {
        if (reminderRequest.getReminderOffsetMinutes() == null) {
            throw new RuntimeException("Reminder offset minutes is required");
        }
        
        if (reminderRequest.getReminderOffsetMinutes() < 0) {
            throw new RuntimeException("Reminder offset minutes must be non-negative");
        }
        
        if (reminderRequest.getNotificationType() == null) {
            throw new RuntimeException("Notification type is required");
        }
    }
    
    /**
     * Clean up old sent reminders (can be run periodically)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Async
    public void cleanupOldReminders() {
        try {
            Instant cutoffDate = Instant.now().minus(java.time.Duration.ofDays(30)); // Keep for 30 days
            reminderRepository.deleteOldSentReminders(cutoffDate);
            logger.info("Cleaned up old sent reminders older than {}", cutoffDate);
        } catch (Exception e) {
            logger.error("Error cleaning up old reminders", e);
        }
    }
    
    /**
     * Reminder statistics inner class
     */
    public static class ReminderStatistics {
        private final long totalReminders;
        private final long dueReminders;
        private final long overdueReminders;
        
        public ReminderStatistics(long totalReminders, long dueReminders, long overdueReminders) {
            this.totalReminders = totalReminders;
            this.dueReminders = dueReminders;
            this.overdueReminders = overdueReminders;
        }
        
        // Getters
        public long getTotalReminders() { return totalReminders; }
        public long getDueReminders() { return dueReminders; }
        public long getOverdueReminders() { return overdueReminders; }
    }
}