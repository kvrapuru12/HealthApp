package com.healthapp.dto;

import java.time.LocalDateTime;

public class ParsedFoodItem {
    
    private String foodName;
    private Double quantity;
    private String unit;
    private String mealType;
    private LocalDateTime loggedAt;
    
    // Constructors
    public ParsedFoodItem() {}
    
    public ParsedFoodItem(String foodName, Double quantity, String unit, String mealType, LocalDateTime loggedAt) {
        this.foodName = foodName;
        this.quantity = quantity;
        this.unit = unit;
        this.mealType = mealType;
        this.loggedAt = loggedAt;
    }
    
    // Getters and Setters
    public String getFoodName() {
        return foodName;
    }
    
    public void setFoodName(String foodName) {
        this.foodName = foodName;
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
    
    public String getMealType() {
        return mealType;
    }
    
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
    
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
}
