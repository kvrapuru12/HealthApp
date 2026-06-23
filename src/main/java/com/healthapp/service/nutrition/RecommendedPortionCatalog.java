package com.healthapp.service.nutrition;

import java.util.Locale;

/**
 * Single source of truth for typical single-portion weights (grams) used by the LLM prompt,
 * {@link com.healthapp.service.PortionGramEstimator}, and post-parse normalization.
 */
public final class RecommendedPortionCatalog {

    private RecommendedPortionCatalog() {}

    public record PortionRecommendation(double quantity, String unit, double totalGrams) {}

    /**
     * Default single portion when the user did not state quantity, unit, or grams.
     */
    public static PortionRecommendation singlePortion(String foodName) {
        String n = normalize(foodName);
        if (n.isEmpty()) {
            return new PortionRecommendation(1, "serving", 200);
        }
        if (NutritionValidator.isVaguePortion(n)) {
            return new PortionRecommendation(1, "serving", 80);
        }
        if (n.contains("banana")) {
            return new PortionRecommendation(1, "medium", 120);
        }
        if (n.contains("apple") || n.contains("orange")) {
            return new PortionRecommendation(1, "medium", 150);
        }
        if (n.contains("egg")) {
            return new PortionRecommendation(1, "pieces", 50);
        }
        if (n.contains("avocado")) {
            return new PortionRecommendation(1, "medium", 150);
        }
        if (n.contains("coffee") && !n.contains("latte") && !n.contains("cappuccino")) {
            return new PortionRecommendation(1, "cup", 250);
        }
        if (n.contains("latte") || n.contains("cappuccino") || n.contains("mocha")) {
            return new PortionRecommendation(1, "cup", lattePortionGrams(n, extractLatteSize(n)));
        }
        if (n.contains("espresso")) {
            return new PortionRecommendation(1, "shot", 30);
        }
        if (n.contains("lassi") || n.contains("smoothie") || n.contains("milkshake")) {
            return new PortionRecommendation(1, "glass", 300);
        }
        if (n.contains("juice") || n.contains("coke") || n.contains("cola") || n.contains("soda")) {
            return new PortionRecommendation(1, "glass", 330);
        }
        if (n.contains("wine")) {
            return new PortionRecommendation(1, "glass", 150);
        }
        if (n.contains("beer")) {
            return new PortionRecommendation(1, "glass", 355);
        }
        if (n.contains("milk") && !n.contains("shake")) {
            return new PortionRecommendation(1, "glass", 240);
        }
        if (n.contains("muffin") || n.contains("croissant")) {
            return new PortionRecommendation(1, "serving", 110);
        }
        if (n.contains("cookie")) {
            return new PortionRecommendation(1, "pieces", 32);
        }
        if (n.contains("toast") || n.contains("bread") || n.contains("slice")) {
            return new PortionRecommendation(1, "slices", 30);
        }
        if (n.contains("sandwich") || n.contains("wrap")) {
            return new PortionRecommendation(1, "serving", 220);
        }
        if (n.contains("burger")) {
            return new PortionRecommendation(1, "serving", 200);
        }
        if (n.contains("fries") || n.contains("french fry")) {
            return new PortionRecommendation(1, "serving", 130);
        }
        if (n.contains("crisp") || n.contains("chip") && !n.contains("chocolate")) {
            return new PortionRecommendation(1, "serving", 32);
        }
        if (n.contains("salad")) {
            return new PortionRecommendation(1, "serving", 200);
        }
        if (n.contains("soup")) {
            return new PortionRecommendation(1, "serving", 250);
        }
        if (n.contains("quinoa") || n.contains("rice") || n.contains("pasta")) {
            return new PortionRecommendation(1, "cup", 185);
        }
        if (n.contains("broccoli") || n.contains("vegetable")) {
            return new PortionRecommendation(1, "cup", 150);
        }
        if (n.contains("oatmeal") || n.contains("porridge")) {
            return new PortionRecommendation(1, "cup", 100);
        }
        if (n.contains("yogurt")) {
            return new PortionRecommendation(1, "serving", 170);
        }
        if (n.contains("biryani") || n.contains("curry") || n.contains("thali")) {
            return new PortionRecommendation(1, "serving", 350);
        }
        if (n.contains("pizza")) {
            return new PortionRecommendation(1, "slices", 120);
        }
        if (n.contains("salmon") || n.contains("chicken breast") || n.contains("steak")) {
            return new PortionRecommendation(1, "serving", 150);
        }
        if (n.contains("protein shake") || (n.contains("whey") && n.contains("shake"))) {
            return new PortionRecommendation(1, "glass", 350);
        }
        if (n.contains("almond") || n.contains("nut")) {
            return new PortionRecommendation(1, "serving", 30);
        }
        if (n.contains("butter") && !n.contains("peanut")) {
            return new PortionRecommendation(1, "tablespoon", 14);
        }
        if (n.contains("peanut butter")) {
            return new PortionRecommendation(1, "tablespoon", 16);
        }
        if (n.contains("olive oil") || CookingOilDetector.containsCookingOil(n)) {
            return new PortionRecommendation(1, "tablespoon", 14);
        }
        return new PortionRecommendation(1, "serving", 200);
    }

