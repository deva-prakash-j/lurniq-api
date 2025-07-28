package com.lurniq.repository;

import com.lurniq.entity.PasswordResetToken;
import com.lurniq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.token = :token")
    Optional<PasswordResetToken> findByToken(@Param("token") String token);
    
    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.user = :user AND t.used = false")
    Optional<PasswordResetToken> findByUserAndUsedFalse(@Param("user") User user);
    
    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.user = :user AND t.used = false AND t.expiryDate > :now")
    Optional<PasswordResetToken> findValidTokenByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :expiredBefore")
    int deleteExpiredTokens(@Param("expiredBefore") LocalDateTime expiredBefore);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    boolean existsByUserAndUsedFalse(User user);
    
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user = :user AND t.createdAt > :since")
    long countRecentTokensByUser(@Param("user") User user, @Param("since") LocalDateTime since);
}
