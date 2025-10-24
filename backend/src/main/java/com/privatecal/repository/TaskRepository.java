package com.privatecal.repository;

import com.privatecal.entity.Task;
import com.privatecal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {  // Changed from Long to String (UID)

    /**
     * Find all tasks for a specific user
     */
    List<Task> findByUserOrderByStartDatetimeAsc(User user);

    /**
     * Find all tasks for a specific user by user ID
     */
    List<Task> findByUser_IdOrderByStartDatetimeAsc(Long userId);

    /**
     * Find task by UID and user (for security - user can only access their own tasks)
     * UID is now the primary key
     */
    Optional<Task> findByUidAndUser(String uid, User user);

    /**
     * Find task by UID and user ID
     * UID is now the primary key
     */
    Optional<Task> findByUidAndUser_Id(String uid, Long userId);

    /**
     * Backward compatibility: alias for findByUidAndUser
     * @deprecated Use findByUidAndUser instead
     */
    @Deprecated
    default Optional<Task> findByIdAndUser(String id, User user) {
        return findByUidAndUser(id, user);
    }

    /**
     * Backward compatibility: alias for findByUidAndUser_Id
     * @deprecated Use findByUidAndUser_Id instead
     */
    @Deprecated
    default Optional<Task> findByIdAndUser_Id(String id, Long userId) {
        return findByUidAndUser_Id(id, userId);
    }
    
    /**
     * Find tasks within a date range for a user
     * Includes recurring tasks regardless of their original date (they will be expanded later)
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user AND " +
           "(t.recurrenceRule IS NOT NULL OR " +
           "((t.startDatetime >= :startDate AND t.startDatetime < :endDate) OR " +
           "(t.endDatetime > :startDate AND t.endDatetime <= :endDate) OR " +
           "(t.startDatetime <= :startDate AND t.endDatetime >= :endDate)))")
    List<Task> findTasksInDateRangeForUser(@Param("user") User user,
                                         @Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);
    
    /**
     * Find tasks within a date range for a user by user ID
     * Includes recurring tasks regardless of their original date (they will be expanded later)
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "(t.recurrenceRule IS NOT NULL OR " +
           "((t.startDatetime >= :startDate AND t.startDatetime < :endDate) OR " +
           "(t.endDatetime > :startDate AND t.endDatetime <= :endDate) OR " +
           "(t.startDatetime <= :startDate AND t.endDatetime >= :endDate)))")
    List<Task> findTasksInDateRangeForUserId(@Param("userId") Long userId,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate);
    
    /**
     * Find upcoming tasks for a user (starting from now)
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user AND t.startDatetime >= :now ORDER BY t.startDatetime ASC")
    List<Task> findUpcomingTasksForUser(@Param("user") User user, @Param("now") Instant now);
    
    /**
     * Find overdue tasks for a user (ended before now)
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user AND t.endDatetime < :now ORDER BY t.endDatetime DESC")
    List<Task> findOverdueTasksForUser(@Param("user") User user, @Param("now") Instant now);
    
    /**
     * Find tasks starting within next X minutes for a user (useful for reminders)
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "t.startDatetime >= :now AND t.startDatetime <= :futureTime " +
           "ORDER BY t.startDatetime ASC")
    List<Task> findTasksStartingWithinMinutes(@Param("userId") Long userId,
                                            @Param("now") Instant now,
                                            @Param("futureTime") Instant futureTime);
    
    /**
     * Find tasks by title containing search term for a user
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Task> findTasksBySearchTermForUser(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    /**
     * Count tasks for a user
     */
    long countByUser(User user);
    
    /**
     * Count tasks for a user by user ID
     */
    long countByUser_Id(Long userId);
    
    /**
     * Delete all tasks for a user
     */
    void deleteByUser(User user);
    
    /**
     * Find all tasks that have reminders due (for notification processing)
     */
    @Query("SELECT DISTINCT t FROM Task t JOIN t.reminders r WHERE " +
           "r.reminderTime <= :currentTime AND r.isSent = false")
    List<Task> findTasksWithDueReminders(@Param("currentTime") Instant currentTime);
    
    /**
     * Find today's tasks for a user
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "t.startDatetime >= :startOfDay AND t.startDatetime < :endOfDay " +
           "ORDER BY t.startDatetime ASC")
    List<Task> findTodayTasksForUser(@Param("userId") Long userId,
                                     @Param("startOfDay") Instant startOfDay,
                                     @Param("endOfDay") Instant endOfDay);
    
    /**
     * Check if user has any tasks in a specific time slot (for conflict detection)
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND " +
           "((t.startDatetime < :endTime AND t.endDatetime > :startTime))")
    long countConflictingTasks(@Param("userId") Long userId, 
                              @Param("startTime") Instant startTime, 
                              @Param("endTime") Instant endTime);
    
    /**
     * Check if user has any tasks in a specific time slot excluding a specific task
     * Updated to use UID instead of numeric ID
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.uid != :excludeTaskUid AND " +
           "((t.startDatetime < :endTime AND t.endDatetime > :startTime))")
    long countConflictingTasksExcludingTask(@Param("userId") Long userId,
                                          @Param("excludeTaskUid") String excludeTaskUid,
                                          @Param("startTime") Instant startTime,
                                          @Param("endTime") Instant endTime);

    /**
     * Find task by UID for a specific user
     */
    Optional<Task> findByUserAndUid(User user, String uid);

    /**
     * Find all tasks with UIDs in the provided list for a specific user (batch check)
     */
    List<Task> findByUserAndUidIn(User user, List<String> uids);

    /**
     * Count tasks for a specific user and calendar
     */
    long countByUser_IdAndCalendar_Id(Long userId, Long calendarId);

    /**
     * Find all tasks for a specific calendar
     */
    List<Task> findByCalendar_IdOrderByStartDatetimeAsc(Long calendarId);
}