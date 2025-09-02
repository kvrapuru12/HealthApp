package com.healthapp.repository;

import com.healthapp.entity.ActivityLog;
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
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId AND al.status = :status")
    List<ActivityLog> findByUserAndStatus(@Param("userId") Long userId, @Param("status") ActivityLog.Status status);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId AND al.status = :status")
    Page<ActivityLog> findByUserAndStatus(@Param("userId") Long userId, @Param("status") ActivityLog.Status status, Pageable pageable);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId AND al.status = :status AND al.loggedAt BETWEEN :from AND :to")
    Page<ActivityLog> findByUserAndStatusAndDateRange(@Param("userId") Long userId, @Param("status") ActivityLog.Status status, 
                                                     @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId AND al.status = :status AND al.loggedAt >= :from")
    Page<ActivityLog> findByUserAndStatusAndFromDate(@Param("userId") Long userId, @Param("status") ActivityLog.Status status, 
                                                   @Param("from") LocalDateTime from, Pageable pageable);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId AND al.status = :status AND al.loggedAt <= :to")
    Page<ActivityLog> findByUserAndStatusAndToDate(@Param("userId") Long userId, @Param("status") ActivityLog.Status status, 
                                                 @Param("to") LocalDateTime to, Pageable pageable);
    
    Optional<ActivityLog> findByIdAndStatus(Long id, ActivityLog.Status status);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.id = :id AND al.status = :status AND al.user.id = :userId")
    Optional<ActivityLog> findByIdAndStatusAndUser(@Param("id") Long id, @Param("status") ActivityLog.Status status, @Param("userId") Long userId);
}
