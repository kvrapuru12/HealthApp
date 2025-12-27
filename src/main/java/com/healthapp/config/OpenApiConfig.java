package com.healthapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.servlet.context-path:/api}")
    private String contextPath;
    
    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // Always include localhost for development
        servers.add(new Server()
            .url("http://localhost:8080" + contextPath)
            .description("Local Development Server"));
        
        // Add production server if running in AWS
        String awsAlbUrl = System.getenv("AWS_ALB_URL");
        if (awsAlbUrl != null && !awsAlbUrl.isEmpty()) {
            servers.add(new Server()
                .url(awsAlbUrl + contextPath)
                .description("Production Server (AWS)"));
        } else {
            // Default production URL (can be overridden via AWS_ALB_URL env var)
            servers.add(new Server()
                .url("http://healthapp-alb-1571435665.us-east-1.elb.amazonaws.com" + contextPath)
                .description("Production Server (AWS)"));
        }
        
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
            .servers(servers)
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
