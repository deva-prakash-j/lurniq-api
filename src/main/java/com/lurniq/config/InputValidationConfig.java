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
        
        // Common XSS patterns - more specific to avoid false positives
        private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<script[^>]*>.*?</script>|javascript\\s*:|vbscript\\s*:|data\\s*:.*?base64|on\\w+\\s*=)", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        // SQL injection patterns - more precise to avoid false positives  
        private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)('\\s*(OR|AND)\\s*'|'\\s*;|--\\s|/\\*.*?\\*/|\\bUNION\\s+SELECT\\b|\\bSELECT\\s+.*\\bFROM\\b|\\bINSERT\\s+INTO\\b|\\bUPDATE\\s+.*\\bSET\\b|\\bDELETE\\s+FROM\\b|\\bDROP\\s+TABLE\\b)", 
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
            
            // Skip validation for static resources and health endpoints
            String requestURI = request.getRequestURI();
            if (shouldSkipValidation(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Only validate POST/PUT/PATCH requests with body content
            String method = request.getMethod();
            if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Validate request parameters
            if (containsMaliciousContent(request)) {
                log.warn("SECURITY_ALERT: Malicious content detected in request from IP: {} to URI: {} - Method: {}", 
                    getClientIpAddress(request), requestURI, method);
                
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
                   uri.startsWith("/v3/api-docs") ||
                   uri.startsWith("/webjars/") ||
                   uri.equals("/health") ||
                   uri.equals("/ready") ||
                   uri.equals("/") ||
                   uri.equals("/login") ||
                   uri.startsWith("/oauth2/") ||
                   uri.startsWith("/auth/success") ||
                   uri.endsWith(".css") ||
                   uri.endsWith(".js") ||
                   uri.endsWith(".png") ||
                   uri.endsWith(".jpg") ||
                   uri.endsWith(".jpeg") ||
                   uri.endsWith(".gif") ||
                   uri.endsWith(".ico") ||
                   uri.endsWith(".svg") ||
                   uri.endsWith(".woff") ||
                   uri.endsWith(".woff2") ||
                   uri.endsWith(".ttf") ||
                   uri.endsWith(".eot");
        }
        
        private boolean containsMaliciousContent(HttpServletRequest request) {
            // Check query parameters
            if (request.getQueryString() != null) {
                String queryString = request.getQueryString();
                if (isMalicious(queryString)) {
                    return true;
                }
            }
            
            // Check request parameters
            for (String paramName : request.getParameterMap().keySet()) {
                String[] paramValues = request.getParameterValues(paramName);
                for (String paramValue : paramValues) {
                    if (paramValue != null && isMalicious(paramValue)) {
                        return true;
                    }
                }
            }
            
            // Only check specific headers that are commonly used for attacks
            // Skip User-Agent as it often contains legitimate complex strings
            String referer = request.getHeader("Referer");
            if (referer != null && isMalicious(referer)) {
                return true;
            }
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && isMalicious(xForwardedFor)) {
                return true;
            }
            
            return false;
        }
        
        private boolean isMalicious(String input) {
            if (input == null || input.trim().isEmpty()) {
                return false;
            }
            
            // Convert to lowercase for case-insensitive matching, but keep original for logging
            String lowerInput = input.toLowerCase();
            
            boolean isXSS = XSS_PATTERN.matcher(lowerInput).find();
            boolean isSQLInjection = SQL_INJECTION_PATTERN.matcher(lowerInput).find();
            boolean isPathTraversal = PATH_TRAVERSAL_PATTERN.matcher(lowerInput).find();
            
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
