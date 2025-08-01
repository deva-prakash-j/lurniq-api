package com.lurniq.controller;

import com.lurniq.dto.ApiResponse;
import com.lurniq.dto.AuthResponse;
import com.lurniq.dto.UserProfileResponse;
import com.lurniq.entity.User;
import com.lurniq.repository.UserRepository;
import com.lurniq.service.ResponseService;
import com.lurniq.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Root", description = "Root and public endpoints")
@Hidden
@RequiredArgsConstructor
public class RootController {
    
    private final ResponseService responseService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    
    @GetMapping("/")
    @Operation(summary = "Get API information", description = "Returns basic API information and available endpoints")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API information retrieved successfully")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = Map.of(
            "message", "Lurniq API is running",
            "version", "1.0.0",
            "documentation", Map.of(
                "swagger_ui", "/swagger-ui.html",
                "api_docs", "/api-docs"
            )
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/login")
    @Operation(summary = "Get login options", description = "Returns available login methods")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login options retrieved successfully")
    public ResponseEntity<Map<String, Object>> login() {
        Map<String, Object> response = Map.of(
            "message", "Login page",
            "options", Map.of(
                "email_login", "/api/auth/login",
                "google_oauth", "/oauth2/authorization/google"
            )
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/auth/success")
    @Operation(summary = "OAuth2 authentication success", description = "Handles successful OAuth2 authentication and returns JWT tokens with user profile")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> authSuccess(
            @Parameter(description = "JWT access token", required = true) @RequestParam String token,
            @Parameter(description = "JWT refresh token", required = true) @RequestParam String refreshToken
    ) {
        try {
            // Extract username from JWT token
            String username = jwtUtil.extractUsername(token);
            
            // Get user details
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Create AuthResponse similar to login endpoint
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(token)
                    .refreshToken(refreshToken)
                    .user(UserProfileResponse.fromUser(user))
                    .build();
            
            ApiResponse<AuthResponse> response = responseService.success(
                "OAuth2 authentication successful", 
                authResponse
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Fallback to simple response if user lookup fails
            ApiResponse<AuthResponse> response = responseService.error(
                "Authentication successful but user details unavailable"
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
}
