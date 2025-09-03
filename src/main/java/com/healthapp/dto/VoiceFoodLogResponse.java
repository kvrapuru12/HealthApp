package com.healthapp.dto;

import java.util.List;

public class VoiceFoodLogResponse {
    
    private String message;
    private List<LoggedFoodItem> logs;
    
    // Constructors
    public VoiceFoodLogResponse() {}
    
    public VoiceFoodLogResponse(String message, List<LoggedFoodItem> logs) {
        this.message = message;
        this.logs = logs;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<LoggedFoodItem> getLogs() {
        return logs;
    }
    
    public void setLogs(List<LoggedFoodItem> logs) {
        this.logs = logs;
    }
    
    // Inner class for logged food items
    public static class LoggedFoodItem {
        private String food;
        private Double quantity;
        private String mealType;
        private Double calories;
        private Double protein;
        private Double carbs;
        private Double fat;
        private Double fiber;
        
        // Constructors
        public LoggedFoodItem() {}
        
        public LoggedFoodItem(String food, Double quantity, String mealType, 
                             Double calories, Double protein, Double carbs, Double fat, Double fiber) {
            this.food = food;
            this.quantity = quantity;
            this.mealType = mealType;
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.fiber = fiber;
        }
        
        // Getters and Setters
        public String getFood() {
            return food;
        }
        
        public void setFood(String food) {
            this.food = food;
        }
        
        public Double getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }
        
        public String getMealType() {
            return mealType;
        }
        
        public void setMealType(String mealType) {
            this.mealType = mealType;
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
    }
}
