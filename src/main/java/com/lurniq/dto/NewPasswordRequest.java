package com.lurniq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "New password confirmation request payload")
public class NewPasswordRequest {
    
    @NotBlank(message = "Token is required")
    @Schema(description = "Password reset token", example = "abc123...")
    private String token;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "New password (minimum 8 characters with uppercase, lowercase, number, and special character)", 
            example = "NewPassword123!")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Confirmation of the new password", example = "NewPassword123!")
    private String confirmPassword;
}
