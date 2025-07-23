package com.lurniq.controller;

import com.lurniq.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Verification", description = "Email verification and account activation endpoints")
public class EmailVerificationController {
    
    private final EmailVerificationService emailVerificationService;
    
    @Value("${oauth2.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;
    
    @GetMapping("/activate")
    @Operation(summary = "Activate user account", description = "Activate user account using email verification token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account activated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> activateAccount(
            @Parameter(description = "Email verification token", required = true)
            @RequestParam String token) {
        
        try {
            log.info("Attempting to activate account with token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
            
            boolean activated = emailVerificationService.verifyAndActivateAccount(token);
            
            if (activated) {
                log.info("Account activated successfully");
                
                // Redirect to frontend success page
                String redirectUrl = frontendBaseUrl + "/auth/activation-success";
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account activated successfully! You can now log in to your account.",
                    "redirectUrl", redirectUrl
                ));
            } else {
                log.warn("Failed to activate account - invalid or expired token");
                
                // Redirect to frontend error page
                String redirectUrl = frontendBaseUrl + "/auth/activation-error";
                
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired activation token. Please request a new activation email.",
                    "redirectUrl", redirectUrl
                ));
            }
            
        } catch (Exception e) {
            log.error("Error during account activation: {}", e.getMessage());
            
            String redirectUrl = frontendBaseUrl + "/auth/activation-error";
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred during account activation. Please try again later.",
                "redirectUrl", redirectUrl
            ));
        }
    }
    
    @PostMapping("/resend-activation")
    @Operation(summary = "Resend activation email", description = "Resend account activation email to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activation email sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email or user already verified"),
        @ApiResponse(responseCode = "429", description = "Too many requests"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> resendActivationEmail(
            @Parameter(description = "User email address", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String email = request.get("email");
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email address is required"
                ));
            }
            
            log.info("Attempting to resend activation email to: {}", email);
            
            boolean sent = emailVerificationService.resendActivationEmail(email);
            
            if (sent) {
                log.info("Activation email resent successfully to: {}", email);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Activation email has been sent. Please check your inbox and spam folder."
                ));
            } else {
                log.warn("Failed to resend activation email to: {} - user not found or already verified", email);
                
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not found, already verified, or has a pending activation email."
                ));
            }
            
        } catch (Exception e) {
            log.error("Error during resend activation email: {}", e.getMessage());
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred while sending the activation email. Please try again later."
            ));
        }
    }
    
    @GetMapping("/activation-status/{email}")
    @Operation(summary = "Check activation status", description = "Check if user email is verified")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<?> checkActivationStatus(
            @Parameter(description = "User email address", required = true)
            @PathVariable String email) {
        
        try {
            // This would require adding a method to check user verification status
            // For now, return a simple response
            
            return ResponseEntity.ok(Map.of(
                "email", email,
                "message", "Use the resend-activation endpoint to send a new activation email if needed."
            ));
            
        } catch (Exception e) {
            log.error("Error checking activation status for: {}", email);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred while checking activation status."
            ));
        }
    }
}
