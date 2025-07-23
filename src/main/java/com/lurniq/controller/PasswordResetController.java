package com.lurniq.controller;

import com.lurniq.dto.NewPasswordRequest;
import com.lurniq.dto.PasswordResetRequest;
import com.lurniq.entity.User;
import com.lurniq.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password Reset", description = "Password reset functionality")
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    @Value("${oauth2.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send password reset email to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent (or user not found - no info disclosed for security)"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "429", description = "Too many requests"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            log.info("Password reset requested for email: {}", request.getEmail());
            
            boolean sent = passwordResetService.sendPasswordResetEmail(request.getEmail());
            
            if (sent) {
                log.info("Password reset email sent for: {}", request.getEmail());
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "If an account with that email exists, we've sent you a password reset link. Please check your inbox and spam folder."
                ));
            } else {
                // Rate limited
                log.warn("Password reset rate limited for: {}", request.getEmail());
                
                return ResponseEntity.status(429).body(Map.of(
                    "success", false,
                    "message", "Too many password reset requests. Please wait before trying again."
                ));
            }
            
        } catch (Exception e) {
            log.error("Error during password reset request for: {}. Error: {}", request.getEmail(), e.getMessage());
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred while processing your request. Please try again later."
            ));
        }
    }
    
    @GetMapping("/reset-password")
    @Operation(summary = "Verify password reset token", description = "Verify if password reset token is valid")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> verifyResetToken(
            @Parameter(description = "Password reset token", required = true)
            @RequestParam String token) {
        
        try {
            log.info("Verifying password reset token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
            
            Optional<User> userOpt = passwordResetService.verifyResetToken(token);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("Valid password reset token for user: {}", user.getEmail());
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Token is valid. You can now reset your password.",
                    "email", user.getEmail(),
                    "firstName", user.getFirstName()
                ));
            } else {
                log.warn("Invalid or expired password reset token");
                
                String redirectUrl = frontendBaseUrl + "/auth/reset-password-error";
                
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired password reset token. Please request a new one.",
                    "redirectUrl", redirectUrl
                ));
            }
            
        } catch (Exception e) {
            log.error("Error verifying password reset token: {}", e.getMessage());
            
            String redirectUrl = frontendBaseUrl + "/auth/reset-password-error";
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred while verifying the token. Please try again later.",
                "redirectUrl", redirectUrl
            ));
        }
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset user password using valid token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        try {
            log.info("Attempting password reset with token: {}", 
                request.getToken().substring(0, Math.min(request.getToken().length(), 10)) + "...");
            
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Passwords do not match."
                ));
            }
            
            // Validate password strength
            if (!passwordResetService.isPasswordValid(request.getNewPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character."
                ));
            }
            
            boolean reset = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            if (reset) {
                log.info("Password reset successfully");
                
                String redirectUrl = frontendBaseUrl + "/auth/reset-password-success";
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Your password has been reset successfully. You can now log in with your new password.",
                    "redirectUrl", redirectUrl
                ));
            } else {
                log.warn("Failed to reset password - invalid or expired token");
                
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired reset token. Please request a new password reset."
                ));
            }
            
        } catch (Exception e) {
            log.error("Error during password reset: {}", e.getMessage());
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred while resetting your password. Please try again later."
            ));
        }
    }
    
    @GetMapping("/password-requirements")
    @Operation(summary = "Get password requirements", description = "Get password strength requirements")
    @ApiResponse(responseCode = "200", description = "Password requirements retrieved")
    public ResponseEntity<?> getPasswordRequirements() {
        return ResponseEntity.ok(Map.of(
            "requirements", Map.of(
                "minLength", 8,
                "requireUppercase", true,
                "requireLowercase", true,
                "requireDigit", true,
                "requireSpecialChar", true
            ),
            "message", "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character."
        ));
    }
}
