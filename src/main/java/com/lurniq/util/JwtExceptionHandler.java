package com.lurniq.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lurniq.dto.ErrorResponse;
import com.lurniq.service.RequestTrackingService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtExceptionHandler {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestTrackingService requestTrackingService;
    
    public void handleJwtException(HttpServletResponse response, Exception exception, String requestUri) throws IOException {
        ErrorResponse errorResponse;
        HttpStatus status;
        
        // Get current trace ID from MDC
        String traceId = requestTrackingService.getTraceId();
        if (traceId == null || traceId.isEmpty()) {
            traceId = "unknown";
        }
        
        // Get elapsed time
        Long timeTaken = requestTrackingService.getTimeTaken();
        
        if (exception instanceof ExpiredJwtException) {
            status = HttpStatus.UNAUTHORIZED;
            errorResponse = ErrorResponse.of(
                status.value(),
                "JWT_EXPIRED",
                "Your session has expired. Please log in again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.warn("JWT expired for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage());
            
        } else if (exception instanceof SignatureException) {
            status = HttpStatus.UNAUTHORIZED;
            errorResponse = ErrorResponse.of(
                status.value(),
                "JWT_INVALID_SIGNATURE",
                "Invalid token signature. Please log in again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.warn("Invalid JWT signature for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage());
            
        } else if (exception instanceof MalformedJwtException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = ErrorResponse.of(
                status.value(),
                "JWT_MALFORMED",
                "Invalid token format. Please log in again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.warn("Malformed JWT for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage());
            
        } else if (exception instanceof UnsupportedJwtException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = ErrorResponse.of(
                status.value(),
                "JWT_UNSUPPORTED",
                "Unsupported token format. Please log in again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.warn("Unsupported JWT for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage());
            
        } else if (exception instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = ErrorResponse.of(
                status.value(),
                "JWT_INVALID",
                "Invalid token. Please log in again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.warn("Invalid JWT argument for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage());
            
        } else if (exception instanceof JwtException) {
            status = HttpStatus.UNAUTHORIZED;
            errorResponse = ErrorResponse.of(
                status.value(),
                "JWT_ERROR",
                "Token validation failed. Please log in again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.warn("JWT validation error for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage());
            
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse = ErrorResponse.of(
                status.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again.",
                requestUri,
                traceId,
                timeTaken
            );
            log.error("Unexpected error during JWT processing for request: {} - TraceID: {} - {}", requestUri, traceId, exception.getMessage(), exception);
        }
        
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // Add trace ID to response header
        response.setHeader("X-Trace-ID", traceId);
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
