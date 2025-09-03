package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.FoodLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FoodLogResponse {
    
    @Schema(description = "Unique identifier of the food log")
    private Long id;
    
    @Schema(description = "User ID")
    private Long userId;
    
    @Schema(description = "Food item ID")
    private Long foodItemId;
    
    @Schema(description = "Name of the food item")
    private String foodItemName;
    
    @Schema(description = "When the food was logged")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime loggedAt;
    
    @Schema(description = "Type of meal")
    private String mealType;
    
    @Schema(description = "Quantity consumed")
    private Double quantity;
    
    @Schema(description = "Unit of measurement")
    private String unit;
    
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
    
    @Schema(description = "Additional notes")
    private String note;
    
    @Schema(description = "Timestamp when the food log was created")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the food log was last updated")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
    
    // Constructors
    public FoodLogResponse() {}
    
    public FoodLogResponse(Long id, Long userId, Long foodItemId, String foodItemName,
                          LocalDateTime loggedAt, String mealType, Double quantity, String unit,
                          Double calories, Double protein, Double carbs, Double fat, Double fiber,
                          String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.foodItemId = foodItemId;
        this.foodItemName = foodItemName;
        this.loggedAt = loggedAt;
        this.mealType = mealType;
        this.quantity = quantity;
        this.unit = unit;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public FoodLogResponse(FoodLog foodLog, String foodItemName) {
        this.id = foodLog.getId();
        this.userId = foodLog.getUserId();
        this.foodItemId = foodLog.getFoodItemId();
        this.foodItemName = foodItemName;
        this.loggedAt = foodLog.getLoggedAt();
        this.mealType = foodLog.getMealType() != null ? foodLog.getMealType().name().toLowerCase() : null;
        this.quantity = foodLog.getQuantity();
        this.unit = foodLog.getUnit();
        this.calories = foodLog.getCalories();
        this.protein = foodLog.getProtein();
        this.carbs = foodLog.getCarbs();
        this.fat = foodLog.getFat();
        this.fiber = foodLog.getFiber();
        this.note = foodLog.getNote();
        this.createdAt = foodLog.getCreatedAt();
        this.updatedAt = foodLog.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getFoodItemId() {
        return foodItemId;
    }
    
    public void setFoodItemId(Long foodItemId) {
        this.foodItemId = foodItemId;
    }
    
    public String getFoodItemName() {
        return foodItemName;
    }
    
    public void setFoodItemName(String foodItemName) {
        this.foodItemName = foodItemName;
    }
    
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
    
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
    
    public String getMealType() {
        return mealType;
    }
    
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    
    public Double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
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
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
