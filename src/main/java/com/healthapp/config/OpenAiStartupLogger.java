package com.healthapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpenAiStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiStartupLogger.class);

    private final OpenAiModelProperties openAiModelProperties;
    private final Environment environment;

    public OpenAiStartupLogger(OpenAiModelProperties openAiModelProperties, Environment environment) {
        this.openAiModelProperties = openAiModelProperties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logOpenAiStatus() {
        if (!isLocalProfile()) {
            return;
        }
        if (openAiModelProperties.hasValidApiKey()) {
            logger.info("OpenAI API key loaded — voice AI endpoints are enabled");
            return;
        }
        logger.warn(
                "OpenAI API key is missing or blank. Voice AI endpoints will return AI_PARSE_FAILED. "
                        + "Set OPENAI_API_KEY in project-root .env, export it in ~/.zshrc, "
                        + "or pass it in your run configuration environment.");
    }

    private boolean isLocalProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("local".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
