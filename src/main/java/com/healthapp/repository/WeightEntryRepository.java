package com.healthapp.repository;

import com.healthapp.entity.WeightEntry;
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
public interface WeightEntryRepository extends JpaRepository<WeightEntry, Long> {
    
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.status = :status")
    List<WeightEntry> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") WeightEntry.Status status);
    
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    List<WeightEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status
    );
    
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Page<WeightEntry> findByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status,
            Pageable pageable
    );
    
    @Query("SELECT w FROM WeightEntry w WHERE w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Page<WeightEntry> findByDateRangeAndStatus(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status,
            Pageable pageable
    );
    
    @Query("SELECT COUNT(w) > 0 FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :startTime AND :endTime AND w.status = :status")
    boolean existsByUserIdAndTimeRangeAndStatus(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") WeightEntry.Status status
    );
    
    @Query("SELECT COUNT(w) FROM WeightEntry w WHERE w.user.id = :userId AND w.loggedAt BETWEEN :fromDate AND :toDate AND w.status = :status")
    Long countByUserIdAndDateRangeAndStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") WeightEntry.Status status
    );
    
    @Query("SELECT w FROM WeightEntry w WHERE w.user.id = :userId AND w.status = :status ORDER BY w.loggedAt DESC")
    List<WeightEntry> findMostRecentByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") WeightEntry.Status status,
            Pageable pageable
    );
    
    Optional<WeightEntry> findByIdAndStatus(Long id, WeightEntry.Status status);
    
    @Query("SELECT w FROM WeightEntry w WHERE w.id = :id AND w.user.id = :userId AND w.status = :status")
    Optional<WeightEntry> findByIdAndUserIdAndStatus(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") WeightEntry.Status status
    );
}