    public static double gramsPerUnit(String foodName, String unit, double quantity) {
        double qty = quantity > 0 ? quantity : 1.0;
        String normalizedUnit = unit != null ? unit.toLowerCase(Locale.ROOT).trim() : "serving";
        String normalizedFood = normalize(foodName);
        return qty * weightPerUnit(normalizedUnit, normalizedFood);
    }

    public static double weightPerUnit(String unit, String foodName) {
        String normalizedUnit = unit != null ? unit.toLowerCase(Locale.ROOT).trim() : "serving";
        return switch (normalizedUnit) {
            case "pieces", "piece", "slice", "slices" -> weightForPiece(foodName);
            case "cup", "cups" -> weightForCup(foodName);
            case "glass", "glasses" -> weightForGlass(foodName);
            case "tablespoon", "tablespoons", "tbsp" -> weightForTablespoon(foodName);
            case "teaspoon", "teaspoons", "tsp" -> weightForTeaspoon(foodName);
            case "shot", "shots" -> 30.0;
            case "gram", "grams", "g" -> 1.0;
            case "milliliter", "milliliters", "millilitre", "millilitres", "ml" -> 1.0;
            case "liter", "liters", "litre", "litres", "l" -> 1000.0;
            case "kilogram", "kilograms", "kg" -> 1000.0;
            case "ounce", "ounces", "oz" -> 28.35;
            case "pound", "pounds", "lb" -> 453.59;
            case "serving", "servings" -> weightForServing(foodName);
            case "medium" -> weightForSize(foodName, "medium");
            case "small" -> weightForSize(foodName, "small");
            case "large" -> weightForSize(foodName, "large");
            default -> weightForPiece(foodName);
        };
    }

    public static double minimumPlausibleGrams(String foodName) {
        String n = normalize(foodName);
        if (n.isEmpty()) {
            return 5.0;
        }
        if (NutritionValidator.isVaguePortion(n)) {
            return 80.0;
        }
        if (n.contains("nuts") || n.contains("crisp") || n.contains("chip")) {
            return 5.0;
        }
        if (n.contains("thali") || n.contains("biryani") || n.contains("burger") && n.contains("fries")) {
            return 350.0;
        }
        if (n.contains("plate") || n.contains("combo") || n.contains("meal")) {
            return 250.0;
        }
        if (n.contains("toast") && (n.contains("butter") || n.contains("peanut"))) {
            return 90.0;
        }
        if (n.contains("sandwich")) {
            return 220.0;
        }
        if (n.contains("salmon") || (n.contains("quinoa") && n.contains("broccoli"))) {
            return 400.0;
        }
        if (NutritionValidator.isCaloricBeverage(n)) {
            return 200.0;
        }
        return 5.0;
    }

    /** Typical grams for one ingredient line inside a composite meal. */
    public static double ingredientPortionGrams(String ingredientName) {
        return ingredientPortionGrams(ingredientName, null);
    }

