package com.healthapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiActivityVoiceParsingServiceTest {

    @Test
    void ensureVoiceLineInNote_prependsWhenMissing() {
        var data = new AiActivityVoiceParsingService.ParsedActivityData();
        data.setNote("Assumed: duration 25 min.");
        AiActivityVoiceParsingService.ensureVoiceLineInNote(data, "went for a walk");
        assertEquals("Voice: went for a walk Assumed: duration 25 min.", data.getNote());
    }

    @Test
    void ensureVoiceLineInNote_fillsWhenEmpty() {
        var data = new AiActivityVoiceParsingService.ParsedActivityData();
        data.setNote("");
        AiActivityVoiceParsingService.ensureVoiceLineInNote(data, "yoga");
        assertEquals("Voice: yoga", data.getNote());
    }

    @Test
    void ensureVoiceLineInNote_noopWhenVoicePresent() {
        var data = new AiActivityVoiceParsingService.ParsedActivityData();
        data.setNote("Voice: run. Assumed: duration 20 min.");
        AiActivityVoiceParsingService.ensureVoiceLineInNote(data, "ignored");
        assertEquals("Voice: run. Assumed: duration 20 min.", data.getNote());
    }
}
