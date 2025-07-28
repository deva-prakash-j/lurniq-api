package com.lurniq.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Input validation and security filter to prevent malicious payloads
 * and ensure secure data handling
 */
@Configuration
@Slf4j
public class InputValidationConfig {

    @Bean
    @Order(1) // Execute before other security filters
    public InputValidationFilter inputValidationFilter() {
        return new InputValidationFilter();
    }

    public static class InputValidationFilter extends OncePerRequestFilter {
        
        // Common XSS patterns
        private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(.*(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=).*)|" +
            "(.*(<object|</object|<embed|</embed|<applet|</applet).*)", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        // SQL injection patterns - more precise to avoid false positives
        private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(.*('|(\\-\\-)|(%27)|(%2D%2D)).*)|" +  // Quote and comment patterns
            "(.*\\b(ALTER\\s+TABLE|CREATE\\s+TABLE|DELETE\\s+FROM|DROP\\s+TABLE|EXEC(UTE)?\\s*\\(|INSERT\\s+INTO|SELECT\\s+.*\\s+FROM|UPDATE\\s+.*\\s+SET|UNION\\s+(ALL\\s+)?SELECT)\\b.*)", // SQL keywords with context
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        // Path traversal patterns
        private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e\\\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            
            // Skip validation for static resources and actuator endpoints
            String requestURI = request.getRequestURI();
            if (shouldSkipValidation(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Validate request parameters
            if (containsMaliciousContent(request)) {
                log.warn("SECURITY_ALERT: Malicious content detected in request from IP: {} to URI: {}", 
                    getClientIpAddress(request), requestURI);
                
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Invalid request\",\"message\":\"Request contains invalid characters\"}"
                );
                return;
            }
            
            filterChain.doFilter(request, response);
        }
        
        private boolean shouldSkipValidation(String uri) {
            return uri.startsWith("/actuator/") || 
                   uri.startsWith("/swagger-ui/") || 
                   uri.startsWith("/api-docs/") ||
                   uri.startsWith("/webjars/") ||
                   uri.equals("/health") ||
                   uri.equals("/ready") ||
                   uri.endsWith(".css") ||
                   uri.endsWith(".js") ||
                   uri.endsWith(".png") ||
                   uri.endsWith(".ico");
        }
        
        private boolean containsMaliciousContent(HttpServletRequest request) {
            // Check query parameters
            if (request.getQueryString() != null) {
                String queryString = request.getQueryString().toLowerCase();
                if (isMalicious(queryString)) {
                    return true;
                }
            }
            
            // Check request parameters
            for (String paramName : request.getParameterMap().keySet()) {
                String[] paramValues = request.getParameterValues(paramName);
                for (String paramValue : paramValues) {
                    if (paramValue != null && isMalicious(paramValue.toLowerCase())) {
                        return true;
                    }
                }
            }
            
            // Check headers for XSS attempts
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && (XSS_PATTERN.matcher(userAgent).matches() || 
                                     SQL_INJECTION_PATTERN.matcher(userAgent).matches())) {
                return true;
            }
            
            return false;
        }
        
        private boolean isMalicious(String input) {
            boolean isXSS = XSS_PATTERN.matcher(input).matches();
            boolean isSQLInjection = SQL_INJECTION_PATTERN.matcher(input).matches();
            boolean isPathTraversal = PATH_TRAVERSAL_PATTERN.matcher(input).find();
            
            // Debug logging to identify which pattern is triggering
            if (isXSS || isSQLInjection || isPathTraversal) {
                log.debug("Malicious content detected - Input: '{}', XSS: {}, SQL: {}, PathTraversal: {}", 
                         input, isXSS, isSQLInjection, isPathTraversal);
            }
            
            return isXSS || isSQLInjection || isPathTraversal;
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
