package com.autohub.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AutoHub Spare Parts API",
                version = "2026.1.6",
                description = "Complete Backend API for AutoHub. Features include MFA authentication, " +
                        "a multi-step Vehicle Discovery Engine (Make/Model/Year), and " +
                        "comprehensive Admin CRUD for inventory management."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
        // This class primarily uses annotations to configure the OpenAPI (Swagger) definition.
}