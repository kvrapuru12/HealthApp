package com.healthapp.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
public class FlywayConfig {
    // This configuration completely disables Flyway autoconfiguration
} 