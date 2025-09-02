package com.healthapp.service;

import com.healthapp.dto.WeightCreateRequest;
import com.healthapp.dto.WeightCreateResponse;
import com.healthapp.dto.WeightPaginatedResponse;
import com.healthapp.dto.WeightResponse;
import com.healthapp.dto.WeightUpdateRequest;
import com.healthapp.entity.WeightEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.WeightEntryRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class WeightEntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(WeightEntryService.class);
    
    @Autowired
    private WeightEntryRepository weightEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Optional<WeightResponse> getWeightEntryById(Long id, Long authenticatedUserId, boolean isAdmin) {
        WeightEntry weightEntry;
        if (isAdmin) {
            weightEntry = weightEntryRepository.findByIdAndStatus(id, WeightEntry.Status.ACTIVE).orElse(null);
        } else {
            weightEntry = weightEntryRepository.findByIdAndUserIdAndStatus(id, authenticatedUserId, WeightEntry.Status.ACTIVE).orElse(null);
        }
        return Optional.ofNullable(weightEntry).map(WeightResponse::new);
    }
    
    public WeightPaginatedResponse getWeightEntries(Long userId, LocalDateTime from, LocalDateTime to,
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
        Page<WeightEntry> weightEntriesPage;
        if (userId != null) {
            weightEntriesPage = weightEntryRepository.findByUserIdAndDateRangeAndStatus(
                    userId, from, to, WeightEntry.Status.ACTIVE, pageable);
        } else {
            weightEntriesPage = weightEntryRepository.findByDateRangeAndStatus(
                    from, to, WeightEntry.Status.ACTIVE, pageable);
        }
        
        // Convert to response DTOs
        List<WeightResponse> items = weightEntriesPage.getContent().stream()
                .map(WeightResponse::new)
                .toList();
        
        return new WeightPaginatedResponse(items, page, limit, weightEntriesPage.getTotalElements());
    }
    
    public WeightCreateResponse createWeightEntry(WeightCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        try {
            
            // Validate user access
            if (!isAdmin && !request.getUserId().equals(authenticatedUserId)) {
                throw new IllegalArgumentException("Users can only create weight entries for themselves");
            }
            
            // Validate future timestamp (allow up to 10 minutes in the future)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime maxFutureTime = now.plusMinutes(10);
            if (request.getLoggedAt().isAfter(maxFutureTime)) {
                throw new IllegalArgumentException("Logged at time cannot be more than 10 minutes in the future");
            }
            
            // Check for duplicate entries within 5 minutes
            LocalDateTime startTime = request.getLoggedAt().minusMinutes(5);
            LocalDateTime endTime = request.getLoggedAt().plusMinutes(5);
            if (weightEntryRepository.existsByUserIdAndTimeRangeAndStatus(
                    request.getUserId(), startTime, endTime, WeightEntry.Status.ACTIVE)) {
                throw new IllegalArgumentException("A weight entry already exists within 5 minutes of this time");
            }
            
            // Get user
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Create weight entry
            WeightEntry weightEntry = new WeightEntry(user, request.getLoggedAt(), request.getWeight(), request.getNote());
            WeightEntry savedEntry = weightEntryRepository.save(weightEntry);
            
            // Sync user's latest weight if this is the most recent entry
            syncUserLatestWeight(request.getUserId());
            
            return new WeightCreateResponse(savedEntry.getId(), savedEntry.getCreatedAt());
            
        } catch (Exception e) {
            logger.error("Error creating weight entry: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Map<String, Object> updateWeightEntry(Long id, WeightUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        try {
            
            // Get weight entry with access control
            WeightEntry weightEntry;
            if (isAdmin) {
                weightEntry = weightEntryRepository.findByIdAndStatus(id, WeightEntry.Status.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("Weight entry not found"));
            } else {
                weightEntry = weightEntryRepository.findByIdAndUserIdAndStatus(id, authenticatedUserId, WeightEntry.Status.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("Weight entry not found"));
            }
            
            // Validate future timestamp if loggedAt is being updated
            if (request.getLoggedAt() != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime maxFutureTime = now.plusMinutes(10);
                if (request.getLoggedAt().isAfter(maxFutureTime)) {
                    throw new IllegalArgumentException("Logged at time cannot be more than 10 minutes in the future");
                }
            }
            
            // Update fields
            boolean needsSync = false;
            if (request.getLoggedAt() != null) {
                weightEntry.setLoggedAt(request.getLoggedAt());
                needsSync = true;
            }
            if (request.getWeight() != null) {
                weightEntry.setWeight(request.getWeight());
            }
            if (request.getNote() != null) {
                weightEntry.setNote(request.getNote());
            }
            
            // Save the updated entry
            WeightEntry savedEntry = weightEntryRepository.save(weightEntry);
            
            // Sync user's latest weight if loggedAt was updated
            if (needsSync) {
                syncUserLatestWeight(weightEntry.getUserId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "updated");
            response.put("updatedAt", savedEntry.getUpdatedAt());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error updating weight entry: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public Map<String, String> deleteWeightEntry(Long id, Long authenticatedUserId, boolean isAdmin) {
        try {
            
            // Get weight entry with access control
            WeightEntry weightEntry;
            if (isAdmin) {
                weightEntry = weightEntryRepository.findByIdAndStatus(id, WeightEntry.Status.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("Weight entry not found"));
            } else {
                weightEntry = weightEntryRepository.findByIdAndUserIdAndStatus(id, authenticatedUserId, WeightEntry.Status.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("Weight entry not found"));
            }
            
            // Soft delete
            weightEntry.setStatus(WeightEntry.Status.DELETED);
            weightEntryRepository.save(weightEntry);
            
            // Sync user's latest weight since this entry was deleted
            syncUserLatestWeight(weightEntry.getUserId());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "deleted");
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error deleting weight entry: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void syncUserLatestWeight(Long userId) {
        try {
            // Find the most recent active weight entry for the user
            Pageable pageable = PageRequest.of(0, 1);
            List<WeightEntry> recentEntries = weightEntryRepository.findMostRecentByUserIdAndStatus(
                    userId, WeightEntry.Status.ACTIVE, pageable);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            if (!recentEntries.isEmpty()) {
                WeightEntry latestEntry = recentEntries.get(0);
                user.setWeight(latestEntry.getWeight().doubleValue());
            } else {
                user.setWeight(null);
            }
            
            userRepository.save(user);
            
        } catch (Exception e) {
            logger.error("Error syncing user weight for userId {}: {}", userId, e.getMessage(), e);
            // Don't throw exception to avoid breaking the main operation
        }
    }
}
