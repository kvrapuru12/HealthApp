package com.healthapp.repository;

import com.healthapp.entity.FoodLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {
    
    // Find by ID and status
    Optional<FoodLog> findByIdAndStatus(Long id, FoodLog.FoodLogStatus status);
    
    // Find by user ID and status
    List<FoodLog> findByUserIdAndStatus(Long userId, FoodLog.FoodLogStatus status);
    
    // Find by user ID and meal type
    List<FoodLog> findByUserIdAndMealTypeAndStatus(Long userId, FoodLog.MealType mealType, FoodLog.FoodLogStatus status);
    
    // Find by user ID and date range
    @Query("SELECT f FROM FoodLog f WHERE f.userId = :userId AND f.status = :status AND " +
           "f.loggedAt BETWEEN :fromDate AND :toDate")
    List<FoodLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") FoodLog.FoodLogStatus status);
    
    // Find by user ID with filters and pagination
    @Query("SELECT f FROM FoodLog f WHERE f.userId = :userId AND f.status = :status AND " +
           "(:fromDate IS NULL OR f.loggedAt >= :fromDate) AND " +
           "(:toDate IS NULL OR f.loggedAt <= :toDate) AND " +
           "(:mealType IS NULL OR f.mealType = :mealType)")
    Page<FoodLog> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("mealType") FoodLog.MealType mealType,
            @Param("status") FoodLog.FoodLogStatus status,
            Pageable pageable);
    
    // Get daily totals for a user
    @Query("SELECT SUM(f.calories) as totalCalories, " +
           "SUM(f.protein) as totalProtein, " +
           "SUM(f.carbs) as totalCarbs, " +
           "SUM(f.fat) as totalFat, " +
           "SUM(f.fiber) as totalFiber " +
           "FROM FoodLog f WHERE f.userId = :userId AND f.status = 'ACTIVE' AND " +
           "DATE(f.loggedAt) = DATE(:date)")
    Object[] getDailyTotals(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    // Get food logs for a specific food item
    List<FoodLog> findByFoodItemIdAndStatus(Long foodItemId, FoodLog.FoodLogStatus status);
    
    // Count food logs by user and date
    @Query("SELECT COUNT(f) FROM FoodLog f WHERE f.userId = :userId AND f.status = 'ACTIVE' AND " +
           "DATE(f.loggedAt) = DATE(:date)")
    long countByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDateTime date);
}
