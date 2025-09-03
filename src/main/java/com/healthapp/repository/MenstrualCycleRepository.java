package com.healthapp.repository;

import com.healthapp.entity.MenstrualCycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenstrualCycleRepository extends JpaRepository<MenstrualCycle, Long> {
    
    // Find by user ID and status
    Page<MenstrualCycle> findByUserIdAndStatusOrderByPeriodStartDateDesc(Long userId, MenstrualCycle.Status status, Pageable pageable);
    
    // Find by user ID only (active status)
    Page<MenstrualCycle> findByUserIdOrderByPeriodStartDateDesc(Long userId, Pageable pageable);
    
    // Find by user ID and date range
    @Query("SELECT mc FROM MenstrualCycle mc WHERE mc.userId = :userId AND mc.status = :status " +
           "AND mc.periodStartDate BETWEEN :fromDate AND :toDate ORDER BY mc.periodStartDate DESC")
    Page<MenstrualCycle> findByUserIdAndStatusAndPeriodStartDateBetween(
            @Param("userId") Long userId,
            @Param("status") MenstrualCycle.Status status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
    
    // Find most recent cycle for a user
    Optional<MenstrualCycle> findFirstByUserIdAndStatusOrderByPeriodStartDateDesc(Long userId, MenstrualCycle.Status status);
    
    // Find by ID and user ID (for access control)
    Optional<MenstrualCycle> findByIdAndUserIdAndStatus(Long id, Long userId, MenstrualCycle.Status status);
    
    // Check if user has any cycles
    boolean existsByUserIdAndStatus(Long userId, MenstrualCycle.Status status);
    
    // Get all cycles for a user (for calculations)
    List<MenstrualCycle> findByUserIdAndStatusOrderByPeriodStartDateDesc(Long userId, MenstrualCycle.Status status);
    
    // Find cycles in a specific date range
    @Query("SELECT mc FROM MenstrualCycle mc WHERE mc.userId = :userId AND mc.status = :status " +
           "AND mc.periodStartDate >= :fromDate ORDER BY mc.periodStartDate DESC")
    List<MenstrualCycle> findByUserIdAndStatusAndPeriodStartDateAfter(
            @Param("userId") Long userId,
            @Param("status") MenstrualCycle.Status status,
            @Param("fromDate") LocalDate fromDate);
}
