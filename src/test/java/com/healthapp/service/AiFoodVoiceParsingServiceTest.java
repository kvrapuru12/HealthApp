package com.healthapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiFoodVoiceParsingServiceTest {

    @Test
    void extractJsonObject_returnsTrimmedObject_whenPlainJson() {
        String raw = "  {\"foodItems\":[]}  ";
        assertEquals("{\"foodItems\":[]}", AiFoodVoiceParsingService.extractJsonObject(raw));
    }

    @Test
    void extractJsonObject_stripsMarkdownFence() {
        String raw = "```json\n{\"a\":1}\n```";
        assertEquals("{\"a\":1}", AiFoodVoiceParsingService.extractJsonObject(raw));
    }

    @Test
    void extractJsonObject_extractsObjectFromPreamble() {
        String raw = "Here is the result: {\"x\":true} thanks";
        assertEquals("{\"x\":true}", AiFoodVoiceParsingService.extractJsonObject(raw));
    }

    @Test
    void extractJsonObject_returnsEmpty_whenNull() {
        assertEquals("", AiFoodVoiceParsingService.extractJsonObject(null));
    }
}
