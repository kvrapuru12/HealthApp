package com.healthapp.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_items")
@EntityListeners(AuditingEntityListener.class)
public class FoodItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 50)
    private String category;
    
    @Column(name = "default_unit", length = 20)
    private String defaultUnit = "grams";
    
    @Column(name = "quantity_per_unit")
    private Double quantityPerUnit = 100.0;
    
    @Column(name = "weight_per_unit")
    private Double weightPerUnit = 100.0;
    
    @Column(name = "calories_per_unit", nullable = false)
    private Integer caloriesPerUnit;
    
    @Column(name = "protein_per_unit")
    private Double proteinPerUnit;
    
    @Column(name = "carbs_per_unit")
    private Double carbsPerUnit;
    
    @Column(name = "fat_per_unit")
    private Double fatPerUnit;
    
    @Column(name = "fiber_per_unit")
    private Double fiberPerUnit;
    
    @Enumerated(EnumType.STRING)
    private FoodVisibility visibility = FoodVisibility.PRIVATE;
    
    @Enumerated(EnumType.STRING)
    private FoodStatus status = FoodStatus.ACTIVE;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public FoodItem() {}
    
    public FoodItem(String name, Integer caloriesPerUnit, Long createdBy) {
        this.name = name;
        this.caloriesPerUnit = caloriesPerUnit;
        this.createdBy = createdBy;
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
    
    public Double getWeightPerUnit() {
        return weightPerUnit;
    }
    
    public void setWeightPerUnit(Double weightPerUnit) {
        this.weightPerUnit = weightPerUnit;
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
    
    public FoodVisibility getVisibility() {
        return visibility;
    }
    
    public void setVisibility(FoodVisibility visibility) {
        this.visibility = visibility;
    }
    
    public FoodStatus getStatus() {
        return status;
    }
    
    public void setStatus(FoodStatus status) {
        this.status = status;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
    
    public enum FoodVisibility {
        PRIVATE, PUBLIC
    }
    
    public enum FoodStatus {
        ACTIVE, DELETED
    }
}