    public static double ingredientPortionGrams(String ingredientName, String mealContext) {
        String n = normalize(ingredientName);
        String meal = mealContext != null ? normalize(mealContext) : "";
        if (n.contains("toast") || n.contains("bread") || n.contains("slice")) {
            return 60.0;
        }
        if (n.contains("peanut butter")) {
            return 32.0;
        }
        if (n.contains("butter") && !n.contains("peanut")) {
            return 14.0;
        }
        if (n.contains("egg")) {
            return 50.0;
        }
        if (n.contains("avocado")) {
            return 75.0;
        }
        if (n.contains("chicken") || n.contains("salmon") || n.contains("fish") || n.contains("steak")) {
            return 120.0;
        }
        if (n.contains("rice") || n.contains("pasta") || n.contains("quinoa")) {
            return 150.0;
        }
        if (n.contains("salad") || n.contains("vegetable") || n.contains("broccoli")) {
            if (isSideSalad(n, meal) && mealContainsMainDish(meal)) {
                return 100.0;
            }
            return 100.0;
        }
        if (n.contains("olive oil") || CookingOilDetector.containsCookingOil(n)) {
            return 14.0;
        }
        if (n.contains("muffin") || n.contains("cookie")) {
            return 95.0;
        }
        if (n.contains("latte") || n.contains("cappuccino")) {
            return lattePortionGrams(n, "medium");
        }
        if (n.contains("milk") && !n.contains("shake")) {
            return 240.0;
        }
        if (n.contains("sandwich")) {
            return 220.0;
        }
        if (n.contains("fries") || n.contains("fry")) {
            return 130.0;
        }
        if (n.contains("burger")) {
            return 180.0;
        }
        if (n.contains("cheese")) {
            return 28.0;
        }
        if (n.contains("banana")) {
            return 120.0;
        }
        if (n.contains("protein powder") || n.contains("whey")) {
            return 30.0;
        }
        return 80.0;
    }

    /**
     * Minimum reasonable single serving (grams) for portion sanity correction when AI underestimates.
     */
    public static double typicalMinimumServingGrams(String foodName, String mealContext) {
        String n = normalize(foodName);
        if (n.contains("peanut butter")) {
            return 32.0;
        }
        if (n.contains("butter") && !n.contains("peanut")) {
            return 10.0;
        }
        if (n.contains("jam") || n.contains("jelly") || n.contains("marmalade")) {
            return 20.0;
        }
        if (n.contains("cream cheese")) {
            return 15.0;
        }
        if (n.contains("whey") || n.contains("protein powder")) {
            return 30.0;
        }
        if (n.contains("cracker") && (n.contains("cheese") || normalize(mealContext).contains("cheese"))) {
            return 40.0;
        }
        if (n.contains("cheese") && (n.contains("board") || normalize(mealContext).contains("cheese board"))) {
            return 120.0;
        }
        return 0;
    }

    public static double lattePortionGrams(String foodName, String size) {
        String normalizedSize = size != null ? size.toLowerCase(Locale.ROOT).trim() : "medium";
        return switch (normalizedSize) {
            case "small" -> 240.0;
            case "large" -> 425.0;
            default -> 250.0;
        };
    }

    private static boolean isSideSalad(String ingredientName, String mealContext) {
        return ingredientName.contains("side salad")
                || (ingredientName.contains("salad") && mealContext.contains("sandwich"));
    }

    private static boolean mealContainsMainDish(String mealContext) {
        return mealContext.contains("sandwich")
                || mealContext.contains("burger")
                || mealContext.contains("wrap");
    }

    public static String promptReferenceLines() {
        return """
        Recommended single portions when the user gives NO quantity or grams (quantity=1 unless stated):
        - banana / apple → medium, ~120–150g | boiled egg → 1 piece ~50g, two eggs → 2 pieces ~100g
        - black coffee → 1 cup ~250g | latte/cappuccino → 1 medium cup ~250g (small ~240g, large ~425g) | espresso → 1 shot ~30g
        - juice/soda/coke → 1 glass ~330ml | wine → 1 glass ~150ml | beer → 1 can/glass ~355ml
        - mango lassi / smoothie → 1 glass ~300g | milk → 1 glass ~240g
        - muffin/croissant → 1 serving ~95g | cookie → 1 piece ~32g
        - crisps/chips snack bag → 1 serving ~32g | sandwich/wrap → 1 serving ~220g
        - cooked rice/quinoa/pasta → 1 cup ~185g | standalone salad → 1 serving ~200g | side salad with sandwich → ~100g
        - chicken breast / salmon fillet → 1 serving ~150g | burger → ~200g | fries side → ~130g
        - curry/biryani/thali → 1 plate ~350g | protein shake → 1 glass ~350g
        - vague "some snacks" → modest portion ~80g, not a full bag
        """;
    }

    private static String normalize(String foodName) {
        return foodName != null ? foodName.toLowerCase(Locale.ROOT).trim() : "";
    }

