package com.lurniq.controller;

import com.lurniq.dto.ApiResponse;
import com.lurniq.dto.AuthResponse;
import com.lurniq.dto.LoginRequest;
import com.lurniq.dto.RegisterRequest;
import com.lurniq.dto.RegistrationResponse;
import com.lurniq.service.AuthService;
import com.lurniq.service.ResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    
    private final AuthService authService;
    private final ResponseService responseService;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user with email and password")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User registered successfully", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationResponse registrationResponse = authService.register(request);
        ApiResponse<RegistrationResponse> response = responseService.success(
            "User registered successfully! Please check your email to activate your account.", 
            registrationResponse
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email and password")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.login(request, httpRequest);
        ApiResponse<AuthResponse> response = responseService.success(
            "Login successful", 
            authResponse
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        // Remove "Bearer " prefix if present
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        ApiResponse<AuthResponse> response = responseService.success(
            "Token refreshed successfully", 
            authResponse
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/oauth2/success")
    @Operation(summary = "OAuth2 success callback", description = "OAuth2 authentication success endpoint")
    public ResponseEntity<ApiResponse<String>> oauth2Success() {
        ApiResponse<String> response = responseService.success(
            "OAuth2 authentication successful", 
            "Authentication completed successfully"
        );
        return ResponseEntity.ok(response);
    }
}
