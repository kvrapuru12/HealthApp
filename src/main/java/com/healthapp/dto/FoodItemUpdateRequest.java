package com.healthapp.dto;


import jakarta.validation.constraints.*;

public class FoodItemUpdateRequest {
    
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;
    
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;
    
    @Size(max = 20, message = "Default unit cannot exceed 20 characters")
    private String defaultUnit;
    
    @DecimalMin(value = "1.0", message = "Quantity per unit must be at least 1")
    @DecimalMax(value = "1000.0", message = "Quantity per unit cannot exceed 1000")
    private Double quantityPerUnit;
    
    @Min(value = 1, message = "Calories per unit must be at least 1")
    @Max(value = 2000, message = "Calories per unit cannot exceed 2000")
    private Integer caloriesPerUnit;
    
    @DecimalMin(value = "0.0", message = "Protein per unit cannot be negative")
    @DecimalMax(value = "100.0", message = "Protein per unit cannot exceed 100g")
    private Double proteinPerUnit;
    
    @DecimalMin(value = "0.0", message = "Carbs per unit cannot be negative")
    @DecimalMax(value = "100.0", message = "Carbs per unit cannot exceed 100g")
    private Double carbsPerUnit;
    
    @DecimalMin(value = "0.0", message = "Fat per unit cannot be negative")
    @DecimalMax(value = "100.0", message = "Fat per unit cannot exceed 100g")
    private Double fatPerUnit;
    
    @DecimalMin(value = "0.0", message = "Fiber per unit cannot be negative")
    @DecimalMax(value = "50.0", message = "Fiber per unit cannot exceed 50g")
    private Double fiberPerUnit;
    
    private String visibility;
    
    // Constructors
    public FoodItemUpdateRequest() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDefaultUnit() {
        return defaultUnit;
    }
    
    public void setDefaultUnit(String defaultUnit) {
        this.defaultUnit = defaultUnit;
    }
    
    public Double getQuantityPerUnit() {
        return quantityPerUnit;
    }
    
    public void setQuantityPerUnit(Double quantityPerUnit) {
        this.quantityPerUnit = quantityPerUnit;
    }
    
    public Integer getCaloriesPerUnit() {
        return caloriesPerUnit;
    }
    
    public void setCaloriesPerUnit(Integer caloriesPerUnit) {
        this.caloriesPerUnit = caloriesPerUnit;
    }
    
    public Double getProteinPerUnit() {
        return proteinPerUnit;
    }
    
    public void setProteinPerUnit(Double proteinPerUnit) {
        this.proteinPerUnit = proteinPerUnit;
    }
    
    public Double getCarbsPerUnit() {
        return carbsPerUnit;
    }
    
    public void setCarbsPerUnit(Double carbsPerUnit) {
        this.carbsPerUnit = carbsPerUnit;
    }
    
    public Double getFatPerUnit() {
        return fatPerUnit;
    }
    
    public void setFatPerUnit(Double fatPerUnit) {
        this.fatPerUnit = fatPerUnit;
    }
    
    public Double getFiberPerUnit() {
        return fiberPerUnit;
    }
    
    public void setFiberPerUnit(Double fiberPerUnit) {
        this.fiberPerUnit = fiberPerUnit;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
