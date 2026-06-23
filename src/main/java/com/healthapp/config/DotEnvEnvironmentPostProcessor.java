package com.healthapp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads project-root {@code .env} with highest property precedence so local secrets
 * beat shell-injected variables (e.g. Cursor's read-only {@code OPENAI_API_KEY}).
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "projectDotEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotEnv = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.isRegularFile(dotEnv)) {
            return;
        }

        Map<String, Object> properties = loadDotEnvProperties(dotEnv);
        if (properties.isEmpty()) {
            return;
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    static Map<String, Object> loadDotEnvProperties(Path dotEnv) {
        Map<String, Object> properties = parseDotEnv(dotEnv);
        mapOpenAiKey(properties);
        mapUsdaKey(properties);
        mapDbPassword(properties);
        return properties;
    }

    static Map<String, Object> parseDotEnv(Path dotEnv) {
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(dotEnv);
            for (String line : lines) {
                parseLine(line).ifPresent(entry -> properties.put(entry.key(), entry.value()));
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return properties;
    }

    private static void mapOpenAiKey(Map<String, Object> properties) {
        Object openAiKey = properties.get("OPENAI_API_KEY");
        if (openAiKey != null) {
            properties.put("openai.api.key", openAiKey);
        }
    }

    private static void mapUsdaKey(Map<String, Object> properties) {
        Object usdaKey = properties.get("USDA_API_KEY");
        if (usdaKey != null) {
            properties.put("nutrition.lookup.usda.api-key", usdaKey);
        }
    }

    private static void mapDbPassword(Map<String, Object> properties) {
        Object dbPassword = properties.get("DB_PASSWORD");
        if (dbPassword != null) {
            properties.put("spring.datasource.password", dbPassword);
        }
    }

    private static java.util.Optional<DotEnvEntry> parseLine(String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return java.util.Optional.empty();
        }
        int equals = line.indexOf('=');
        if (equals <= 0) {
            return java.util.Optional.empty();
        }
        String key = line.substring(0, equals).trim();
        String value = unquote(line.substring(equals + 1).trim());
        if (key.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new DotEnvEntry(key, value));
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private record DotEnvEntry(String key, String value) {}
}
