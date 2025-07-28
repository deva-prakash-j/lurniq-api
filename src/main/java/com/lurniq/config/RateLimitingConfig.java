package com.lurniq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting configuration to prevent brute force attacks on authentication endpoints
 */
@Configuration
public class RateLimitingConfig {

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter();
    }

    public static class RateLimitingFilter extends OncePerRequestFilter {
        
        private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // Rate limiting settings
        private static final int MAX_REQUESTS_PER_MINUTE = 5;  // Very restrictive for auth endpoints
        private static final long TIME_WINDOW_MS = 60000; // 1 minute
        private static final long CLEANUP_INTERVAL_MS = 300000; // 5 minutes
        
        public RateLimitingFilter() {
            // Schedule cleanup of old entries
            scheduler.scheduleAtFixedRate(this::cleanupOldEntries, 
                CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                FilterChain filterChain) throws ServletException, IOException {
            
            String requestURI = request.getRequestURI();
            
            // Only apply rate limiting to authentication endpoints
            if (isAuthenticationEndpoint(requestURI)) {
                String clientIp = getClientIpAddress(request);
                String key = clientIp + ":" + requestURI;
                
                if (isRateLimited(key)) {
                    response.setStatus(429); // Too Many Requests
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many authentication attempts. Please try again later.\"}"
                    );
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        }
        
        private boolean isAuthenticationEndpoint(String uri) {
            return uri.startsWith("/api/auth/login") || 
                   uri.startsWith("/api/auth/register") || 
                   uri.startsWith("/api/password-reset/") ||
                   uri.startsWith("/oauth2/authorization/");
        }
        
        private boolean isRateLimited(String key) {
            long currentTime = System.currentTimeMillis();
            
            // Clean up if this key is too old
            Long lastAccess = lastAccessTime.get(key);
            if (lastAccess != null && (currentTime - lastAccess) > TIME_WINDOW_MS) {
                requestCounts.remove(key);
                lastAccessTime.remove(key);
            }
            
            // Update access time
            lastAccessTime.put(key, currentTime);
            
            // Increment and check count
            AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
            int currentCount = count.incrementAndGet();
            
            return currentCount > MAX_REQUESTS_PER_MINUTE;
        }
        
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
        
        private void cleanupOldEntries() {
            long currentTime = System.currentTimeMillis();
            lastAccessTime.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > TIME_WINDOW_MS * 2
            );
            
            // Remove corresponding request counts
            requestCounts.keySet().retainAll(lastAccessTime.keySet());
        }
    }
}
