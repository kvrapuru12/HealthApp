package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoiceFoodLogResponse {
    
    private String message;
    private List<LoggedFoodItem> logs;
    /** Set on error responses for client routing and analytics; null on success. */
    private String errorCode;
    
    // Constructors
    public VoiceFoodLogResponse() {}
    
    public VoiceFoodLogResponse(String message, List<LoggedFoodItem> logs) {
        this.message = message;
        this.logs = logs;
    }

    /**
     * Error payload for voice food logging. {@code logs} is always an empty list.
     */
    public static VoiceFoodLogResponse error(String userMessage, String errorCode) {
        VoiceFoodLogResponse r = new VoiceFoodLogResponse(userMessage, List.of());
        r.setErrorCode(errorCode);
        return r;
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

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    // Inner class for logged food items
    public static class LoggedFoodItem {
        private String food;
        private Double quantity;
        private String mealType;
        /** True when this log was created from a compositeMeal (single dish from many ingredients). */
        private boolean compositeMeal;
        private Double calories;
        private Double protein;
        private Double carbs;
        private Double fat;
        private Double fiber;
        private String loggedAt;
        
        // Constructors
        public LoggedFoodItem() {}
        
        public LoggedFoodItem(String food, Double quantity, String mealType, boolean compositeMeal,
                             Double calories, Double protein, Double carbs, Double fat, Double fiber, String loggedAt) {
            this.food = food;
            this.quantity = quantity;
            this.mealType = mealType;
            this.compositeMeal = compositeMeal;
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.fiber = fiber;
            this.loggedAt = loggedAt;
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

        public boolean isCompositeMeal() {
            return compositeMeal;
        }

        public void setCompositeMeal(boolean compositeMeal) {
            this.compositeMeal = compositeMeal;
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
        
        public String getLoggedAt() {
            return loggedAt;
        }
        
        public void setLoggedAt(String loggedAt) {
            this.loggedAt = loggedAt;
        }
    }
}
