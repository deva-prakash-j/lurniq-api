package com.lurniq.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Lurniq API",
        version = "1.0.0",
        description = "REST API for Lurniq - A learning management platform with OAuth2 authentication",
        contact = @Contact(
            name = "Lurniq Team",
            email = "support@lurniq.com"
        )
    ),
    servers = {
        @Server(
            url = "https://lurniq-api-production.up.railway.app",
            description = "Production Server"
        ),
        @Server(
            url = "http://localhost:8080",
            description = "Development Server"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT token obtained from login or OAuth2 authentication"
)
public class OpenApiConfig {
}
