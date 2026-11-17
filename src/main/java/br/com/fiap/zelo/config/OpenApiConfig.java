package br.com.fiap.zelo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Zelo Monitor API — Monitoramento Não Invasivo de Rotina",
                version = "1.0.0",
                description = "Backend Java Advanced (Spring Boot 3, Spring Security JWT, RabbitMQ e Spring AI) — Global Solution 2026/2 CitizenTech FIAP",
                contact = @Contact(name = "Leandro Guarido de Oliveira (RM 561760)")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Cole aqui apenas o valor de accessToken obtido em POST /api/v1/auth/login"
)
public class OpenApiConfig {
}
