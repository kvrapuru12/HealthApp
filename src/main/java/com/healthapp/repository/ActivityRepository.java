package com.healthapp.repository;

import com.healthapp.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    @Query("SELECT a FROM Activity a WHERE a.createdBy.id = :userId AND a.status = :status")
    List<Activity> findByCreatedByAndStatus(@Param("userId") Long userId, @Param("status") Activity.Status status);
    
    @Query("SELECT a FROM Activity a WHERE a.createdBy.id = :userId AND a.status = :status")
    Page<Activity> findByCreatedByAndStatus(@Param("userId") Long userId, @Param("status") Activity.Status status, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE (a.createdBy.id = :userId OR a.visibility = 'PUBLIC') AND a.status = :status")
    Page<Activity> findByUserOrPublicAndStatus(@Param("userId") Long userId, @Param("status") Activity.Status status, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.status = :status AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Activity> findByStatusAndSearch(@Param("status") Activity.Status status, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.status = :status AND a.visibility = :visibility")
    Page<Activity> findByStatusAndVisibility(@Param("status") Activity.Status status, @Param("visibility") Activity.Visibility visibility, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.status = :status AND a.visibility = :visibility AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Activity> findByStatusAndVisibilityAndSearch(@Param("status") Activity.Status status, @Param("visibility") Activity.Visibility visibility, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.createdBy.id = :userId AND a.name = :name AND a.status = :status")
    Optional<Activity> findByCreatedByAndNameAndStatus(@Param("userId") Long userId, @Param("name") String name, @Param("status") Activity.Status status);
    
    @Query("SELECT a FROM Activity a WHERE a.createdBy.id = :userId AND a.name = :name AND a.id != :excludeId AND a.status = :status")
    Optional<Activity> findByCreatedByAndNameAndIdNotAndStatus(@Param("userId") Long userId, @Param("name") String name, @Param("excludeId") Long excludeId, @Param("status") Activity.Status status);
    
    Optional<Activity> findByIdAndStatus(Long id, Activity.Status status);
    
    @Query("SELECT a FROM Activity a WHERE a.id = :id AND a.status = :status AND (a.createdBy.id = :userId OR a.visibility = 'PUBLIC')")
    Optional<Activity> findByIdAndStatusAndAccessible(@Param("id") Long id, @Param("status") Activity.Status status, @Param("userId") Long userId);
}
