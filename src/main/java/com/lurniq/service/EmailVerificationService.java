package com.lurniq.service;

import com.lurniq.entity.EmailVerificationToken;
import com.lurniq.entity.User;
import com.lurniq.repository.EmailVerificationTokenRepository;
import com.lurniq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Value("${email.activation.expiration-hours:24}")
    private int expirationHours;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate and send activation email for a user
     */
    @Transactional
    public void sendActivationEmail(User user) {
        try {
            // Delete any existing tokens for this user
            tokenRepository.deleteAllByUser(user);
            
            // Generate new token
            String token = generateSecureToken();
            
            // Create and save verification token
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(expirationHours))
                    .used(false)
                    .build();
            
            tokenRepository.save(verificationToken);
            
            // Send activation email (async)
            emailService.sendActivationEmail(user.getEmail(), user.getFirstName(), token)
                .thenRun(() -> log.info("Activation email sent for user: {}", user.getEmail()))
                .exceptionally(ex -> {
                    log.error("Failed to send activation email for user: {}. Error: {}", user.getEmail(), ex.getMessage());
                    return null;
                });
            
        } catch (Exception e) {
            log.error("Failed to send activation email for user: {}. Error: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send activation email", e);
        }
    }
    
    /**
     * Verify activation token and activate user account
     */
    @Transactional
    public boolean verifyAndActivateAccount(String token) {
        try {
            Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
            
            if (tokenOpt.isEmpty()) {
                log.warn("Invalid activation token: {}", token);
                return false;
            }
            
            EmailVerificationToken verificationToken = tokenOpt.get();
            
            // Check if token is valid
            if (!verificationToken.isValid()) {
                log.warn("Expired or used activation token: {}", token);
                return false;
            }
            
            // Activate user account
            User user = verificationToken.getUser();
            user.setEmailVerified(true);
            userRepository.save(user);
            
            // Mark token as used
            verificationToken.setUsed(true);
            tokenRepository.save(verificationToken);
            
            // Send welcome email (async)
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName())
                .thenRun(() -> log.info("Welcome email queued for user: {}", user.getEmail()))
                .exceptionally(ex -> {
                    log.warn("Failed to send welcome email for user: {}. Error: {}", user.getEmail(), ex.getMessage());
                    return null;
                });
            
            log.info("Account activated successfully for user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to verify activation token: {}. Error: {}", token, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if user has a valid (unused and not expired) activation token
     */
    public boolean hasValidActivationToken(User user) {
        return tokenRepository.existsByUserAndUsedFalse(user) &&
               tokenRepository.findValidTokenByUser(user, LocalDateTime.now()).isPresent();
    }
    
    /**
     * Resend activation email if user doesn't have a valid token
     */
    @Transactional
    public boolean resendActivationEmail(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                log.warn("User not found for email: {}", email);
                return false;
            }
            
            User user = userOpt.get();
            
            if (user.getEmailVerified()) {
                log.info("User already verified: {}", email);
                return false;
            }
            
            // Check if user already has a valid token
            if (hasValidActivationToken(user)) {
                log.info("User already has a valid activation token: {}", email);
                return false;
            }
            
            // Send new activation email
            sendActivationEmail(user);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to resend activation email for: {}. Error: {}", email, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate a secure random token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Clean up expired tokens (runs daily at midnight)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1);
            int deletedCount = tokenRepository.deleteExpiredTokens(cutoffTime);
            log.info("Cleaned up {} expired email verification tokens", deletedCount);
        } catch (Exception e) {
            log.error("Failed to clean up expired tokens: {}", e.getMessage());
        }
    }
}
