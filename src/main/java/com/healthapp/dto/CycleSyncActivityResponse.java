package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI activity recommendations based on menstrual cycle phase")
public class CycleSyncActivityResponse {
    
    @Schema(description = "Current menstrual cycle phase", example = "menstrual")
    private String phase;
    
    @Schema(description = "Recommended workouts for this phase")
    private List<String> recommendedWorkouts;
    
    @Schema(description = "Activities to avoid during this phase")
    private List<String> avoid;
    
    @Schema(description = "Additional notes", example = "Hormones are at their lowest. Light movement helps reduce cramps and bloating.")
    private String note;
    
    // Constructors
    public CycleSyncActivityResponse() {}
    
    public CycleSyncActivityResponse(String phase, List<String> recommendedWorkouts, List<String> avoid, String note) {
        this.phase = phase;
        this.recommendedWorkouts = recommendedWorkouts;
        this.avoid = avoid;
        this.note = note;
    }
    
    // Getters and Setters
    public String getPhase() {
        return phase;
    }
    
    public void setPhase(String phase) {
        this.phase = phase;
    }
    
    public List<String> getRecommendedWorkouts() {
        return recommendedWorkouts;
    }
    
    public void setRecommendedWorkouts(List<String> recommendedWorkouts) {
        this.recommendedWorkouts = recommendedWorkouts;
    }
    
    public List<String> getAvoid() {
        return avoid;
    }
    
    public void setAvoid(List<String> avoid) {
        this.avoid = avoid;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
}
