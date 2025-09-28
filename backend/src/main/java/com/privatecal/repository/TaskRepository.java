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
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * Find all tasks for a specific user
     */
    List<Task> findByUserOrderByStartDatetimeAsc(User user);
    
    /**
     * Find all tasks for a specific user by user ID
     */
    List<Task> findByUser_IdOrderByStartDatetimeAsc(Long userId);
    
    /**
     * Find task by ID and user (for security - user can only access their own tasks)
     */
    Optional<Task> findByIdAndUser(Long id, User user);
    
    /**
     * Find task by ID and user ID
     */
    Optional<Task> findByIdAndUser_Id(Long id, Long userId);
    
    /**
     * Find tasks within a date range for a user
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user AND " +
           "((t.startDatetime >= :startDate AND t.startDatetime < :endDate) OR " +
           "(t.endDatetime > :startDate AND t.endDatetime <= :endDate) OR " +
           "(t.startDatetime <= :startDate AND t.endDatetime >= :endDate))")
    List<Task> findTasksInDateRangeForUser(@Param("user") User user, 
                                         @Param("startDate") Instant startDate, 
                                         @Param("endDate") Instant endDate);
    
    /**
     * Find tasks within a date range for a user by user ID
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "((t.startDatetime >= :startDate AND t.startDatetime < :endDate) OR " +
           "(t.endDatetime > :startDate AND t.endDatetime <= :endDate) OR " +
           "(t.startDatetime <= :startDate AND t.endDatetime >= :endDate))")
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
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.id != :excludeTaskId AND " +
           "((t.startDatetime < :endTime AND t.endDatetime > :startTime))")
    long countConflictingTasksExcludingTask(@Param("userId") Long userId,
                                          @Param("excludeTaskId") Long excludeTaskId,
                                          @Param("startTime") Instant startTime, 
                                          @Param("endTime") Instant endTime);
}