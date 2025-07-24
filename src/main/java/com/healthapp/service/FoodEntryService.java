package com.healthapp.service;

import com.healthapp.entity.FoodEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.FoodEntryRepository;
import com.healthapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FoodEntryService {
    
    @Autowired
    private FoodEntryRepository foodEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<FoodEntry> getAllFoodEntries() {
        return foodEntryRepository.findAll();
    }
    
    public Optional<FoodEntry> getFoodEntryById(Long id) {
        return foodEntryRepository.findById(id);
    }
    
    public List<FoodEntry> getFoodEntriesByUserId(Long userId) {
        return foodEntryRepository.findByUserId(userId);
    }
    
    public List<FoodEntry> getFoodEntriesByUserAndDate(Long userId, LocalDate date) {
        return foodEntryRepository.findByUserIdAndConsumptionDate(userId, date);
    }
    
    public FoodEntry createFoodEntry(FoodEntry foodEntry) {
        if (foodEntry.getConsumptionDate() == null) {
            foodEntry.setConsumptionDate(LocalDate.now());
        }
        return foodEntryRepository.save(foodEntry);
    }
    
    public FoodEntry updateFoodEntry(Long id, FoodEntry foodEntryDetails) {
        return foodEntryRepository.findById(id)
                .map(foodEntry -> {
                    foodEntry.setFoodName(foodEntryDetails.getFoodName());
                    foodEntry.setCalories(foodEntryDetails.getCalories());
                    foodEntry.setProteinG(foodEntryDetails.getProteinG());
                    foodEntry.setCarbsG(foodEntryDetails.getCarbsG());
                    foodEntry.setFatG(foodEntryDetails.getFatG());
                    foodEntry.setFiberG(foodEntryDetails.getFiberG());
                    foodEntry.setServingSize(foodEntryDetails.getServingSize());
                    foodEntry.setQuantity(foodEntryDetails.getQuantity());
                    foodEntry.setMealType(foodEntryDetails.getMealType());
                    foodEntry.setConsumptionDate(foodEntryDetails.getConsumptionDate());
                    foodEntry.setNotes(foodEntryDetails.getNotes());
                    return foodEntryRepository.save(foodEntry);
                })
                .orElseThrow(() -> new RuntimeException("Food entry not found"));
    }
    
    public void deleteFoodEntry(Long id) {
        if (!foodEntryRepository.existsById(id)) {
            throw new RuntimeException("Food entry not found");
        }
        foodEntryRepository.deleteById(id);
    }
    
    public Integer getTotalCaloriesByUserAndDate(Long userId, LocalDate date) {
        Integer totalCalories = foodEntryRepository.sumCaloriesByUserAndDate(userId, date);
        return totalCalories != null ? totalCalories : 0;
    }
    
    public Integer getTotalCaloriesByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Integer totalCalories = foodEntryRepository.sumCaloriesByUserAndDateRange(userId, startDate, endDate);
        return totalCalories != null ? totalCalories : 0;
    }
    
    public Integer getRemainingCalories(Long userId, LocalDate date) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        Integer dailyGoal = user.getDailyCalorieGoal();
        if (dailyGoal == null) {
            dailyGoal = 2000; // Default daily calorie goal
        }
        
        Integer consumedCalories = getTotalCaloriesByUserAndDate(userId, date);
        return dailyGoal - consumedCalories;
    }
} 