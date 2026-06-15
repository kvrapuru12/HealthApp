package com.healthapp.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExplicitMacroParserTest {

    @Test
    void parsesMuffinStyleMacros() {
        Optional<ExplicitMacroParser.StatedMacros> macros = ExplicitMacroParser.parse(
                "blueberry muffin with 300cal , P=13, C=79, F=30");
        assertTrue(macros.isPresent());
        assertEquals(300.0, macros.get().calories(), 0.1);
        assertEquals(13.0, macros.get().protein(), 0.1);
        assertEquals(79.0, macros.get().carbs(), 0.1);
        assertEquals(30.0, macros.get().fat(), 0.1);
        assertTrue(ExplicitMacroParser.isPlausible(macros.get()));
    }

    @Test
    void parsesCaloriesOnly() {
        Optional<ExplicitMacroParser.StatedMacros> macros = ExplicitMacroParser.parse("protein bar 250 kcal");
        assertTrue(macros.isPresent());
        assertEquals(250.0, macros.get().calories(), 0.1);
    }

    @Test
    void ignoresVagueText() {
        assertTrue(ExplicitMacroParser.parse("had a banana").isEmpty());
    }

    @Test
    void ignoresCaloriesFromBurnContext() {
        assertTrue(ExplicitMacroParser.parse("burned 300 calories on my run, then had a banana").isEmpty());
    }
}
