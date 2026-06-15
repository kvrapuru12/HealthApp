package com.healthapp.service.nutrition;

import com.healthapp.service.nutrition.RecommendedPortionCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendedPortionCatalogTest {

    @Test
    void singlePortionBanana() {
        var rec = RecommendedPortionCatalog.singlePortion("banana");
        assertEquals(1, rec.quantity());
        assertEquals("medium", rec.unit());
        assertEquals(120, rec.totalGrams());
    }

    @Test
    void singlePortionVagueSnacks() {
        var rec = RecommendedPortionCatalog.singlePortion("some snacks");
        assertEquals(80, rec.totalGrams());
    }

    @Test
    void singlePortionMangoLassi() {
        var rec = RecommendedPortionCatalog.singlePortion("mango lassi");
        assertEquals("glass", rec.unit());
        assertEquals(300, rec.totalGrams());
    }

    @Test
    void ingredientPortionSalmon() {
        assertEquals(120, RecommendedPortionCatalog.ingredientPortionGrams("grilled salmon"));
    }

    @Test
    void latteMediumCupIs300g() {
        assertEquals(300, RecommendedPortionCatalog.lattePortionGrams("latte with oat milk", "medium"));
        var rec = RecommendedPortionCatalog.singlePortion("latte with oat milk");
        assertEquals(300, rec.totalGrams());
    }

    @Test
    void latteSizeModifiers() {
        assertEquals(240, RecommendedPortionCatalog.lattePortionGrams("small latte", "small"));
        assertEquals(425, RecommendedPortionCatalog.lattePortionGrams("large latte", "large"));
    }

    @Test
    void blackCoffeeUnchanged() {
        var rec = RecommendedPortionCatalog.singlePortion("black coffee");
        assertEquals(250, rec.totalGrams());
    }

    @Test
    void sideSaladWithSandwichSmallerThanStandalone() {
        double side = RecommendedPortionCatalog.ingredientPortionGrams(
                "side salad", "turkey sandwich with cheese and side salad");
        double standalone = RecommendedPortionCatalog.singlePortion("garden salad").totalGrams();
        assertTrue(side <= standalone);
    }

    @Test
    void peanutButterTypicalMinimum() {
        assertEquals(32.0, RecommendedPortionCatalog.typicalMinimumServingGrams("peanut butter", null));
    }
}
