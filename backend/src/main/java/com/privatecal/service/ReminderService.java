package com.privatecal.service;

import com.privatecal.dto.ReminderRequest;
import com.privatecal.dto.ReminderResponse;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import com.privatecal.repository.ReminderRepository;
import com.privatecal.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for reminder management and notification processing
 */
@Service
@Transactional
public class ReminderService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);
    
    @Autowired
    private ReminderRepository reminderRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Create reminder for a task
     */
    public ReminderResponse createReminderForTask(Long taskId, ReminderRequest reminderRequest) {
        logger.debug("Creating reminder for task ID: {}", taskId);
        
        User currentUser = userService.getCurrentUser();
        
        // Find task and validate ownership
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Validate reminder request
        validateReminderRequest(reminderRequest);
        
        // Create reminder entity
        Reminder reminder = new Reminder();
        reminder.setTask(task);
        reminder.setReminderOffsetMinutes(reminderRequest.getReminderOffsetMinutes());
        reminder.setNotificationType(reminderRequest.getNotificationType());
        reminder.calculateReminderTime();
        
        // Validate reminder time
        if (reminder.getReminderTime() != null && reminder.getReminderTime().isAfter(task.getStartDatetime())) {
            throw new RuntimeException("Reminder time cannot be after task start time");
        }
        
        // Save reminder
        Reminder savedReminder = reminderRepository.save(reminder);
        
        logger.info("Reminder created for task: {} at {} minutes before", 
                   task.getTitle(), reminderRequest.getReminderOffsetMinutes());
        
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
        LocalDateTime now = LocalDateTime.now();
        
        List<Reminder> reminders = reminderRepository.findUpcomingRemindersForUser(currentUserId, now);
        
        return reminders.stream()
                .map(ReminderResponse::fromReminder)
                .collect(Collectors.toList());
    }
    
    /**
     * Get reminders in date range for current user
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
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
     * Reset reminder status (for testing/retry)
     */
    public void resetReminderStatus(Long reminderId) {
        Long currentUserId = userService.getCurrentUserId();
        
        // Validate ownership
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        
        reminderRepository.resetReminderStatus(reminderId);
        logger.info("Reminder status reset for reminder ID: {}", reminderId);
    }
    
    /**
     * Get due reminders (ready to be sent)
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getDueReminders() {
        LocalDateTime currentTime = LocalDateTime.now();
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
            LocalDateTime currentTime = LocalDateTime.now();
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
            logger.debug("Processing reminder ID: {} for task: {}", 
                        reminder.getId(), reminder.getTask().getTitle());
            
            // Send notification based on type
            if (reminder.getNotificationType() == Reminder.NotificationType.PUSH) {
                notificationService.sendPushNotification(reminder);
            } else if (reminder.getNotificationType() == Reminder.NotificationType.EMAIL) {
                notificationService.sendEmailNotification(reminder);
            }
            
            // Mark as sent
            markReminderAsSent(reminder.getId());
            
            logger.info("Reminder processed successfully for task: {}", 
                       reminder.getTask().getTitle());
            
        } catch (Exception e) {
            logger.error("Failed to process reminder ID {}: {}", 
                        reminder.getId(), e.getMessage());
        }
    }
    
    /**
     * Get reminder statistics for current user
     */
    @Transactional(readOnly = true)
    public ReminderStatistics getReminderStatistics() {
        Long currentUserId = userService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        
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
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Keep for 30 days
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