package com.lurniq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request payload")
public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "User's password", example = "password123")
    private String password;
}
