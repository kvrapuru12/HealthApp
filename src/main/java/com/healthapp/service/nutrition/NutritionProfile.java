package com.healthapp.service.nutrition;

public class NutritionProfile {

    private final double caloriesPer100g;
    private final double proteinPer100g;
    private final double carbsPer100g;
    private final double fatPer100g;
    private final double fiberPer100g;
    private final NutritionSource source;
    private final double confidence;
    private final Integer fdcId;

    public NutritionProfile(double caloriesPer100g, double proteinPer100g, double carbsPer100g,
                            double fatPer100g, double fiberPer100g, NutritionSource source,
                            double confidence, Integer fdcId) {
        this.caloriesPer100g = caloriesPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.fiberPer100g = fiberPer100g;
        this.source = source;
        this.confidence = confidence;
        this.fdcId = fdcId;
    }

    public double getCaloriesPer100g() {
        return caloriesPer100g;
    }

    public double getProteinPer100g() {
        return proteinPer100g;
    }

    public double getCarbsPer100g() {
        return carbsPer100g;
    }

    public double getFatPer100g() {
        return fatPer100g;
    }

    public double getFiberPer100g() {
        return fiberPer100g;
    }

    public NutritionSource getSource() {
        return source;
    }

    public double getConfidence() {
        return confidence;
    }

    public Integer getFdcId() {
        return fdcId;
    }

    public NutritionConfidence toConfidenceLevel() {
        if (source == NutritionSource.USDA && confidence >= 0.75) {
            return NutritionConfidence.HIGH;
        }
        if (source == NutritionSource.USDA || source == NutritionSource.LLM
                || source == NutritionSource.USER_STATED) {
            return NutritionConfidence.MEDIUM;
        }
        return NutritionConfidence.LOW;
    }
}
