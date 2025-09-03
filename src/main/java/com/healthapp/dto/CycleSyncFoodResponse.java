package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI food recommendations based on menstrual cycle phase")
public class CycleSyncFoodResponse {
    
    @Schema(description = "Current menstrual cycle phase", example = "follicular")
    private String phase;
    
    @Schema(description = "Recommended foods for this phase")
    private List<String> recommendedFoods;
    
    @Schema(description = "Foods to avoid during this phase")
    private List<String> avoid;
    
    @Schema(description = "Reasoning for the recommendations", example = "In this high-energy phase, your body benefits from complex carbs and iron-rich foods.")
    private String reasoning;
    
    // Constructors
    public CycleSyncFoodResponse() {}
    
    public CycleSyncFoodResponse(String phase, List<String> recommendedFoods, List<String> avoid, String reasoning) {
        this.phase = phase;
        this.recommendedFoods = recommendedFoods;
        this.avoid = avoid;
        this.reasoning = reasoning;
    }
    
    // Getters and Setters
    public String getPhase() {
        return phase;
    }
    
    public void setPhase(String phase) {
        this.phase = phase;
    }
    
    public List<String> getRecommendedFoods() {
        return recommendedFoods;
    }
    
    public void setRecommendedFoods(List<String> recommendedFoods) {
        this.recommendedFoods = recommendedFoods;
    }
    
    public List<String> getAvoid() {
        return avoid;
    }
    
    public void setAvoid(List<String> avoid) {
        this.avoid = avoid;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}
