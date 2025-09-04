package com.healthapp.repository;

import com.healthapp.entity.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;



@Repository
public interface MoodEntryRepository extends JpaRepository<MoodEntry, Long> {
    
    // Find all mood entries for a specific user
    List<MoodEntry> findByUserId(Long userId);
    
    // Check for duplicate entries within ±5 minutes for the same user
    @Query("SELECT COUNT(m) > 0 FROM MoodEntry m WHERE m.user.id = :userId AND m.loggedAt BETWEEN :startTime AND :endTime")
    boolean existsByUserIdAndTimeRange(@Param("userId") Long userId, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    

}
