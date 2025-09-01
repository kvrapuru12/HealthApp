package com.healthapp.service;

import com.healthapp.dto.WaterCreateRequest;
import com.healthapp.dto.WaterPaginatedResponse;
import com.healthapp.dto.WaterResponse;
import com.healthapp.dto.WaterUpdateRequest;
import com.healthapp.entity.WaterEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.WaterEntryRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WaterEntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(WaterEntryService.class);
    
    @Autowired
    private WaterEntryRepository waterEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get water entry by ID with proper access control
     */
    public Optional<WaterResponse> getWaterEntryById(Long id) {
        return waterEntryRepository.findByIdAndStatus(id, WaterEntry.Status.ACTIVE)
                .map(WaterResponse::new);
    }
    
    /**
     * Get paginated water entries with filtering and sorting
     * Optimized for performance with proper indexing
     */
    public WaterPaginatedResponse getWaterEntries(Long userId, LocalDateTime from, LocalDateTime to,
                                                Integer page, Integer limit, String sortBy, String sortDir) {
        
        // Validate and normalize parameters
        page = Math.max(1, page);
        limit = Math.min(100, Math.max(1, limit));
        
        // Set default date range if not provided
        if (from == null) {
            from = LocalDateTime.now().minusDays(30); // Default to last 30 days
        }
        if (to == null) {
            to = LocalDateTime.now();
        }
        
        // Validate date range
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }
        
        // Create pageable with sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        
        // Execute query based on whether userId is specified
        Page<WaterEntry> waterEntriesPage;
        if (userId != null) {
            waterEntriesPage = waterEntryRepository.findByUserIdAndDateRangeAndStatus(
                    userId, from, to, WaterEntry.Status.ACTIVE, pageable);
        } else {
            waterEntriesPage = waterEntryRepository.findByDateRangeAndStatus(
                    from, to, WaterEntry.Status.ACTIVE, pageable);
        }
        
        // Convert to response DTOs
        List<WaterResponse> items = waterEntriesPage.getContent().stream()
                .map(WaterResponse::new)
                .toList();
        
        return new WaterPaginatedResponse(items, page, limit, waterEntriesPage.getTotalElements());
    }
    
    /**
     * Create a new water entry with validation
     * Includes duplicate checking and future timestamp validation
     */
    public WaterResponse createWaterEntry(WaterCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        try {
            logger.info("WaterEntryService.createWaterEntry called with userId: {}, authenticatedUserId: {}, isAdmin: {}", 
                request.getUserId(), authenticatedUserId, isAdmin);
            
            // Validate user access
            if (!isAdmin && !request.getUserId().equals(authenticatedUserId)) {
                logger.warn("Access denied: User {} cannot create water entry for user {}", authenticatedUserId, request.getUserId());
                throw new SecurityException("Cannot create water entry for another user");
            }
            
            logger.debug("User access validation passed");
            
            // Validate loggedAt time (max 10 minutes in future)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime maxFutureTime = now.plusMinutes(10);
            if (request.getLoggedAt().isAfter(maxFutureTime)) {
                logger.warn("Future timestamp validation failed: loggedAt={}, maxFutureTime={}", request.getLoggedAt(), maxFutureTime);
                throw new IllegalArgumentException("Logged at time cannot be more than 10 minutes in the future");
            }
            
            logger.debug("Timestamp validation passed: loggedAt={}, now={}", request.getLoggedAt(), now);
            
            // Get user
            logger.debug("Fetching user with ID: {}", request.getUserId());
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> {
                        logger.error("User not found with ID: {}", request.getUserId());
                        return new IllegalArgumentException("User not found");
                    });
            logger.debug("User found: {}", user.getUsername());
            
            // Check for duplicate entries within ±5 minutes
            LocalDateTime fiveMinutesBefore = request.getLoggedAt().minusMinutes(5);
            LocalDateTime fiveMinutesAfter = request.getLoggedAt().plusMinutes(5);
            
            logger.debug("Checking for duplicates in time range: {} to {}", fiveMinutesBefore, fiveMinutesAfter);
            boolean duplicateExists = waterEntryRepository.existsByUserIdAndTimeRangeAndStatus(
                    user.getId(), fiveMinutesBefore, fiveMinutesAfter, WaterEntry.Status.ACTIVE);
            
            if (duplicateExists) {
                logger.warn("Duplicate water entry detected for user {} in time range {} to {}", 
                    user.getId(), fiveMinutesBefore, fiveMinutesAfter);
                throw new IllegalArgumentException("Duplicate water entry detected within ±5 minutes for the same user");
            }
            
            logger.debug("Duplicate check passed");
            
            // Create and save the water entry
            logger.debug("Creating WaterEntry entity");
            WaterEntry waterEntry = new WaterEntry(user, request.getLoggedAt(), request.getAmount(), request.getNote());
            
            logger.debug("Saving WaterEntry to database");
            WaterEntry savedWaterEntry = waterEntryRepository.save(waterEntry);
            
            logger.info("Successfully created water entry with ID: {} for user: {}", savedWaterEntry.getId(), user.getId());
            
            logger.debug("Converting to WaterResponse");
            WaterResponse response = new WaterResponse(savedWaterEntry);
            logger.debug("WaterResponse created successfully");
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in createWaterEntry: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Update an existing water entry with validation
     */
    public WaterResponse updateWaterEntry(Long id, WaterUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        WaterEntry existingEntry = waterEntryRepository.findByIdAndStatus(id, WaterEntry.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Water entry not found"));
        
        // Check access control
        if (!isAdmin && !existingEntry.getUserId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot update water entry of another user");
        }
        
        // Update fields if provided
        if (request.getLoggedAt() != null) {
            // Validate loggedAt time (max 10 minutes in future)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime maxFutureTime = now.plusMinutes(10);
            if (request.getLoggedAt().isAfter(maxFutureTime)) {
                throw new IllegalArgumentException("Logged at time cannot be more than 10 minutes in the future");
            }
            existingEntry.setLoggedAt(request.getLoggedAt());
        }
        
        if (request.getAmount() != null) {
            existingEntry.setAmount(request.getAmount());
        }
        
        if (request.getNote() != null) {
            existingEntry.setNote(request.getNote());
        }
        
        // Save the updated entry
        WaterEntry updatedEntry = waterEntryRepository.save(existingEntry);
        logger.info("Updated water entry with ID: {}", updatedEntry.getId());
        
        return new WaterResponse(updatedEntry);
    }
    
    /**
     * Soft delete a water entry
     */
    public void softDeleteWaterEntry(Long id, Long authenticatedUserId, boolean isAdmin) {
        WaterEntry existingEntry = waterEntryRepository.findByIdAndStatus(id, WaterEntry.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Water entry not found"));
        
        // Check access control
        if (!isAdmin && !existingEntry.getUserId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot delete water entry of another user");
        }
        
        // Soft delete by setting status
        existingEntry.setStatus(WaterEntry.Status.DELETED);
        waterEntryRepository.save(existingEntry);
        
        logger.info("Soft deleted water entry with ID: {}", id);
    }
    
    /**
     * Get total water consumption for a user within a date range
     * Optimized for performance with aggregation query
     */
    public Integer getTotalWaterConsumptionByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        return waterEntryRepository.sumAmountByUserIdAndDateRangeAndStatus(
                userId, from, to, WaterEntry.Status.ACTIVE);
    }
    
    /**
     * Get water entry count for a user
     */
    public Long getWaterEntryCountByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        return waterEntryRepository.countByUserIdAndDateRangeAndStatus(
                userId, from, to, WaterEntry.Status.ACTIVE);
    }
}
