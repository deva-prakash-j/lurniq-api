package com.lurniq.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized rate limiting configuration using Caffeine cache for better memory management
 */
@Configuration
public class OptimizedRateLimitingConfig {

    @Bean
    public OptimizedRateLimitingFilter optimizedRateLimitingFilter() {
        return new OptimizedRateLimitingFilter();
    }

    public static class OptimizedRateLimitingFilter extends OncePerRequestFilter {
        
        // Use Caffeine cache for automatic cleanup and better performance
        private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
                .maximumSize(10000)  // Limit memory usage
                .expireAfterWrite(Duration.ofMinutes(1))  // Auto cleanup after 1 minute
                .build();
        
        private static final int MAX_REQUESTS_PER_MINUTE = 10;  // Slightly less restrictive
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                FilterChain filterChain) throws ServletException, IOException {
            
            String requestURI = request.getRequestURI();
            
            // Only apply rate limiting to authentication endpoints
            if (isAuthenticationEndpoint(requestURI)) {
                String clientIp = getClientIpAddress(request);
                String key = clientIp + ":" + requestURI;
                
                // Get or create counter with atomic operation
                AtomicInteger counter = requestCounts.get(key, k -> new AtomicInteger(0));
                int currentCount = counter.incrementAndGet();
                
                if (currentCount > MAX_REQUESTS_PER_MINUTE) {
                    response.setStatus(429); // Too Many Requests
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many authentication attempts. Please try again later.\",\"retryAfter\":60}"
                    );
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        }
        
        private boolean isAuthenticationEndpoint(String uri) {
            return uri.startsWith("/api/auth/login") || 
                   uri.startsWith("/api/auth/register") || 
                   uri.startsWith("/auth/password-reset") ||
                   uri.startsWith("/oauth2/authorization/");
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
    }
}
