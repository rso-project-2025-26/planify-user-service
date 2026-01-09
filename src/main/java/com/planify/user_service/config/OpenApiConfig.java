package com.planify.user_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Planify User Service API",
        version = "1.0.0",
        description = "Microservice for authentication and user management. Handles user registration, authentication via Keycloak, organization management, and member invitations.",
        contact = @Contact(
            name = "Planify User Service Repository - Documentation",
            url = "https://github.com/rso-project-2025-26/planify-user-service"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8082", description = "Local Development"),
        // @Server(url = "", description = "Production")
    }
)
@SecurityScheme(
    name = "bearer-jwt",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT authentication token from Keycloak. Obtain token from /api/auth/register."
)
public class OpenApiConfig {
}
