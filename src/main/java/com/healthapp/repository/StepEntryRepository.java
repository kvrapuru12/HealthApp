package com.healthapp.repository;

import com.healthapp.entity.StepEntry;
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
public interface StepEntryRepository extends JpaRepository<StepEntry, Long> {
    
    // Find all step entries for a specific user
    @Query("SELECT s FROM StepEntry s WHERE s.user.id = :userId AND s.status = :status")
    List<StepEntry> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") StepEntry.Status status);
    
    // Find step entries for a specific user within a date range
    @Query("SELECT s FROM StepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    List<StepEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") StepEntry.Status status
    );
    
    // Find step entries for a specific user within a date range (paginated)
    @Query("SELECT s FROM StepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Page<StepEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") StepEntry.Status status,
            Pageable pageable
    );
    
    // Find all step entries within a date range (admin only)
    @Query("SELECT s FROM StepEntry s WHERE s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Page<StepEntry> findByDateRangeAndStatus(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") StepEntry.Status status,
            Pageable pageable
    );
    
    // Check for duplicate entries within a time range (for validation)
    @Query("SELECT COUNT(s) > 0 FROM StepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :startTime AND :endTime AND s.status = :status")
    boolean existsByUserIdAndTimeRangeAndStatus(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") StepEntry.Status status
    );
    
    // Count total steps for a user within a date range
    @Query("SELECT COUNT(s) FROM StepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Long countByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") StepEntry.Status status
    );
    
    // Sum total steps for a user within a date range
    @Query("SELECT COALESCE(SUM(s.stepCount), 0) FROM StepEntry s WHERE s.user.id = :userId AND s.loggedAt BETWEEN :fromDate AND :toDate AND s.status = :status")
    Integer sumStepCountByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") StepEntry.Status status
    );
    
    // Find by ID and status (for soft delete validation)
    Optional<StepEntry> findByIdAndStatus(Long id, StepEntry.Status status);
}
