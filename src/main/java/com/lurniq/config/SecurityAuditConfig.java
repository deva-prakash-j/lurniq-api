package com.lurniq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Security audit logging configuration to track authentication events
 * and potential security threats
 */
@Configuration
@Slf4j
public class SecurityAuditConfig {

    /**
     * Listen for successful authentication events
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth.getName();
        String clientIp = getClientIpFromAuth(auth);
        
        // Hash username for privacy while maintaining audit capability
        String hashedUser = hashForAudit(username);
        String maskedIp = maskIpAddress(clientIp);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Log with hashed/masked data for privacy compliance
        log.info("SECURITY_AUDIT: Successful authentication - UserHash: {}, MaskedIP: {}, Timestamp: {}", 
                hashedUser, maskedIp, timestamp);
    }

    /**
     * Listen for failed authentication events
     */
    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth.getName();
        String clientIp = getClientIpFromAuth(auth);
        
        // Hash username for privacy while maintaining audit capability
        String hashedUser = hashForAudit(username);
        String maskedIp = maskIpAddress(clientIp);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Log failed attempts with more detail for security monitoring
        log.warn("SECURITY_AUDIT: Failed authentication attempt - UserHash: {}, MaskedIP: {}, Reason: Bad credentials, Timestamp: {}", 
                hashedUser, maskedIp, timestamp);
    }

    private String getClientIpFromAuth(Authentication auth) {
        String clientIp = "unknown";
        
        if (auth.getDetails() instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails details = (WebAuthenticationDetails) auth.getDetails();
            clientIp = details.getRemoteAddress();
            
            // If we have the details, let's also check if it contains forwarded headers info
            // Note: WebAuthenticationDetails.getRemoteAddress() typically gives the direct connection IP
            // For proxy scenarios, we'd ideally need the HttpServletRequest, but we work with what we have
        }
        
        return clientIp != null ? clientIp : "unknown";
    }
    
    /**
     * Hash username for audit logging while preserving uniqueness
     * This allows correlation in logs without exposing actual usernames
     */
    private String hashForAudit(String input) {
        if (input == null || input.isEmpty()) {
            return "empty";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 8 characters for readability
            return hexString.toString().substring(0, 8);
        } catch (Exception e) {
            return "hash_error";
        }
    }
    
    /**
     * Mask IP address for privacy compliance
     * Keep first 3 octets for geographical info, mask the last
     */
    private String maskIpAddress(String ip) {
        if (ip == null || ip.equals("unknown")) {
            return "unknown";
        }
        
        // Handle localhost addresses specially for development
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return "localhost";
        }
        
        // For IPv4
        if (ip.contains(".") && !ip.contains(":")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
            }
        }
        
        // For IPv6 addresses
        if (ip.contains(":")) {
            // Handle compressed IPv6 addresses
            if (ip.equals("::1")) {
                return "localhost";
            }
            // For other IPv6 addresses, show first 4 groups and mask the rest
            String[] parts = ip.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":****";
            }
        }
        
        // For other formats, mask more aggressively
        if (ip.length() > 8) {
            return ip.substring(0, 8) + "****";
        }
        
        return "masked";
    }
}
