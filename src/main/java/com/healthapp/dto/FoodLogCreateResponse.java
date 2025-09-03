package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FoodLogCreateResponse {
    
    @Schema(description = "Unique identifier of the created food log")
    private Long id;
    
    @Schema(description = "Calculated calories")
    private Double calories;
    
    @Schema(description = "Calculated protein (grams)")
    private Double protein;
    
    @Schema(description = "Calculated carbohydrates (grams)")
    private Double carbs;
    
    @Schema(description = "Calculated fat (grams)")
    private Double fat;
    
    @Schema(description = "Calculated fiber (grams)")
    private Double fiber;
    
    @Schema(description = "Timestamp when the food log was created")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    // Constructors
    public FoodLogCreateResponse() {}
    
    public FoodLogCreateResponse(Long id, Double calories, Double protein, Double carbs, 
                                Double fat, Double fiber, LocalDateTime createdAt) {
        this.id = id;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Double getCalories() {
        return calories;
    }
    
    public void setCalories(Double calories) {
        this.calories = calories;
    }
    
    public Double getProtein() {
        return protein;
    }
    
    public void setProtein(Double protein) {
        this.protein = protein;
    }
    
    public Double getCarbs() {
        return carbs;
    }
    
    public void setCarbs(Double carbs) {
        this.carbs = carbs;
    }
    
    public Double getFat() {
        return fat;
    }
    
    public void setFat(Double fat) {
        this.fat = fat;
    }
    
    public Double getFiber() {
        return fiber;
    }
    
    public void setFiber(Double fiber) {
        this.fiber = fiber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
