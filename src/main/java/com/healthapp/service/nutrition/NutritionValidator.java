package com.healthapp.service.nutrition;

import com.healthapp.service.AiFoodVoiceParsingService;

public final class NutritionValidator {

    private NutritionValidator() {}

    public static boolean isLikelyBeverage(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return isLowCalBeverage(foodName) || isCaloricBeverage(foodName);
    }

    public static boolean isLowCalBeverage(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        if (n.contains("wine") || n.contains("beer") || n.contains("cocktail")
                || n.contains("lassi") || n.contains("smoothie") || n.contains("milkshake")
                || n.contains("latte") || n.contains("cappuccino") || n.contains("mocha")
                || n.contains("iced tea") || n.contains("chai")) {
            return false;
        }
        return n.contains("coffee") || n.contains("tea") || n.contains("water")
                || (n.contains("espresso") && !n.contains("latte") && !n.contains("cappuccino"));
    }

    public static boolean isCaloricBeverage(String foodName) {
        if (foodName == null) {
            return false;
        }
        if (isLikelyDessert(foodName)) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.contains("wine") || n.contains("beer") || n.contains("cocktail")
                || n.contains("lassi") || n.contains("smoothie") || n.contains("milkshake")
                || n.contains("latte") || n.contains("cappuccino") || n.contains("mocha")
                || n.contains("juice") || n.contains("soda") || n.contains("cola")
                || n.contains("coke") || n.contains("milk");
    }

    public static boolean isVaguePortion(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.equals("snacks") || n.equals("snack") || n.contains("some snack")
                || n.contains("afternoon snack") || n.equals("food");
    }

    public static boolean isLikelySpice(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.contains("salt") || n.contains("pepper") || n.contains("spice")
                || n.contains("seasoning") || n.contains("herb");
    }

    public static AiFoodVoiceParsingService.NutritionData validateNutritionData(
            String foodName, AiFoodVoiceParsingService.NutritionData nutrition, Double estimatedGrams) {
        if (nutrition == null) {
            return null;
        }
        boolean lowCalBeverage = isLowCalBeverage(foodName);
        boolean caloricBeverage = isCaloricBeverage(foodName);
        if (nutrition.getCaloriesPer100g() < 1 || nutrition.getProteinPer100g() < 0
                || nutrition.getCarbsPer100g() < 0 || nutrition.getFatPer100g() < 0) {
            return null;
        }
        if (!lowCalBeverage && !caloricBeverage && nutrition.getCaloriesPer100g() < 30) {
            return null;
        }
        if (lowCalBeverage && nutrition.getCaloriesPer100g() > 50) {
            return null;
        }
        if (caloricBeverage && nutrition.getCaloriesPer100g() > 120) {
            return null;
        }
        if (isLikelyFruit(foodName) && nutrition.getCaloriesPer100g() > 150) {
            return null;
        }
        if (isLikelyVegetable(foodName) && nutrition.getCaloriesPer100g() > 120) {
            return null;
        }
        if (isLikelyLeanProtein(foodName) && (nutrition.getCaloriesPer100g() < 120 || nutrition.getCaloriesPer100g() > 300)) {
            return null;
        }
        if (isLikelyEgg(foodName) && !isPlausibleEggNutrition(nutrition)) {
            return null;
        }
        if (isLikelyFruit(foodName) && nutrition.getProteinPer100g() > 2.0) {
            return null;
        }
        if (isLikelyDessert(foodName) && nutrition.getCaloriesPer100g() < 150) {
            return null;
        }
        if (!lowCalBeverage && !caloricBeverage) {
            double macroCalories = nutrition.getProteinPer100g() * 4
                    + nutrition.getCarbsPer100g() * 4
                    + nutrition.getFatPer100g() * 9;
            if (nutrition.getCaloriesPer100g() > 0) {
                double ratio = macroCalories / nutrition.getCaloriesPer100g();
                if (ratio < 0.6 || ratio > 1.4) {
                    return null;
                }
            }
        }
        if (estimatedGrams != null && estimatedGrams < 5 && !isLikelySpice(foodName)) {
            return null;
        }

        AiFoodVoiceParsingService.NutritionData validated = new AiFoodVoiceParsingService.NutritionData();
        validated.setCaloriesPer100g(clamp(nutrition.getCaloriesPer100g(), 1, 1000));
        validated.setProteinPer100g(clamp(nutrition.getProteinPer100g(), 0, 100));
        validated.setCarbsPer100g(clamp(nutrition.getCarbsPer100g(), 0, 100));
        validated.setFatPer100g(clamp(nutrition.getFatPer100g(), 0, 100));
        validated.setFiberPer100g(clamp(nutrition.getFiberPer100g(), 0, 50));
        return validated;
    }

