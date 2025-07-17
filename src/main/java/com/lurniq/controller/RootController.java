package com.lurniq.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = Map.of(
            "message", "Lurniq API is running",
            "version", "1.0.0",
            "endpoints", Map.of(
                "register", "/api/auth/register",
                "login", "/api/auth/login",
                "google_oauth", "/oauth2/authorization/google",
                "user_profile", "/api/user/profile",
                "refresh_token", "/api/auth/refresh-token"
            ),
            "error_handling", Map.of(
                "description", "Global exception handling is enabled",
                "supported_errors", new String[]{
                    "400 - Bad Request (validation, invalid JSON)",
                    "401 - Unauthorized (invalid credentials)",
                    "404 - Not Found (endpoint not found)",
                    "405 - Method Not Allowed",
                    "500 - Internal Server Error"
                }
            )
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/login")
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
    public ResponseEntity<Map<String, Object>> authSuccess(
            @RequestParam String token,
            @RequestParam String refreshToken
    ) {
        Map<String, Object> response = Map.of(
            "message", "OAuth2 authentication successful! You can now use these tokens to access protected endpoints.",
            "accessToken", token,
            "refreshToken", refreshToken,
            "tokenType", "Bearer",
            "usage", Map.of(
                "example", "curl -H 'Authorization: Bearer " + token + "' http://localhost:8080/api/user/profile",
                "expires", "Access token expires in 24 hours",
                "refresh", "Use refresh token to get new access token via POST /api/auth/refresh-token"
            )
        );
        return ResponseEntity.ok(response);
    }
}
