package com.privatecal.repository;

import com.privatecal.entity.PasswordResetToken;
import com.privatecal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PasswordResetToken entities
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a valid (not used and not expired) token by token string
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.used = false AND t.expiryDate > :now")
    Optional<PasswordResetToken> findValidTokenByToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Find token by token string regardless of status
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find all tokens for a specific user
     */
    List<PasswordResetToken> findByUser(User user);

    /**
     * Find all valid tokens for a specific user
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user = :user AND t.used = false AND t.expiryDate > :now")
    List<PasswordResetToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Mark all existing tokens for a user as used (when creating a new one)
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void markAllUserTokensAsUsed(@Param("user") User user);

    /**
     * Delete expired tokens (cleanup job)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete used tokens older than specified date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.used = true AND t.createdAt < :before")
    void deleteUsedTokensOlderThan(@Param("before") LocalDateTime before);
}