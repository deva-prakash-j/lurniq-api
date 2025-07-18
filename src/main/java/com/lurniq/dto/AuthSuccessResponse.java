package com.lurniq.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for OAuth2 authentication success response
 * 
 * @param tokenType The type of token (e.g., "Bearer")
 * @param accessToken The JWT access token for API authentication
 * @param refreshToken The refresh token for obtaining new access tokens
 * @param expires The expiration time description for the access token
 */
@Schema(description = "OAuth2 authentication success response")
public record AuthSuccessResponse(
    @Schema(description = "Token type", example = "Bearer")
    String tokenType,
    
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    
    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken,
    
    @Schema(description = "Token expiration information", example = "Access token expires in 24 hours")
    String expires
) {
}
