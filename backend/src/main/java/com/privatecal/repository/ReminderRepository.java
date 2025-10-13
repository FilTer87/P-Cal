package com.privatecal.repository;

import com.privatecal.dto.NotificationType;
import com.privatecal.entity.Reminder;
import com.privatecal.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    /**
     * Find all reminders for a specific task
     */
    List<Reminder> findByTaskOrderByReminderTimeAsc(Task task);
    
    /**
     * Find all reminders for a specific task by task ID
     */
    List<Reminder> findByTask_IdOrderByReminderTimeAsc(Long taskId);
    
    /**
     * Find reminder by ID and task (for security)
     */
    Optional<Reminder> findByIdAndTask(Long id, Task task);
    
    /**
     * Find reminder by ID and task ID
     */
    Optional<Reminder> findByIdAndTask_Id(Long id, Long taskId);
    
    /**
     * Find reminder by ID and ensure it belongs to the specified user
     */
    @Query("SELECT r FROM Reminder r WHERE r.id = :reminderId AND r.task.user.id = :userId")
    Optional<Reminder> findByIdAndUserId(@Param("reminderId") Long reminderId, @Param("userId") Long userId);
    
    /**
     * Find all reminders that are due (time has passed and not yet sent)
     * Excludes reminders for tasks that have already ended (no notifications for past events)
     */
    @Query("SELECT r FROM Reminder r WHERE r.reminderTime <= :currentTime AND r.isSent = false AND r.task.endDatetime > :currentTime ORDER BY r.reminderTime ASC")
    List<Reminder> findDueReminders(@Param("currentTime") Instant currentTime);
    
    /**
     * Find all unsent reminders for a specific user
     */
    @Query("SELECT r FROM Reminder r WHERE r.task.user.id = :userId AND r.isSent = false ORDER BY r.reminderTime ASC")
    List<Reminder> findUnsentRemindersForUser(@Param("userId") Long userId);
    
    /**
     * Find all reminders for a specific user (sent and unsent)
     */
    @Query("SELECT r FROM Reminder r WHERE r.task.user.id = :userId ORDER BY r.reminderTime DESC")
    List<Reminder> findAllByUserId(@Param("userId") Long userId);
    
    /**
     * Find all reminders for a user within a date range
     */
    @Query("SELECT r FROM Reminder r WHERE r.task.user.id = :userId AND " +
           "r.reminderTime >= :startDate AND r.reminderTime <= :endDate " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findRemindersInDateRangeForUser(@Param("userId") Long userId,
                                                 @Param("startDate") Instant startDate,
                                                 @Param("endDate") Instant endDate);
    
    /**
     * Find upcoming reminders for a user (from now onwards)
     */
    @Query("SELECT r FROM Reminder r WHERE r.task.user.id = :userId AND " +
           "r.reminderTime >= :now AND r.isSent = false ORDER BY r.reminderTime ASC")
    List<Reminder> findUpcomingRemindersForUser(@Param("userId") Long userId, @Param("now") Instant now);
    
    /**
     * Find reminders that need to be processed in the next X minutes
     */
    @Query("SELECT r FROM Reminder r WHERE r.reminderTime >= :now AND r.reminderTime <= :futureTime " +
           "AND r.isSent = false ORDER BY r.reminderTime ASC")
    List<Reminder> findRemindersInNextMinutes(@Param("now") Instant now, @Param("futureTime") Instant futureTime);
    
    /**
     * Mark reminder as sent
     */
    @Modifying
    @Transactional
    @Query("UPDATE Reminder r SET r.isSent = true WHERE r.id = :reminderId")
    void markReminderAsSent(@Param("reminderId") Long reminderId);
    
    /**
     * Mark multiple reminders as sent
     */
    @Modifying
    @Transactional
    @Query("UPDATE Reminder r SET r.isSent = true WHERE r.id IN :reminderIds")
    void markRemindersAsSent(@Param("reminderIds") List<Long> reminderIds);
    
    /**
     * Reset reminder status (mark as not sent) - useful for testing or manual retry
     */
    @Modifying
    @Transactional
    @Query("UPDATE Reminder r SET r.isSent = false WHERE r.id = :reminderId")
    void resetReminderStatus(@Param("reminderId") Long reminderId);
    
    /**
     * Delete all reminders for a specific task
     */
    void deleteByTask(Task task);
    
    /**
     * Delete all reminders for a task by task ID
     */
    void deleteByTask_Id(Long taskId);
    
    /**
     * Delete all reminders for tasks belonging to a user
     */
    @Query("DELETE FROM Reminder r WHERE r.task.user.id = :userId")
    @Modifying
    @Transactional
    void deleteAllRemindersForUser(@Param("userId") Long userId);
    
    /**
     * Count reminders for a specific task
     */
    long countByTask(Task task);
    
    /**
     * Count reminders for a task by task ID
     */
    long countByTask_Id(Long taskId);
    
    /**
     * Count unsent reminders for a user
     */
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.task.user.id = :userId AND r.isSent = false")
    long countUnsentRemindersForUser(@Param("userId") Long userId);
    
    /**
     * Count overdue reminders (should have been sent but weren't)
     * Excludes reminders for tasks that have already ended
     */
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.reminderTime < :currentTime AND r.isSent = false AND r.task.endDatetime > :currentTime")
    long countOverdueReminders(@Param("currentTime") Instant currentTime);
    
    /**
     * Find reminders by notification type for a user
     */
    @Query("SELECT r FROM Reminder r WHERE r.task.user.id = :userId AND r.notificationType = :notificationType " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findRemindersByNotificationTypeForUser(@Param("userId") Long userId, 
                                                         @Param("notificationType") NotificationType notificationType);
    
    /**
     * Find the next reminder for a specific task
     */
    @Query("SELECT r FROM Reminder r WHERE r.task.id = :taskId AND r.reminderTime >= :now AND r.isSent = false " +
           "ORDER BY r.reminderTime ASC")
    Optional<Reminder> findNextReminderForTask(@Param("taskId") Long taskId, @Param("now") Instant now);
    
    /**
     * Delete sent reminders older than specified date (cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Reminder r WHERE r.isSent = true AND r.reminderTime < :cutoffDate")
    void deleteOldSentReminders(@Param("cutoffDate") Instant cutoffDate);
}