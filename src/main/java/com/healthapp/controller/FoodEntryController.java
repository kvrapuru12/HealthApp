package com.healthapp.controller;

import com.healthapp.entity.FoodEntry;
import com.healthapp.service.FoodEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/food-entries")
@Tag(name = "Food Entry Management", description = "APIs for managing food entries and calorie tracking")
@CrossOrigin(origins = "*")
public class FoodEntryController {
    
    @Autowired
    private FoodEntryService foodEntryService;
    
    @GetMapping
    @Operation(summary = "Get all food entries", description = "Retrieve a list of all food entries")
    public ResponseEntity<List<FoodEntry>> getAllFoodEntries() {
        List<FoodEntry> foodEntries = foodEntryService.getAllFoodEntries();
        return ResponseEntity.ok(foodEntries);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get food entry by ID", description = "Retrieve a specific food entry by its ID")
    public ResponseEntity<FoodEntry> getFoodEntryById(@PathVariable Long id) {
        return foodEntryService.getFoodEntryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get food entries by user", description = "Retrieve all food entries for a specific user")
    public ResponseEntity<List<FoodEntry>> getFoodEntriesByUserId(@PathVariable Long userId) {
        List<FoodEntry> foodEntries = foodEntryService.getFoodEntriesByUserId(userId);
        return ResponseEntity.ok(foodEntries);
    }
    
    @GetMapping("/user/{userId}/date/{date}")
    @Operation(summary = "Get food entries by user and date", description = "Retrieve food entries for a specific user on a specific date")
    public ResponseEntity<List<FoodEntry>> getFoodEntriesByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<FoodEntry> foodEntries = foodEntryService.getFoodEntriesByUserAndDate(userId, date);
        return ResponseEntity.ok(foodEntries);
    }
    
    @PostMapping
    @Operation(summary = "Create a new food entry", description = "Create a new food entry for calorie tracking")
    public ResponseEntity<FoodEntry> createFoodEntry(@RequestBody FoodEntry foodEntry) {
        try {
            FoodEntry savedFoodEntry = foodEntryService.createFoodEntry(foodEntry);
            return ResponseEntity.ok(savedFoodEntry);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update food entry", description = "Update an existing food entry")
    public ResponseEntity<FoodEntry> updateFoodEntry(@PathVariable Long id, @RequestBody FoodEntry foodEntryDetails) {
        try {
            FoodEntry updatedFoodEntry = foodEntryService.updateFoodEntry(id, foodEntryDetails);
            return ResponseEntity.ok(updatedFoodEntry);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete food entry", description = "Delete a food entry")
    public ResponseEntity<?> deleteFoodEntry(@PathVariable Long id) {
        try {
            foodEntryService.deleteFoodEntry(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}/calories/{date}")
    @Operation(summary = "Get total calories by user and date", description = "Get total calories consumed by a user on a specific date")
    public ResponseEntity<Integer> getTotalCaloriesByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Integer totalCalories = foodEntryService.getTotalCaloriesByUserAndDate(userId, date);
        return ResponseEntity.ok(totalCalories);
    }
    
    @GetMapping("/user/{userId}/remaining-calories/{date}")
    @Operation(summary = "Get remaining calories", description = "Get remaining calories for a user on a specific date based on their daily goal")
    public ResponseEntity<Integer> getRemainingCalories(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Integer remainingCalories = foodEntryService.getRemainingCalories(userId, date);
            return ResponseEntity.ok(remainingCalories);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 