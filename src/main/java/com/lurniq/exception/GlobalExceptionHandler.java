package com.lurniq.exception;

import com.lurniq.dto.ErrorResponse;
import com.lurniq.service.RequestTrackingService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final RequestTrackingService requestTrackingService;
    
    // JWT Exception Handlers
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(
            ExpiredJwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("JWT expired for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "JWT_EXPIRED",
            "Your session has expired. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(
            SignatureException ex, 
            HttpServletRequest request
    ) {
        log.warn("Invalid JWT signature for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "JWT_INVALID_SIGNATURE",
            "Invalid token signature. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(
            MalformedJwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("Malformed JWT for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "JWT_MALFORMED",
            "Invalid token format. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedJwtException(
            UnsupportedJwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("Unsupported JWT for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "JWT_UNSUPPORTED",
            "Unsupported token format. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, 
            HttpServletRequest request
    ) {
        log.warn("JWT validation error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "JWT_ERROR",
            "Token validation failed. Please log in again.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    // General Exception Handlers
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException ex, 
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "ENDPOINT_NOT_FOUND",
            "The requested endpoint " + request.getRequestURI() + " was not found",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, 
            HttpServletRequest request
    ) {
        String supportedMethods = String.join(", ", ex.getSupportedMethods());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "HTTP method " + ex.getMethod() + " is not supported for this endpoint. Supported methods: " + supportedMethods,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = createErrorResponseWithFieldErrors(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "Invalid input data provided",
            request.getRequestURI(),
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_JSON",
            "Request body contains invalid JSON format",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            "The provided credentials are invalid",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_ARGUMENT",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError(
            Exception ex, 
            HttpServletRequest request
    ) {
        log.error("Internal Server Error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    // Helper methods for creating error responses
    private ErrorResponse createErrorResponse(
            HttpStatus status, 
            String error, 
            String message, 
            String path
    ) {
        String traceId = requestTrackingService.getTraceId();
        Long timeTaken = requestTrackingService.getTimeTaken();
        
        return ErrorResponse.of(status.value(), error, message, path, traceId, timeTaken);
    }
    
    private ErrorResponse createErrorResponseWithFieldErrors(
            HttpStatus status, 
            String error, 
            String message, 
            String path,
            Map<String, String> fieldErrors
    ) {
        String traceId = requestTrackingService.getTraceId();
        Long timeTaken = requestTrackingService.getTimeTaken();
        
        return ErrorResponse.withFieldErrors(status.value(), error, message, path, traceId, timeTaken, fieldErrors);
    }
}
