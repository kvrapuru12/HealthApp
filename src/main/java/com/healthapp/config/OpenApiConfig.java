package com.healthapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("HealthApp API")
                .description("Health and wellness management system API for tracking activities, nutrition, mood, and user fitness goals.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("HealthApp Team")
                    .email("support@healthapp.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.healthapp.com/api")
                    .description("Production Server")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token for authentication")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
