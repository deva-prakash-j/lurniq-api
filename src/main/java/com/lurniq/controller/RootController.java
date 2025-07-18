package com.lurniq.controller;

import com.lurniq.dto.AuthSuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Root", description = "Root and public endpoints")
@Hidden
public class RootController {
    
    @GetMapping("/")
    @Operation(summary = "Get API information", description = "Returns basic API information and available endpoints")
    @ApiResponse(responseCode = "200", description = "API information retrieved successfully")
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
    @ApiResponse(responseCode = "200", description = "Login options retrieved successfully")
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
    @Operation(summary = "OAuth2 authentication success", description = "Handles successful OAuth2 authentication and returns JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful", 
                    content = @Content(schema = @Schema(implementation = AuthSuccessResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<AuthSuccessResponse> authSuccess(
            @Parameter(description = "JWT access token", required = true) @RequestParam String token,
            @Parameter(description = "JWT refresh token", required = true) @RequestParam String refreshToken
    ) {
        AuthSuccessResponse response = new AuthSuccessResponse(
            "Bearer",
            token,
            refreshToken,
            "Access token expires in 24 hours"
        );
        return ResponseEntity.ok(response);
    }
}
