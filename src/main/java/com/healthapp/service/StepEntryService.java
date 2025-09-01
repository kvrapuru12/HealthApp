package com.healthapp.service;

import com.healthapp.dto.StepCreateRequest;
import com.healthapp.dto.StepPaginatedResponse;
import com.healthapp.dto.StepResponse;
import com.healthapp.dto.StepUpdateRequest;
import com.healthapp.entity.StepEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.StepEntryRepository;
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
public class StepEntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(StepEntryService.class);
    
    @Autowired
    private StepEntryRepository stepEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get step entry by ID with proper access control
     */
    public Optional<StepResponse> getStepEntryById(Long id) {
        return stepEntryRepository.findByIdAndStatus(id, StepEntry.Status.ACTIVE)
                .map(StepResponse::new);
    }
    
    /**
     * Get paginated step entries with filtering and sorting
     * Optimized for performance with proper indexing
     */
    public StepPaginatedResponse getStepEntries(Long userId, LocalDateTime from, LocalDateTime to,
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
        Page<StepEntry> stepEntriesPage;
        if (userId != null) {
            stepEntriesPage = stepEntryRepository.findByUserIdAndDateRangeAndStatus(
                    userId, from, to, StepEntry.Status.ACTIVE, pageable);
        } else {
            stepEntriesPage = stepEntryRepository.findByDateRangeAndStatus(
                    from, to, StepEntry.Status.ACTIVE, pageable);
        }
        
        // Convert to response DTOs
        List<StepResponse> items = stepEntriesPage.getContent().stream()
                .map(StepResponse::new)
                .toList();
        
        return new StepPaginatedResponse(items, page, limit, stepEntriesPage.getTotalElements());
    }
    
    /**
     * Create a new step entry with validation
     * Includes duplicate checking and future timestamp validation
     */
    public StepResponse createStepEntry(StepCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        try {
            logger.info("StepEntryService.createStepEntry called with userId: {}, authenticatedUserId: {}, isAdmin: {}", 
                request.getUserId(), authenticatedUserId, isAdmin);
            
            // Validate user access
            if (!isAdmin && !request.getUserId().equals(authenticatedUserId)) {
                logger.warn("Access denied: User {} cannot create step entry for user {}", authenticatedUserId, request.getUserId());
                throw new SecurityException("Cannot create step entry for another user");
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
            boolean duplicateExists = stepEntryRepository.existsByUserIdAndTimeRangeAndStatus(
                    user.getId(), fiveMinutesBefore, fiveMinutesAfter, StepEntry.Status.ACTIVE);
            
            if (duplicateExists) {
                logger.warn("Duplicate step entry detected for user {} in time range {} to {}", 
                    user.getId(), fiveMinutesBefore, fiveMinutesAfter);
                throw new IllegalArgumentException("Duplicate step entry detected within ±5 minutes for the same user");
            }
            
            logger.debug("Duplicate check passed");
            
            // Create and save the step entry
            logger.debug("Creating StepEntry entity");
            StepEntry stepEntry = new StepEntry(user, request.getLoggedAt(), request.getStepCount(), request.getNote());
            
            logger.debug("Saving StepEntry to database");
            StepEntry savedStepEntry = stepEntryRepository.save(stepEntry);
            
            logger.info("Successfully created step entry with ID: {} for user: {}", savedStepEntry.getId(), user.getId());
            
            logger.debug("Converting to StepResponse");
            StepResponse response = new StepResponse(savedStepEntry);
            logger.debug("StepResponse created successfully");
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in createStepEntry: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Update an existing step entry with validation
     */
    public StepResponse updateStepEntry(Long id, StepUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        StepEntry existingEntry = stepEntryRepository.findByIdAndStatus(id, StepEntry.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Step entry not found"));
        
        // Check access control
        if (!isAdmin && !existingEntry.getUserId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot update step entry of another user");
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
        
        if (request.getStepCount() != null) {
            existingEntry.setStepCount(request.getStepCount());
        }
        
        if (request.getNote() != null) {
            existingEntry.setNote(request.getNote());
        }
        
        // Save the updated entry
        StepEntry updatedEntry = stepEntryRepository.save(existingEntry);
        logger.info("Updated step entry with ID: {}", updatedEntry.getId());
        
        return new StepResponse(updatedEntry);
    }
    
    /**
     * Soft delete a step entry
     */
    public void softDeleteStepEntry(Long id, Long authenticatedUserId, boolean isAdmin) {
        StepEntry existingEntry = stepEntryRepository.findByIdAndStatus(id, StepEntry.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Step entry not found"));
        
        // Check access control
        if (!isAdmin && !existingEntry.getUserId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot delete step entry of another user");
        }
        
        // Soft delete by setting status
        existingEntry.setStatus(StepEntry.Status.DELETED);
        stepEntryRepository.save(existingEntry);
        
        logger.info("Soft deleted step entry with ID: {}", id);
    }
    
    /**
     * Get total step count for a user within a date range
     * Optimized for performance with aggregation query
     */
    public Integer getTotalStepCountByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        return stepEntryRepository.sumStepCountByUserIdAndDateRangeAndStatus(
                userId, from, to, StepEntry.Status.ACTIVE);
    }
    
    /**
     * Get step count statistics for a user
     */
    public Long getStepEntryCountByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        return stepEntryRepository.countByUserIdAndDateRangeAndStatus(
                userId, from, to, StepEntry.Status.ACTIVE);
    }
}
