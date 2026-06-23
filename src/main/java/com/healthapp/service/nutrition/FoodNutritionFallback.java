package com.healthapp.service.nutrition;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Hardcoded per-100g nutrition when USDA lookup misses (ingredient blending, voice composites).
 */
@Component
public class FoodNutritionFallback {

    public Optional<NutritionProfile> resolveKnown(String foodName) {
        Macros m = macrosFor(foodName);
        if (m == null) {
            return Optional.empty();
        }
        return Optional.of(new NutritionProfile(
                m.calories, m.protein, m.carbs, m.fat, m.fiber,
                NutritionSource.FALLBACK_HARDCODED, 0.5, null));
    }

    private Macros macrosFor(String foodName) {
        String n = foodName != null ? foodName.toLowerCase(Locale.ROOT).trim() : "";
        return switch (n) {
            case "pasta", "spaghetti", "macaroni" -> new Macros(131, 5.0, 25.0, 1.1, 1.8);
            case "rice", "white rice" -> new Macros(130, 2.7, 28.0, 0.3, 0.4);
            case "bread", "white bread" -> new Macros(265, 9.0, 49.0, 3.2, 2.7);
            case "whole wheat bread", "wholemeal bread" -> new Macros(247, 13.0, 41.0, 4.2, 6.0);
            case "chicken", "chicken breast" -> new Macros(165, 31.0, 0.0, 3.6, 0.0);
            case "salmon" -> new Macros(208, 25.0, 0.0, 12.0, 0.0);
            case "eggs", "egg" -> new Macros(155, 13.0, 1.1, 11.0, 0.0);
            case "broccoli" -> new Macros(34, 2.8, 7.0, 0.4, 2.6);
            case "spinach" -> new Macros(23, 2.9, 3.6, 0.4, 2.2);
            case "dal", "lentils", "lentil dal" -> new Macros(116, 9.0, 20.0, 0.4, 2.0);
            case "roti", "chapati" -> new Macros(297, 9.0, 46.0, 7.0, 4.0);
            case "paneer" -> new Macros(265, 18.0, 2.0, 20.0, 0.0);
            case "peanut butter" -> new Macros(588, 25.0, 20.0, 50.0, 8.0);
            case "butter" -> new Macros(717, 0.9, 0.1, 81.0, 0.0);
            case "olive oil" -> new Macros(884, 0.0, 0.0, 100.0, 0.0);
            case "quinoa", "cooked quinoa" -> new Macros(120, 4.4, 21.3, 1.9, 2.8);
            case "coffee", "black coffee" -> new Macros(2, 0.3, 0.0, 0.0, 0.0);
            case "mango lassi", "lassi" -> new Macros(83, 3.0, 14.0, 2.0, 0.5);
            case "red wine", "wine" -> new Macros(85, 0.1, 2.6, 0.0, 0.0);
            case "cola", "coke", "soda" -> new Macros(42, 0.0, 10.6, 0.0, 0.0);
            case "cappuccino", "latte" -> new Macros(40, 2.0, 4.0, 2.0, 0.0);
            case "milk" -> new Macros(42, 3.4, 5.0, 1.0, 0.0);
            case "cookie", "cookies" -> new Macros(420, 5.0, 55.0, 20.0, 2.0);
            case "muffin" -> new Macros(380, 5.0, 50.0, 18.0, 2.0);
            case "burger" -> new Macros(250, 13.0, 25.0, 12.0, 1.0);
            case "fries", "french fries" -> new Macros(312, 3.4, 41.0, 15.0, 3.8);
            case "snacks", "snack" -> new Macros(450, 6.0, 50.0, 25.0, 3.0);
            case "steak", "beef steak", "sirloin" -> new Macros(271, 26.0, 0.0, 18.0, 0.0);
            case "mashed potatoes", "mashed potato" -> new Macros(106, 2.0, 17.0, 3.5, 1.5);
            case "green beans" -> new Macros(35, 2.0, 8.0, 0.2, 3.0);
            case "chocolate cake", "cake" -> new Macros(389, 5.0, 53.0, 18.0, 2.0);
            case "ice cream", "vanilla ice cream" -> new Macros(207, 3.5, 24.0, 11.0, 0.7);
            case "pizza", "pepperoni pizza" -> new Macros(266, 11.0, 33.0, 10.0, 2.0);
            case "cheese" -> new Macros(402, 25.0, 1.3, 33.0, 0.0);
            case "whey protein", "protein powder" -> new Macros(400, 80.0, 8.0, 5.0, 0.0);
            case "mixed nuts", "almonds" -> new Macros(576, 21.0, 22.0, 49.0, 12.0);
            case "cashews", "cashew nuts" -> new Macros(553, 18.0, 30.0, 44.0, 3.3);
            case "oats", "rolled oats", "oatmeal dry" -> new Macros(389, 16.9, 66.3, 6.9, 10.6);
            case "chia seeds", "chia" -> new Macros(486, 16.5, 42.1, 30.7, 34.4);
            case "blueberries" -> new Macros(57, 0.7, 14.5, 0.3, 2.4);
            case "whole milk" -> new Macros(61, 3.2, 4.8, 3.3, 0.0);
            case "crisps", "potato chips" -> new Macros(536, 7.0, 53.0, 34.0, 4.0);
            case "oatmeal", "porridge" -> new Macros(71, 2.5, 12.0, 1.5, 1.7);
            case "greek yogurt", "plain greek yogurt" -> new Macros(59, 10.0, 3.6, 0.4, 0.0);
            case "pad thai" -> new Macros(180, 9.0, 24.0, 6.0, 2.0);
            case "fish and chips" -> new Macros(220, 12.0, 22.0, 10.0, 2.0);
            case "croissant" -> new Macros(406, 8.2, 45.0, 21.0, 2.6);
            case "orange juice" -> new Macros(45, 0.7, 10.4, 0.2, 0.2);
            case "hummus" -> new Macros(166, 8.0, 14.0, 9.6, 6.0);
            case "celery" -> new Macros(16, 0.7, 3.0, 0.2, 1.6);
            case "espresso" -> new Macros(9, 0.1, 1.7, 0.2, 0.0);
            case "protein shake" -> new Macros(120, 20.0, 8.0, 2.0, 1.0);
            case "thai iced tea" -> new Macros(80, 1.0, 18.0, 2.0, 0.0);
            case "side salad", "salad", "mixed greens salad" -> new Macros(40, 1.8, 7.0, 0.6, 2.5);
            case "garlic knots", "garlic knot" -> new Macros(330, 9.0, 52.0, 9.0, 2.5);
            case "masala dosa" -> new Macros(210, 4.5, 30.0, 8.0, 3.0);
            case "coconut chutney" -> new Macros(260, 3.0, 10.0, 24.0, 4.0);
            case "sambar" -> new Macros(55, 2.5, 8.0, 1.2, 2.0);
            case "salmon avocado sushi", "salmon avocado sushi roll" -> new Macros(180, 9.0, 24.0, 5.0, 1.0);
            case "tuna rolls", "tuna roll" -> new Macros(155, 8.0, 22.0, 2.5, 0.8);
            default -> macrosFromContains(n);
        };
    }

