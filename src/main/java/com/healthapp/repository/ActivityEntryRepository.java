package com.healthapp.repository;

import com.healthapp.entity.ActivityEntry;
import com.healthapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActivityEntryRepository extends JpaRepository<ActivityEntry, Long> {
    
    List<ActivityEntry> findByUser(User user);
    
    List<ActivityEntry> findByUserId(Long userId);
    
    List<ActivityEntry> findByUserAndActivityDate(User user, LocalDate activityDate);
    
    List<ActivityEntry> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);
    
    @Query("SELECT SUM(a.caloriesBurned) FROM ActivityEntry a WHERE a.user.id = :userId AND a.activityDate = :date")
    Integer sumCaloriesBurnedByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT SUM(a.caloriesBurned) FROM ActivityEntry a WHERE a.user.id = :userId AND a.activityDate BETWEEN :startDate AND :endDate")
    Integer sumCaloriesBurnedByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(a.durationMinutes) FROM ActivityEntry a WHERE a.user.id = :userId AND a.activityDate = :date")
    Integer sumDurationByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT SUM(a.steps) FROM ActivityEntry a WHERE a.user.id = :userId AND a.activityDate = :date")
    Integer sumStepsByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
} 