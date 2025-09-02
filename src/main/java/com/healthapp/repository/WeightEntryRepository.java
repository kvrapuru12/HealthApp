package com.healthapp.repository;

import com.healthapp.entity.WeightEntry;
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
public interface WeightEntryRepository extends JpaRepository<WeightEntry, Long> {
    
    // Find all weight entries for a specific user
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.status = :status")
    List<WeightEntry> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") WeightEntry.Status status);
    
    // Find weight entries for a specific user within a date range
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    List<WeightEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status
    );
    
    // Find weight entries for a specific user within a date range (paginated)
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Page<WeightEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status,
            Pageable pageable
    );
    
    // Find all weight entries within a date range (admin only)
    @Query("SELECT w FROM WeightEntry w WHERE w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Page<WeightEntry> findByDateRangeAndStatus(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status,
            Pageable pageable
    );
    
    // Check for duplicate entries within a time range (for validation)
    @Query("SELECT COUNT(w) > 0 FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :startTime AND :endTime AND w.status = :status")
    boolean existsByUserIdAndTimeRangeAndStatus(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") WeightEntry.Status status
    );
    
    // Count total weight entries for a user within a date range
    @Query("SELECT COUNT(w) FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Long countByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status
    );
    
    // Find the most recent weight entry for a user
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.status = :status ORDER BY w.loggedAt DESC")
    List<WeightEntry> findMostRecentByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") WeightEntry.Status status,
            Pageable pageable
    );
    
    // Find by ID and status (for soft delete validation)
    Optional<WeightEntry> findByIdAndStatus(Long id, WeightEntry.Status status);
    
    // Find by ID and user ID (for access control)
    @Query("SELECT w FROM WeightEntry w WHERE w.id = :id AND w.user.id = :userId AND w.status = :status")
    Optional<WeightEntry> findByIdAndUserIdAndStatus(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") WeightEntry.Status status
    );
}
