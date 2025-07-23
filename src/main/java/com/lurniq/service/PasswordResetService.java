package com.lurniq.service;

import com.lurniq.entity.PasswordResetToken;
import com.lurniq.entity.User;
import com.lurniq.repository.PasswordResetTokenRepository;
import com.lurniq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${email.password-reset.expiration-hours:1}")
    private int expirationHours;
    
    @Value("${email.password-reset.max-requests-per-hour:3}")
    private int maxRequestsPerHour;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate and send password reset email for a user
     */
    @Transactional
    public boolean sendPasswordResetEmail(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                log.warn("Password reset requested for non-existent email: {}", email);
                // Return true to prevent email enumeration attacks
                return true;
            }
            
            User user = userOpt.get();
            
            // Check rate limiting
            if (isRateLimited(user)) {
                log.warn("Password reset rate limited for user: {}", email);
                return false;
            }
            
            // Delete any existing tokens for this user
            tokenRepository.deleteAllByUser(user);
            
            // Generate new token
            String token = generateSecureToken();
            
            // Create and save reset token
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(expirationHours))
                    .used(false)
                    .build();
            
            tokenRepository.save(resetToken);
            
            // Send password reset email (async)
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token)
                .thenRun(() -> log.info("Password reset email sent for user: {}", email))
                .exceptionally(ex -> {
                    log.error("Failed to send password reset email for user: {}. Error: {}", email, ex.getMessage());
                    return null;
                });
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send password reset email for user: {}. Error: {}", email, e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify reset token and return the associated user
     */
    public Optional<User> verifyResetToken(String token) {
        try {
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
            
            if (tokenOpt.isEmpty()) {
                log.warn("Invalid password reset token: {}", token);
                return Optional.empty();
            }
            
            PasswordResetToken resetToken = tokenOpt.get();
            
            // Check if token is valid
            if (!resetToken.isValid()) {
                log.warn("Expired or used password reset token: {}", token);
                return Optional.empty();
            }
            
            return Optional.of(resetToken.getUser());
            
        } catch (Exception e) {
            log.error("Failed to verify password reset token: {}. Error: {}", token, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Reset password using token
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        try {
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
            
            if (tokenOpt.isEmpty()) {
                log.warn("Invalid password reset token: {}", token);
                return false;
            }
            
            PasswordResetToken resetToken = tokenOpt.get();
            
            // Check if token is valid
            if (!resetToken.isValid()) {
                log.warn("Expired or used password reset token: {}", token);
                return false;
            }
            
            // Update user password
            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            // Mark token as used
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
            
            // Send confirmation email (optional)
            try {
                sendPasswordChangeConfirmationEmail(user);
            } catch (Exception e) {
                log.warn("Failed to send password change confirmation email for user: {}. Error: {}", 
                    user.getEmail(), e.getMessage());
                // Don't fail the password reset if confirmation email fails
            }
            
            log.info("Password reset successfully for user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to reset password with token: {}. Error: {}", token, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if user is rate limited for password reset requests
     */
    private boolean isRateLimited(User user) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentRequests = tokenRepository.countRecentTokensByUser(user, oneHourAgo);
        return recentRequests >= maxRequestsPerHour;
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
     * Send password change confirmation email
     */
    private void sendPasswordChangeConfirmationEmail(User user) {
        try {
            // This would use a separate template for password change confirmation
            log.info("Password change confirmation email would be sent to: {}", user.getEmail());
            // TODO: Implement password change confirmation email template and logic
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email: {}", e.getMessage());
        }
    }
    
    /**
     * Check if user has a valid (unused and not expired) reset token
     */
    public boolean hasValidResetToken(User user) {
        return tokenRepository.existsByUserAndUsedFalse(user) &&
               tokenRepository.findValidTokenByUser(user, LocalDateTime.now()).isPresent();
    }
    
    /**
     * Clean up expired tokens (runs every hour)
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(expirationHours + 1);
            int deletedCount = tokenRepository.deleteExpiredTokens(cutoffTime);
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired password reset tokens", deletedCount);
            }
        } catch (Exception e) {
            log.error("Failed to clean up expired password reset tokens: {}", e.getMessage());
        }
    }
    
    /**
     * Validate password strength
     */
    public boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
