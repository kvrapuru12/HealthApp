package com.healthapp.repository;

import com.healthapp.entity.WaterEntry;
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
public interface WaterEntryRepository extends JpaRepository<WaterEntry, Long> {
    
    // Find all water entries for a specific user
    @Query("SELECT w FROM WaterEntry w WHERE w.user.id = :userId AND w.status = :status")
    List<WaterEntry> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") WaterEntry.Status status);
    
    // Find water entries for a specific user within a date range
    @Query("SELECT w FROM WaterEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    List<WaterEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WaterEntry.Status status
    );
    
    // Find water entries for a specific user within a date range (paginated)
    @Query("SELECT w FROM WaterEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Page<WaterEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WaterEntry.Status status,
            Pageable pageable
    );
    
    // Find all water entries within a date range (admin only)
    @Query("SELECT w FROM WaterEntry w WHERE w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Page<WaterEntry> findByDateRangeAndStatus(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WaterEntry.Status status,
            Pageable pageable
    );
    
    // Check for duplicate entries within a time range (for validation)
    @Query("SELECT COUNT(w) > 0 FROM WaterEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :startTime AND :endTime AND w.status = :status")
    boolean existsByUserIdAndTimeRangeAndStatus(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") WaterEntry.Status status
    );
    
    // Count total water entries for a user within a date range
    @Query("SELECT COUNT(w) FROM WaterEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Long countByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WaterEntry.Status status
    );
    
    // Sum total water consumption for a user within a date range
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WaterEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Integer sumAmountByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WaterEntry.Status status
    );
    
    // Find by ID and status (for soft delete validation)
    Optional<WaterEntry> findByIdAndStatus(Long id, WaterEntry.Status status);
}
