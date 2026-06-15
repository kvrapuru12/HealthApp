package com.healthapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiFoodVoiceParsingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractJsonObject_stripsMarkdownFence() {
        String raw = """
                ```json
                {"foodItems":[],"compositeMeals":[]}
                ```
                """;
        assertTrue(AiFoodVoiceParsingService.extractJsonObject(raw).contains("foodItems"));
    }

    @Test
    void extractJsonObject_findsEmbeddedObject() {
        String raw = "Here is data: {\"estimatedGrams\":150}";
        assertEquals("{\"estimatedGrams\":150}", AiFoodVoiceParsingService.extractJsonObject(raw));
    }

    @Test
    void parseEstimatedGramsFromSamplePayload() throws Exception {
        String json = """
                {"compositeMeals":[],"foodItems":[{"foodName":"avocado","quantity":1,"unit":"medium","estimatedGrams":150,"mealType":"snack","loggedAt":"2026-05-04T12:00:00","note":"test"}]}
                """;
        var node = objectMapper.readTree(json).get("foodItems").get(0);
        assertEquals(150.0, node.get("estimatedGrams").asDouble());
    }
}