    private Macros macrosFromContains(String n) {
        if (n.contains("pad thai")) {
            return new Macros(180, 9.0, 24.0, 6.0, 2.0);
        }
        if (n.contains("fish and chips") || (n.contains("fish") && n.contains("chip"))) {
            return new Macros(220, 12.0, 22.0, 10.0, 2.0);
        }
        if (n.contains("croissant")) {
            return new Macros(406, 8.2, 45.0, 21.0, 2.6);
        }
        if (n.contains("orange juice") || (n.contains("juice") && !n.contains("smoothie"))) {
            return new Macros(45, 0.7, 10.4, 0.2, 0.2);
        }
        if (n.contains("hummus")) {
            return new Macros(166, 8.0, 14.0, 9.6, 6.0);
        }
        if (n.contains("celery")) {
            return new Macros(16, 0.7, 3.0, 0.2, 1.6);
        }
        if (n.contains("espresso")) {
            return new Macros(9, 0.1, 1.7, 0.2, 0.0);
        }
        if (n.contains("protein shake") || (n.contains("whey") && n.contains("shake"))) {
            return new Macros(120, 20.0, 8.0, 2.0, 1.0);
        }
        if (n.contains("thai iced tea") || n.contains("iced tea")) {
            return new Macros(80, 1.0, 18.0, 2.0, 0.0);
        }
        if (n.contains("oatmeal") || n.contains("porridge")) {
            return new Macros(71, 2.5, 12.0, 1.5, 1.7);
        }
        if (n.contains("greek yogurt")) {
            return new Macros(59, 10.0, 3.6, 0.4, 0.0);
        }
        if (n.contains("side salad") || (n.contains("salad") && !n.contains("chicken salad"))) {
            return new Macros(40, 1.8, 7.0, 0.6, 2.5);
        }
        if (n.contains("garlic knot")) {
            return new Macros(330, 9.0, 52.0, 9.0, 2.5);
        }
        if (n.contains("masala dosa")) {
            return new Macros(210, 4.5, 30.0, 8.0, 3.0);
        }
        if (n.contains("coconut chutney")) {
            return new Macros(260, 3.0, 10.0, 24.0, 4.0);
        }
        if (n.contains("sambar")) {
            return new Macros(55, 2.5, 8.0, 1.2, 2.0);
        }
        if (n.contains("salmon avocado sushi")) {
            return new Macros(180, 9.0, 24.0, 5.0, 1.0);
        }
        if (n.contains("tuna roll")) {
            return new Macros(155, 8.0, 22.0, 2.5, 0.8);
        }
        if (n.contains("tuna") && n.contains("wrap")) {
            return new Macros(220, 14.0, 22.0, 9.0, 2.0);
        }
        if (n.contains("toast") || n.contains("bread")) {
            return new Macros(247, 13.0, 41.0, 4.2, 6.0);
        }
        if (n.contains("steak") || n.contains("sirloin") || (n.contains("beef") && !n.contains("broth"))) {
            return new Macros(271, 26.0, 0.0, 18.0, 0.0);
        }
        if (n.contains("salmon") || (n.contains("grill") && n.contains("fish"))) {
            return new Macros(208, 25.0, 0.0, 12.0, 0.0);
        }
        if (n.contains("chicken") && n.contains("breast")) {
            return new Macros(165, 31.0, 0.0, 3.6, 0.0);
        }
        if (n.contains("quinoa")) {
            return new Macros(120, 4.4, 21.3, 1.9, 2.8);
        }
        if (n.contains("broccoli")) {
            return new Macros(34, 2.8, 7.0, 0.4, 2.6);
        }
        if (n.contains("spinach")) {
            return new Macros(23, 2.9, 3.6, 0.4, 2.2);
        }
        if (n.equals("dal") || n.contains("lentil")) {
            return new Macros(116, 9.0, 20.0, 0.4, 2.0);
        }
        if (n.contains("roti") || n.contains("chapati")) {
            return new Macros(297, 9.0, 46.0, 7.0, 4.0);
        }
        if (n.contains("paneer")) {
            return new Macros(265, 18.0, 2.0, 20.0, 0.0);
        }
        if (n.contains("peanut butter")) {
            return new Macros(588, 25.0, 20.0, 50.0, 8.0);
        }
        if (n.contains("butter") && !n.contains("peanut")) {
            return new Macros(717, 0.9, 0.1, 81.0, 0.0);
        }
        if (n.contains("egg")) {
            return new Macros(155, 13.0, 1.1, 11.0, 0.0);
        }
        if (CookingOilDetector.containsCookingOil(n)) {
            return new Macros(884, 0.0, 0.0, 100.0, 0.0);
        }
        if (n.contains("lassi") || n.contains("smoothie")) {
            return new Macros(83, 3.0, 14.0, 2.0, 0.5);
        }
        if (n.contains("raita") || n.contains("yogurt")) {
            return new Macros(59, 10.0, 3.6, 0.4, 0.0);
        }
        if (n.contains("biryani") || n.contains("curry") || n.contains("rice")) {
            return new Macros(180, 8.0, 22.0, 7.0, 1.5);
        }
        if (n.contains("thali")) {
            return new Macros(170, 6.0, 25.0, 5.0, 3.0);
        }
        if (n.contains("avocado")) {
            return new Macros(160, 2.0, 8.5, 14.7, 6.7);
        }
        if (n.contains("banana")) {
            return new Macros(89, 1.1, 23.0, 0.3, 2.6);
        }
        if (n.contains("apple")) {
            return new Macros(52, 0.3, 14.0, 0.2, 2.4);
        }
        if (n.contains("carbonara")) {
            return new Macros(280, 12.0, 28.0, 14.0, 2.0);
        }
        if (n.contains("oat milk") && (n.contains("latte") || n.contains("coffee") || n.contains("cappuccino"))) {
            return new Macros(65, 2.5, 8.0, 3.5, 0.5);
        }
        if (n.contains("wine")) {
            return new Macros(85, 0.1, 2.6, 0.0, 0.0);
        }
        if (NutritionValidator.isDietOrZeroCalBeverage(n)) {
            return new Macros(0, 0.0, 0.0, 0.0, 0.0);
        }
        if ((n.contains("coke") || n.contains("cola") || n.contains("soda")) && !n.contains("chocolate")) {
            return new Macros(42, 0.0, 10.6, 0.0, 0.0);
        }
        if (n.contains("cappuccino") || n.contains("latte") || n.contains("mocha")) {
            return new Macros(40, 2.0, 4.0, 2.0, 0.0);
        }
        if (n.contains("milk") && !n.contains("shake")) {
            return new Macros(42, 3.4, 5.0, 1.0, 0.0);
        }
        if (n.contains("cookie")) {
            return new Macros(420, 5.0, 55.0, 20.0, 2.0);
        }
        if (n.contains("muffin")) {
            return new Macros(380, 5.0, 50.0, 18.0, 2.0);
        }
        if (n.contains("burger") || n.contains("fries") || n.contains("fry")) {
            return n.contains("fries") || n.contains("fry")
                    ? new Macros(312, 3.4, 41.0, 15.0, 3.8)
                    : new Macros(250, 13.0, 25.0, 12.0, 1.0);
        }
        if (n.contains("snack")) {
            return new Macros(450, 6.0, 50.0, 25.0, 3.0);
        }
        if (n.contains("sandwich")) {
            return new Macros(250, 13.0, 25.0, 12.0, 2.0);
        }
        if (n.contains("mashed potato")) {
            return new Macros(106, 2.0, 17.0, 3.5, 1.5);
        }
        if (n.contains("green bean")) {
            return new Macros(35, 2.0, 8.0, 0.2, 3.0);
        }
        if (n.contains("cake") || n.contains("brownie") || n.contains("cupcake")) {
            return new Macros(389, 5.0, 53.0, 18.0, 2.0);
        }
        if (n.contains("ice cream")) {
            return new Macros(207, 3.5, 24.0, 11.0, 0.7);
        }
        if (n.contains("pizza")) {
            return new Macros(266, 11.0, 33.0, 10.0, 2.0);
        }
        if (n.contains("cheese") && !n.contains("cake")) {
            return new Macros(402, 25.0, 1.3, 33.0, 0.0);
        }
        if (n.contains("whey") || n.contains("protein powder")) {
            return new Macros(400, 80.0, 8.0, 5.0, 0.0);
        }
        if (n.contains("nut") || n.contains("almond")) {
            return new Macros(576, 21.0, 22.0, 49.0, 12.0);
        }
        if (n.contains("crisp") || n.contains("chip")) {
            return new Macros(536, 7.0, 53.0, 34.0, 4.0);
        }
        if (n.contains("cashew")) {
            return new Macros(553, 18.0, 30.0, 44.0, 3.3);
        }
        if (n.contains("chia")) {
            return new Macros(486, 16.5, 42.1, 30.7, 34.4);
        }
        if (n.contains("blueberr")) {
            return new Macros(57, 0.7, 14.5, 0.3, 2.4);
        }
        if (n.contains("rolled oats") || (n.contains("oat") && !n.contains("milk") && !n.contains("meal"))) {
            return new Macros(389, 16.9, 66.3, 6.9, 10.6);
        }
        if (n.contains("whole milk")) {
            return new Macros(61, 3.2, 4.8, 3.3, 0.0);
        }
        return null;
    }

    private record Macros(double calories, double protein, double carbs, double fat, double fiber) {}
}
