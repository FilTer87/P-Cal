package com.privatecal.repository;

import com.privatecal.entity.Calendar;
import com.privatecal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    /**
     * Find all calendars for a specific user
     */
    List<Calendar> findByUserOrderByIsDefaultDescCreatedAtAsc(User user);

    /**
     * Find all calendars for a specific user by user ID
     */
    List<Calendar> findByUser_IdOrderByIsDefaultDescCreatedAtAsc(Long userId);

    /**
     * Find visible calendars for a user
     */
    List<Calendar> findByUserAndIsVisibleTrueOrderByIsDefaultDescCreatedAtAsc(User user);

    /**
     * Find calendar by ID and user (for security - user can only access their own calendars)
     */
    Optional<Calendar> findByIdAndUser(Long id, User user);

    /**
     * Find calendar by ID and user ID
     */
    Optional<Calendar> findByIdAndUser_Id(Long id, Long userId);

    /**
     * Find calendar by slug and user
     */
    Optional<Calendar> findBySlugAndUser(String slug, User user);

    /**
     * Find calendar by slug and username/email (for CalDAV path resolution)
     * Accepts both username or email for flexibility
     */
    @Query("SELECT c FROM Calendar c WHERE c.slug = :slug AND (c.user.username = :usernameOrEmail OR c.user.email = :usernameOrEmail)")
    Optional<Calendar> findBySlugAndUsername(@Param("slug") String slug, @Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Find default calendar for a user
     */
    Optional<Calendar> findByUserAndIsDefaultTrue(User user);

    /**
     * Find default calendar for a user by user ID
     */
    Optional<Calendar> findByUser_IdAndIsDefaultTrue(Long userId);

    /**
     * Check if calendar slug already exists for user
     */
    boolean existsByUserAndSlug(User user, String slug);

    /**
     * Check if calendar slug exists for user by user ID
     */
    boolean existsByUser_IdAndSlug(Long userId, String slug);

    /**
     * Count calendars for a user
     */
    long countByUser(User user);

    /**
     * Count calendars for a user by user ID
     */
    long countByUser_Id(Long userId);

    /**
     * Delete all calendars for a user
     */
    void deleteByUser(User user);

    /**
     * Find calendar by user and name (case insensitive)
     */
    @Query("SELECT c FROM Calendar c WHERE c.user = :user AND LOWER(c.name) = LOWER(:name)")
    Optional<Calendar> findByUserAndNameIgnoreCase(@Param("user") User user, @Param("name") String name);
}
