package com.lurniq.repository;

import com.lurniq.entity.EmailVerificationToken;
import com.lurniq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    @Query("SELECT t FROM EmailVerificationToken t JOIN FETCH t.user WHERE t.token = :token")
    Optional<EmailVerificationToken> findByToken(@Param("token") String token);
    
    @Query("SELECT t FROM EmailVerificationToken t JOIN FETCH t.user WHERE t.user = :user AND t.used = false")
    Optional<EmailVerificationToken> findByUserAndUsedFalse(@Param("user") User user);
    
    @Query("SELECT t FROM EmailVerificationToken t JOIN FETCH t.user WHERE t.user = :user AND t.used = false AND t.expiryDate > :now")
    Optional<EmailVerificationToken> findValidTokenByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiryDate < :expiredBefore")
    int deleteExpiredTokens(@Param("expiredBefore") LocalDateTime expiredBefore);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    boolean existsByUserAndUsedFalse(User user);
}
