package com.privatecal.repository;

import com.privatecal.entity.TelegramRegistrationToken;
import com.privatecal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for TelegramRegistrationToken entity
 */
@Repository
public interface TelegramRegistrationTokenRepository extends JpaRepository<TelegramRegistrationToken, Long> {

    /**
     * Find a valid (unused and not expired) token by token string
     */
    @Query("SELECT t FROM TelegramRegistrationToken t WHERE t.token = ?1 AND t.used = false AND t.expiresAt > ?2")
    Optional<TelegramRegistrationToken> findValidToken(String token, Instant now);

    /**
     * Find token by user
     */
    Optional<TelegramRegistrationToken> findByUser(User user);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM TelegramRegistrationToken t WHERE t.expiresAt < ?1")
    void deleteExpiredTokens(Instant now);

    /**
     * Delete used tokens
     */
    @Modifying
    @Query("DELETE FROM TelegramRegistrationToken t WHERE t.used = true")
    void deleteUsedTokens();
}
