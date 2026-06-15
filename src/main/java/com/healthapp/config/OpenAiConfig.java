package com.healthapp.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAiService openAiService(OpenAiModelProperties properties) {
        if (!properties.hasValidApiKey()) {
            return null;
        }
        return new OpenAiService(properties.getApiKey(), Duration.ofSeconds(properties.getTimeout()));
    }
    
    @Bean
    public RestTemplate restTemplate(OpenAiModelProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(5, properties.getTimeout()) * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
