package com.healthapp.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.timeout:60}")
    private int timeoutSeconds;

    @Bean
    public OpenAiService openAiService() {
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty() || openAiApiKey.equals("your-openai-api-key-here")) {
            return null; // Return null if no valid API key
        }
        return new OpenAiService(openAiApiKey, Duration.ofSeconds(timeoutSeconds));
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
