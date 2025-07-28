package com.lurniq.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // JWT Exception Handlers
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwtException(
            ExpiredJwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("JWT expired for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "JWT_EXPIRED",
            "Your session has expired. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureException(
            SignatureException ex, 
            HttpServletRequest request
    ) {
        log.warn("Invalid JWT signature for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "JWT_INVALID_SIGNATURE",
            "Invalid token signature. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJwtException(
            MalformedJwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("Malformed JWT for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "JWT_MALFORMED",
            "Invalid token format. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedJwtException(
            UnsupportedJwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("Unsupported JWT for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "JWT_UNSUPPORTED",
            "Unsupported token format. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(
            JwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("JWT validation error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "JWT_ERROR",
            "Token validation failed. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    // Existing Exception Handlers
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NoHandlerFoundException ex, 
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Endpoint not found",
            "The requested endpoint " + request.getRequestURI() + " was not found",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, 
            HttpServletRequest request
    ) {
        String supportedMethods = String.join(", ", ex.getSupportedMethods());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method not allowed",
            "HTTP method " + ex.getMethod() + " is not supported for this endpoint. Supported methods: " + supportedMethods,
            request.getRequestURI()
        );
        
        errorResponse.put("supportedMethods", ex.getSupportedMethods());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInternalServerError(
            Exception ex, 
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        
        // Log the actual exception for debugging (you can use a logger here)
        System.err.println("Internal Server Error: " + ex.getMessage());
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "Invalid input data provided",
            request.getRequestURI()
        );
        
        errorResponse.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid JSON",
            "Request body contains invalid JSON format",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Invalid credentials",
            "The provided credentials are invalid",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "User not found",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid argument",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    private Map<String, Object> createErrorResponse(
            HttpStatus status, 
            String error, 
            String message, 
            String path
    ) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        
        return errorResponse;
    }
}
