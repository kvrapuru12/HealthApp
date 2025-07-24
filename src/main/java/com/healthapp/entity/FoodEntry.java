package com.healthapp.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_entries")
@EntityListeners(AuditingEntityListener.class)
public class FoodEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "food_name", nullable = false)
    private String foodName;
    
    @Column(name = "calories")
    private Integer calories;
    
    @Column(name = "protein_g")
    private Double proteinG;
    
    @Column(name = "carbs_g")
    private Double carbsG;
    
    @Column(name = "fat_g")
    private Double fatG;
    
    @Column(name = "fiber_g")
    private Double fiberG;
    
    @Column(name = "serving_size")
    private String servingSize;
    
    @Column(name = "quantity")
    private Double quantity;
    
    @Column(name = "meal_type")
    @Enumerated(EnumType.STRING)
    private MealType mealType;
    
    @Column(name = "consumption_date")
    private LocalDate consumptionDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public FoodEntry() {}
    
    public FoodEntry(User user, String foodName, Integer calories) {
        this.user = user;
        this.foodName = foodName;
        this.calories = calories;
        this.consumptionDate = LocalDate.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getFoodName() {
        return foodName;
    }
    
    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }
    
    public Integer getCalories() {
        return calories;
    }
    
    public void setCalories(Integer calories) {
        this.calories = calories;
    }
    
    public Double getProteinG() {
        return proteinG;
    }
    
    public void setProteinG(Double proteinG) {
        this.proteinG = proteinG;
    }
    
    public Double getCarbsG() {
        return carbsG;
    }
    
    public void setCarbsG(Double carbsG) {
        this.carbsG = carbsG;
    }
    
    public Double getFatG() {
        return fatG;
    }
    
    public void setFatG(Double fatG) {
        this.fatG = fatG;
    }
    
    public Double getFiberG() {
        return fiberG;
    }
    
    public void setFiberG(Double fiberG) {
        this.fiberG = fiberG;
    }
    
    public String getServingSize() {
        return servingSize;
    }
    
    public void setServingSize(String servingSize) {
        this.servingSize = servingSize;
    }
    
    public Double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    
    public MealType getMealType() {
        return mealType;
    }
    
    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }
    
    public LocalDate getConsumptionDate() {
        return consumptionDate;
    }
    
    public void setConsumptionDate(LocalDate consumptionDate) {
        this.consumptionDate = consumptionDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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
    
    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }
} 