package com.healthapp.repository;

import com.healthapp.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    
    // Find by ID and status
    Optional<FoodItem> findByIdAndStatus(Long id, FoodItem.FoodStatus status);
    
    // Find by created by and status
    List<FoodItem> findByCreatedByAndStatus(Long createdBy, FoodItem.FoodStatus status);
    
    // Find by visibility and status
    List<FoodItem> findByVisibilityAndStatus(FoodItem.FoodVisibility visibility, FoodItem.FoodStatus status);
    
    // Find by created by, visibility and status
    List<FoodItem> findByCreatedByAndVisibilityAndStatus(Long createdBy, FoodItem.FoodVisibility visibility, FoodItem.FoodStatus status);
    
    // Search by name or category with pagination
    @Query("SELECT f FROM FoodItem f WHERE f.status = :status AND " +
           "((:createdBy IS NULL AND f.visibility = 'PUBLIC') OR f.createdBy = :createdBy) AND " +
           "(:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<FoodItem> findBySearchCriteria(
            @Param("createdBy") Long createdBy,
            @Param("search") String search,
            @Param("status") FoodItem.FoodStatus status,
            Pageable pageable);
    
    // Find by visibility with pagination
    @Query("SELECT f FROM FoodItem f WHERE f.status = :status AND " +
           "((:createdBy IS NULL AND f.visibility = :visibility) OR " +
           "(f.createdBy = :createdBy AND (f.visibility = :visibility OR f.visibility = 'PUBLIC')))")
    Page<FoodItem> findByVisibilityAndCreatedBy(
            @Param("createdBy") Long createdBy,
            @Param("visibility") FoodItem.FoodVisibility visibility,
            @Param("status") FoodItem.FoodStatus status,
            Pageable pageable);
    
    // Check if name exists for user
    boolean existsByNameAndCreatedByAndStatusNot(String name, Long createdBy, FoodItem.FoodStatus status);
    
    // Find all active food items for a user (including public ones)
    @Query("SELECT f FROM FoodItem f WHERE f.status = 'ACTIVE' AND " +
           "(f.createdBy = :userId OR f.visibility = 'PUBLIC')")
    List<FoodItem> findAvailableFoodItems(@Param("userId") Long userId);
    
    // Find by name (case insensitive), status and created by
    Optional<FoodItem> findByNameIgnoreCaseAndStatusAndCreatedBy(String name, FoodItem.FoodStatus status, Long createdBy);
    
    // Find by name (case insensitive), status and visibility
    Optional<FoodItem> findByNameIgnoreCaseAndStatusAndVisibility(String name, FoodItem.FoodStatus status, FoodItem.FoodVisibility visibility);
}
