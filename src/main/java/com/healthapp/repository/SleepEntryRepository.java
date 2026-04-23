package com.healthapp.repository;

import com.healthapp.entity.SleepEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepEntryRepository extends JpaRepository<SleepEntry, Long> {
    
    // Find all sleep entries for a specific user
    @Query("SELECT s FROM SleepEntry s WHERE s.user.id = :userId")
    List<SleepEntry> findByUserId(@Param("userId") Long userId);
    
    // Find all active sleep entries for a specific user
    @Query("SELECT s FROM SleepEntry s WHERE s.user.id = :userId AND s.status = :status")
    List<SleepEntry> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SleepEntry.Status status);
    
    // Find sleep entries for a specific user within a date range
    @Query("SELECT s FROM SleepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    List<SleepEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") SleepEntry.Status status
    );
    
    // Find sleep entries for a specific user within a date range (paginated)
    @Query("SELECT s FROM SleepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Page<SleepEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") SleepEntry.Status status,
            Pageable pageable
    );
    
    // Find all sleep entries within a date range (admin only)
    @Query("SELECT s FROM SleepEntry s WHERE s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Page<SleepEntry> findByDateRangeAndStatus(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") SleepEntry.Status status,
            Pageable pageable
    );
    
    // Check for duplicate entries with same hours within ±5 minutes for the same user
    @Query("SELECT COUNT(s) > 0 FROM SleepEntry s WHERE s.user.id = :userId AND s.hours = :hours AND s.loggedAt BETWEEN :startTime AND :endTime AND s.status = :status")
    boolean existsByUserIdAndHoursAndTimeRangeAndStatus(
            @Param("userId") Long userId, 
            @Param("hours") BigDecimal hours,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime,
            @Param("status") SleepEntry.Status status
    );
    
    // Count total entries for a user within a date range
    @Query("SELECT COUNT(s) FROM SleepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Long countByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") SleepEntry.Status status
    );

    /** Half-open interval [fromInclusive, toExclusive) for calendar-day totals with UTC-stored timestamps */
    @Query("SELECT SUM(s.hours) FROM SleepEntry s WHERE s.user.id = :userId AND s.loggedAt >= :fromInclusive AND s.loggedAt < :toExclusive AND s.status = :status")
    Optional<BigDecimal> sumHoursByUserIdAndDateRangeHalfOpen(
            @Param("userId") Long userId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("status") SleepEntry.Status status
    );
}
