package com.healthapp.dto;

import com.healthapp.entity.Activity;
import jakarta.validation.constraints.*;

public class ActivityUpdateRequest {
    
    @Size(min = 3, max = 100, message = "Activity name must be between 3 and 100 characters")
    private String name;
    
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;
    
    @DecimalMin(value = "0.1", message = "Calories per minute must be at least 0.1")
    @DecimalMax(value = "50.0", message = "Calories per minute cannot exceed 50.0")
    private java.math.BigDecimal caloriesPerMinute;
    
    private Activity.Visibility visibility;
    
    // Constructors
    public ActivityUpdateRequest() {}
    
    public ActivityUpdateRequest(String name, String category, java.math.BigDecimal caloriesPerMinute, Activity.Visibility visibility) {
        this.name = name;
        this.category = category;
        this.caloriesPerMinute = caloriesPerMinute;
        this.visibility = visibility;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public java.math.BigDecimal getCaloriesPerMinute() { return caloriesPerMinute; }
    public void setCaloriesPerMinute(java.math.BigDecimal caloriesPerMinute) { this.caloriesPerMinute = caloriesPerMinute; }
    
    public Activity.Visibility getVisibility() { return visibility; }
    public void setVisibility(Activity.Visibility visibility) { this.visibility = visibility; }
}
