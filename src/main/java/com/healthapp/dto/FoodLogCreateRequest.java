package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class FoodLogCreateRequest {
    
    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    private Long userId;
    
    @NotNull(message = "Food item ID is required")
    @Min(value = 1, message = "Food item ID must be positive")
    private Long foodItemId;
    
    @NotNull(message = "Logged at time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime loggedAt;
    
    private String mealType;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "1.0", message = "Quantity must be at least 1")
    @DecimalMax(value = "2000.0", message = "Quantity cannot exceed 2000")
    private Double quantity;
    
    @Size(max = 20, message = "Unit cannot exceed 20 characters")
    private String unit;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    private String note;
    
    // Constructors
    public FoodLogCreateRequest() {}
    
    public FoodLogCreateRequest(Long userId, Long foodItemId, LocalDateTime loggedAt, Double quantity) {
        this.userId = userId;
        this.foodItemId = foodItemId;
        this.loggedAt = loggedAt;
        this.quantity = quantity;
    }
    
    // Getters and Setters
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
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
}
