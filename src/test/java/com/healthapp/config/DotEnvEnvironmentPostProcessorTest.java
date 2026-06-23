package com.healthapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void parseDotEnv_ignoresCommentsAndMapsSpringProperties() throws Exception {
        Path dotEnv = tempDir.resolve(".env");
        Files.writeString(
                dotEnv,
                """
                # comment
                OPENAI_API_KEY=sk-test-openai
                USDA_API_KEY=usda-test
                AI_QA_PASS='VoiceTest1!'
                """);

        Map<String, Object> properties = DotEnvEnvironmentPostProcessor.loadDotEnvProperties(dotEnv);

        assertEquals("sk-test-openai", properties.get("OPENAI_API_KEY"));
        assertEquals("usda-test", properties.get("USDA_API_KEY"));
        assertEquals("VoiceTest1!", properties.get("AI_QA_PASS"));
        assertTrue(properties.containsKey("openai.api.key"));
        assertTrue(properties.containsKey("nutrition.lookup.usda.api-key"));
    }
}
