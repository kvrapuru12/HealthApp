package com.healthapp.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.timeout:60}")
    private int timeoutSeconds;

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(openAiApiKey, Duration.ofSeconds(timeoutSeconds));
    }
}
