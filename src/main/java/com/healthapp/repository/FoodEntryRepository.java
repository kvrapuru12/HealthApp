package com.healthapp.repository;

import com.healthapp.entity.FoodEntry;
import com.healthapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, Long> {
    
    List<FoodEntry> findByUser(User user);
    
    List<FoodEntry> findByUserId(Long userId);
    
    List<FoodEntry> findByUserAndConsumptionDate(User user, LocalDate consumptionDate);
    
    List<FoodEntry> findByUserIdAndConsumptionDate(Long userId, LocalDate consumptionDate);
    
    @Query("SELECT SUM(f.calories) FROM FoodEntry f WHERE f.user.id = :userId AND f.consumptionDate = :date")
    Integer sumCaloriesByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT SUM(f.calories) FROM FoodEntry f WHERE f.user.id = :userId AND f.consumptionDate BETWEEN :startDate AND :endDate")
    Integer sumCaloriesByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
} 