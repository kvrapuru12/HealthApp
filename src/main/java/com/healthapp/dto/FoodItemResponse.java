package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.healthapp.entity.FoodItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FoodItemResponse {
    
    @Schema(description = "Unique identifier of the food item")
    private Long id;
    
    @Schema(description = "Name of the food item")
    private String name;
    
    @Schema(description = "Category of the food item")
    private String category;
    
    @Schema(description = "Default unit for the food item")
    private String defaultUnit;
    
    @Schema(description = "Quantity per unit")
    private Double quantityPerUnit;
    
    @Schema(description = "Calories per unit")
    private Integer caloriesPerUnit;
    
    @Schema(description = "Protein per unit (grams)")
    private Double proteinPerUnit;
    
    @Schema(description = "Carbohydrates per unit (grams)")
    private Double carbsPerUnit;
    
    @Schema(description = "Fat per unit (grams)")
    private Double fatPerUnit;
    
    @Schema(description = "Fiber per unit (grams)")
    private Double fiberPerUnit;
    
    @Schema(description = "Visibility of the food item")
    private String visibility;
    
    @Schema(description = "Timestamp when the food item was created")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the food item was last updated")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
    
    // Constructors
    public FoodItemResponse() {}
    
    public FoodItemResponse(Long id, String name, String category, String defaultUnit, 
                          Double quantityPerUnit, Integer caloriesPerUnit, Double proteinPerUnit,
                          Double carbsPerUnit, Double fatPerUnit, Double fiberPerUnit,
                          String visibility, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.defaultUnit = defaultUnit;
        this.quantityPerUnit = quantityPerUnit;
        this.caloriesPerUnit = caloriesPerUnit;
        this.proteinPerUnit = proteinPerUnit;
        this.carbsPerUnit = carbsPerUnit;
        this.fatPerUnit = fatPerUnit;
        this.fiberPerUnit = fiberPerUnit;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public FoodItemResponse(FoodItem foodItem) {
        this.id = foodItem.getId();
        this.name = foodItem.getName();
        this.category = foodItem.getCategory();
        this.defaultUnit = foodItem.getDefaultUnit();
        this.quantityPerUnit = foodItem.getQuantityPerUnit();
        this.caloriesPerUnit = foodItem.getCaloriesPerUnit();
        this.proteinPerUnit = foodItem.getProteinPerUnit();
        this.carbsPerUnit = foodItem.getCarbsPerUnit();
        this.fatPerUnit = foodItem.getFatPerUnit();
        this.fiberPerUnit = foodItem.getFiberPerUnit();
        this.visibility = foodItem.getVisibility().name().toLowerCase();
        this.createdAt = foodItem.getCreatedAt();
        this.updatedAt = foodItem.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