    private static double weightForPiece(String foodName) {
        if (foodName.contains("egg")) return 50.0;
        if (foodName.contains("apple")) return 150.0;
        if (foodName.contains("banana")) return 120.0;
        if (foodName.contains("orange")) return 130.0;
        if (foodName.contains("cashew") || foodName.contains("walnut")) return 2.5;
        if (foodName.contains("almond")) return 1.0;
        if (foodName.contains("cookie")) return 32.0;
        if (foodName.contains("toast") || foodName.contains("bread") || foodName.contains("slice")) return 30.0;
        if (foodName.contains("hash brown")) return 300.0;
        if (foodName.contains("avocado")) return 150.0;
        if (foodName.contains("tomato")) return 120.0;
        if (foodName.contains("potato") && !foodName.contains("chip")) return 170.0;
        return 100.0;
    }

    private static double weightForSize(String foodName, String size) {
        double base = weightForPiece(foodName);
        return switch (size) {
            case "small" -> base * 0.75;
            case "large" -> base * 1.35;
            default -> base;
        };
    }

    private static double weightForCup(String foodName) {
        if (foodName.contains("quinoa")) return 185.0;
        if (foodName.contains("coffee") || foodName.contains("tea")) return 250.0;
        if (foodName.contains("milk") || foodName.contains("lassi") || foodName.contains("yogurt drink")) return 240.0;
        if (foodName.contains("rice")) return 200.0;
        if (foodName.contains("pasta")) return 140.0;
        if (foodName.contains("oats") || foodName.contains("oatmeal")) return 100.0;
        if (foodName.contains("broccoli") || foodName.contains("vegetable")) return 150.0;
        return 200.0;
    }

    private static double weightForGlass(String foodName) {
        if (foodName.contains("coke") || foodName.contains("cola") || foodName.contains("soda")) return 330.0;
        if (foodName.contains("wine")) return 150.0;
        if (foodName.contains("milk") && !foodName.contains("shake")) return 240.0;
        if (foodName.contains("lassi") || foodName.contains("smoothie") || foodName.contains("shake")) return 300.0;
        if (foodName.contains("cappuccino") || foodName.contains("latte")) {
            return lattePortionGrams(foodName, extractLatteSize(foodName));
        }
        return 250.0;
    }

    private static double weightForTablespoon(String foodName) {
        if (CookingOilDetector.containsCookingOil(foodName) || (foodName.contains("butter") && !foodName.contains("peanut"))) {
            return 14.0;
        }
        if (foodName.contains("peanut butter")) return 16.0;
        if (foodName.contains("honey") || foodName.contains("syrup")) return 21.0;
        return 15.0;
    }

    private static double weightForTeaspoon(String foodName) {
        if (foodName.contains("sugar")) return 4.0;
        if (foodName.contains("salt")) return 6.0;
        if (CookingOilDetector.containsCookingOil(foodName)
                || (foodName.contains("butter") && !foodName.contains("peanut"))) {
            return 4.5;
        }
        return 5.0;
    }

    private static double weightForServing(String foodName) {
        if (foodName.contains("fish") && foodName.contains("chip")) return 200.0;
        if (foodName.contains("fries") || foodName.contains("french fry")) return 130.0;
        if (foodName.contains("tortilla chip") || foodName.contains("potato chip") || foodName.contains("kettle chip")) {
            return 32.0;
        }
        if ((foodName.contains("crisp") || foodName.contains("crisps")) && !foodName.contains("crispy")) {
            return 32.0;
        }
        if (foodName.contains("chip")
                && !foodName.contains("chocolate")
                && !foodName.contains("cookie")) {
            return 32.0;
        }
        if (foodName.contains("cookie")) return 90.0;
        if (foodName.contains("muffin")) return 95.0;
        if (foodName.contains("burger")) return 200.0;
        if (foodName.contains("salad")) {
            return 200.0;
        }
        if (foodName.contains("soup")) return 250.0;
        if (foodName.contains("curry") || foodName.contains("stew") || foodName.contains("biryani")) return 350.0;
        if (foodName.contains("lassi") || foodName.contains("smoothie")) return 300.0;
        return 200.0;
    }

    private static String extractLatteSize(String foodName) {
        String n = normalize(foodName);
        if (n.contains("small")) {
            return "small";
        }
        if (n.contains("large")) {
            return "large";
        }
        return "medium";
    }
}
