package com.healthapp.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplicitPortionParserTest {

    @Test
    void parsesSmoothieStyleTyposAndEmbeddedQuantity() {
        String voice = "I have a smoothie with 10 g cachenuts, 10g almonds, 200ml whole milk, "
                + "50g oasts, 2 tabel spoon chia seeds, 200 g ble berries, 100 g banana, 100g apple for breakfast";
        List<ExplicitPortionParser.ExplicitPortion> portions = ExplicitPortionParser.parse(voice);
        assertTrue(portions.size() >= 7, "expected most smoothie ingredients to parse, got " + portions.size());
        assertTrue(portions.stream().anyMatch(p -> p.foodName().toLowerCase().contains("cashew")));
        assertTrue(portions.stream().anyMatch(p -> p.foodName().toLowerCase().contains("chia")));
    }

    @Test
    void parsesMultipleGramSegments() {
        List<ExplicitPortionParser.ExplicitPortion> portions = ExplicitPortionParser.parse(
                "170 grams grilled steak, 100 grams mashed potatoes, and 80 grams green beans");
        assertEquals(3, portions.size());
        assertEquals(170.0, portions.get(0).grams(), 0.1);
        assertTrue(portions.get(0).foodName().toLowerCase().contains("steak"));
        assertEquals(100.0, portions.get(1).grams(), 0.1);
        assertEquals(80.0, portions.get(2).grams(), 0.1);
    }

    @Test
    void parsesMlAndTablespoon() {
        List<ExplicitPortionParser.ExplicitPortion> portions = ExplicitPortionParser.parse(
                "300ml red wine and 60 grams cheese");
        assertEquals(2, portions.size());
        assertEquals(300.0, portions.get(0).grams(), 0.1);
        assertEquals(60.0, portions.get(1).grams(), 0.1);
    }

    @Test
    void parsesOzAsGrams() {
        var portion = ExplicitPortionParser.parseSegment("8 oz steak");
        assertTrue(portion.isPresent());
        assertEquals(226.8, portion.get().grams(), 1.0);
    }

    @Test
    void returnsEmptyForVagueText() {
        assertTrue(ExplicitPortionParser.parse("had some snacks in the afternoon").isEmpty());
    }

    @Test
    void parsesCountPiecesAndGlassOfMilk() {
        var portions = ExplicitPortionParser.parseCountPortions("3 cookies and a glass of milk");
        assertEquals(2, portions.size());
        assertEquals(3.0, portions.get(0).quantity(), 0.1);
        assertEquals("pieces", portions.get(0).unit());
        assertTrue(portions.get(0).foodName().contains("cookie"));
        assertEquals(1.0, portions.get(1).quantity(), 0.1);
        assertEquals("glass", portions.get(1).unit());
        assertEquals("milk", portions.get(1).foodName());
    }

    @Test
    void hasExplicitMultiItemBreakdown_forCountSegments() {
        assertTrue(ExplicitPortionParser.hasExplicitMultiItemBreakdown("3 cookies and a glass of milk"));
    }
}