    /**
     * Final persist check after nutrition resolution. Skips single-food heuristics (fruit protein caps,
     * lean-protein calorie bands, etc.) that misfire on composite meal names like "protein shake with banana".
     */
    public static AiFoodVoiceParsingService.NutritionData validateForPersist(
            String foodName, AiFoodVoiceParsingService.NutritionData nutrition, Double estimatedGrams) {
        if (nutrition == null) {
            return null;
        }
        if (nutrition.getCaloriesPer100g() < 1 || nutrition.getProteinPer100g() < 0
                || nutrition.getCarbsPer100g() < 0 || nutrition.getFatPer100g() < 0) {
            return null;
        }
        boolean beverage = isLikelyBeverage(foodName);
        if (!beverage && nutrition.getCaloriesPer100g() < 5 && !isLikelySpice(foodName)) {
            return null;
        }
        if (estimatedGrams != null && estimatedGrams < 5 && !isLikelySpice(foodName)) {
            return null;
        }
        AiFoodVoiceParsingService.NutritionData validated = new AiFoodVoiceParsingService.NutritionData();
        validated.setCaloriesPer100g(clamp(nutrition.getCaloriesPer100g(), 1, 1000));
        validated.setProteinPer100g(clamp(nutrition.getProteinPer100g(), 0, 100));
        validated.setCarbsPer100g(clamp(nutrition.getCarbsPer100g(), 0, 100));
        validated.setFatPer100g(clamp(nutrition.getFatPer100g(), 0, 100));
        validated.setFiberPer100g(clamp(nutrition.getFiberPer100g(), 0, 50));
        return validated;
    }

    public static boolean isMultiIngredientComposite(AiFoodVoiceParsingService.ParsedFoodData parsedData) {
        return parsedData != null
                && parsedData.getIngredients() != null
                && parsedData.getIngredients().size() >= 2;
    }

    /**
     * Basic sanity for blended composite meals — does not apply single-food heuristics
     * (vegetable calorie caps, lean-protein bands, etc.) that misfire on multi-item names.
     */
    public static boolean hasImplausibleCompositeNutrition(
            AiFoodVoiceParsingService.NutritionData nutrition) {
        if (nutrition == null) {
            return true;
        }
        if (nutrition.getCaloriesPer100g() < 30) {
            return true;
        }
        double macroCalories = nutrition.getProteinPer100g() * 4
                + nutrition.getCarbsPer100g() * 4
                + nutrition.getFatPer100g() * 9;
        if (nutrition.getCaloriesPer100g() > 0) {
            double ratio = macroCalories / nutrition.getCaloriesPer100g();
            if (ratio < 0.5 || ratio > 1.5) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasImplausibleStoredNutrition(String foodName, Integer caloriesPer100g) {
        if (caloriesPer100g == null) {
            return true;
        }
        if (isLikelyDessert(foodName) && caloriesPer100g < 200) {
            return true;
        }
        if (isLowCalBeverage(foodName)) {
            return caloriesPer100g > 50;
        }
        if (isCaloricBeverage(foodName)) {
            return caloriesPer100g > 120;
        }
        if (caloriesPer100g < 30) {
            return true;
        }
        if (isLikelyFruit(foodName) && caloriesPer100g > 150) {
            return true;
        }
        if (isLikelyVegetable(foodName) && caloriesPer100g > 120) {
            return true;
        }
        if (isLikelyLeanProtein(foodName) && (caloriesPer100g < 120 || caloriesPer100g > 300)) {
            return true;
        }
        if (isLikelyEgg(foodName) && (caloriesPer100g < 120 || caloriesPer100g > 200)) {
            return true;
        }
        return false;
    }

    public static boolean isLikelyEgg(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.contains("egg");
    }

    private static boolean isPlausibleEggNutrition(AiFoodVoiceParsingService.NutritionData nutrition) {
        if (nutrition.getCaloriesPer100g() < 120 || nutrition.getCaloriesPer100g() > 200) {
            return false;
        }
        if (nutrition.getProteinPer100g() < 5) {
            return false;
        }
        if (nutrition.getFatPer100g() > 20 && nutrition.getProteinPer100g() < 5) {
            return false;
        }
        return true;
    }

    public static boolean isLikelyDessert(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.contains("cake") || n.contains("ice cream") || n.contains("cookie")
                || n.contains("brownie") || n.contains("pie") || n.contains("donut")
                || n.contains("doughnut") || n.contains("pastry") || n.contains("cupcake");
    }

    public static boolean isLikelyFruit(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        if (n.contains("muffin") || n.contains("cake") || n.contains("pie")
                || n.contains("pastry") || n.contains("bread") || n.contains("smoothie")
                || n.contains("yogurt") || n.contains("lassi") || n.contains("shake")) {
            return false;
        }
        return n.contains("banana") || n.contains("apple") || n.contains("orange")
                || n.contains("berry") || n.contains("grape") || n.contains("mango")
                || n.contains("melon") || n.contains("peach") || n.contains("pear");
    }

    public static boolean isLikelyVegetable(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.contains("broccoli") || n.contains("spinach") || n.contains("carrot")
                || n.contains("lettuce") || n.contains("cucumber") || n.contains("celery")
                || n.contains("kale") || n.contains("zucchini");
    }

    public static boolean isLikelyLeanProtein(String foodName) {
        if (foodName == null) {
            return false;
        }
        String n = foodName.toLowerCase();
        return n.contains("chicken") || n.contains("turkey") || n.contains("salmon")
                || n.contains("tuna") || n.contains("cod") || n.contains("shrimp")
                || n.contains("breast") || n.contains("fish");
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
