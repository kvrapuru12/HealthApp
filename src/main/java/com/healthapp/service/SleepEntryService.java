package com.healthapp.service;

import com.healthapp.dto.SleepCreateRequest;
import com.healthapp.dto.SleepPaginatedResponse;
import com.healthapp.dto.SleepResponse;
import com.healthapp.dto.SleepUpdateRequest;
import com.healthapp.entity.SleepEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.SleepEntryRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SleepEntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SleepEntryService.class);
    
    @Autowired
    private SleepEntryRepository sleepEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public Optional<SleepResponse> getSleepEntryById(Long id) {
        return sleepEntryRepository.findById(id)
                .map(SleepResponse::new);
    }
    
    @Transactional(readOnly = true)
    public SleepPaginatedResponse getSleepEntries(Long userId, LocalDateTime from, LocalDateTime to, 
                                               Integer page, Integer limit, String sortBy, String sortDir) {
        
        // Validate and set default values
        if (page == null || page < 1) page = 1;
        if (limit == null || limit < 1 || limit > 100) limit = 20;
        if (sortBy == null) sortBy = "loggedAt";
        if (sortDir == null) sortDir = "desc";
        
        // Validate date range (max 366 days)
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("From date must be before or equal to to date");
            }
            if (ChronoUnit.DAYS.between(from, to) > 366) {
                throw new IllegalArgumentException("Date range cannot exceed 366 days");
            }
        }
        
        // Create sort
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        
        // Set default date range if not provided
        if (from == null) {
            from = LocalDateTime.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDateTime.now();
        }
        
        Page<SleepEntry> sleepEntriesPage;
        if (userId != null) {
            // Filter by specific user
            sleepEntriesPage = sleepEntryRepository.findByUserIdAndDateRangeAndStatus(
                    userId, from, to, SleepEntry.Status.ACTIVE, pageable);
        } else {
            // Get all entries (admin only)
            sleepEntriesPage = sleepEntryRepository.findByDateRangeAndStatus(
                    from, to, SleepEntry.Status.ACTIVE, pageable);
        }
        
        List<SleepResponse> items = sleepEntriesPage.getContent().stream()
                .map(SleepResponse::new)
                .collect(Collectors.toList());
        
        return new SleepPaginatedResponse(items, page, limit, sleepEntriesPage.getTotalElements());
    }
    
    @Transactional
    public SleepResponse createSleepEntry(SleepCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        // Validate user ID matches authenticated user (unless admin)
        if (!isAdmin && !request.getUserId().equals(authenticatedUserId)) {
            throw new SecurityException("User ID must match authenticated user");
        }
        
        // Find the user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate loggedAt is not more than 10 minutes in the future
        LocalDateTime now = LocalDateTime.now();
        if (request.getLoggedAt().isAfter(now.plus(10, ChronoUnit.MINUTES))) {
            throw new IllegalArgumentException("Logged at timestamp cannot be more than 10 minutes in the future");
        }
        
        // Check for duplicate entries within ±5 minutes for the same user
        LocalDateTime fiveMinutesBefore = request.getLoggedAt().minus(5, ChronoUnit.MINUTES);
        LocalDateTime fiveMinutesAfter = request.getLoggedAt().plus(5, ChronoUnit.MINUTES);
        
        if (sleepEntryRepository.existsByUserIdAndTimeRangeAndStatus(
                user.getId(), fiveMinutesBefore, fiveMinutesAfter, SleepEntry.Status.ACTIVE)) {
            throw new IllegalArgumentException("Duplicate sleep entry detected within ±5 minutes for the same user");
        }
        
        // Create and save the sleep entry
        SleepEntry sleepEntry = request.toEntity(user);
        SleepEntry savedSleepEntry = sleepEntryRepository.save(sleepEntry);
        
        logger.info("Created sleep entry with ID: {} for user: {}", savedSleepEntry.getId(), user.getId());
        
        return new SleepResponse(savedSleepEntry);
    }
    
    @Transactional
    public SleepResponse updateSleepEntry(Long id, SleepUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        SleepEntry existingEntry = sleepEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sleep entry not found"));
        
        // Check if user can update this entry
        if (!isAdmin && !existingEntry.getUser().getId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot update sleep entry of another user");
        }
        
        // Update only provided fields
        if (request.getHours() != null) {
            existingEntry.setHours(request.getHours());
        }
        if (request.getNote() != null) {
            existingEntry.setNote(request.getNote());
        }
        
        SleepEntry updatedEntry = sleepEntryRepository.save(existingEntry);
        logger.info("Updated sleep entry with ID: {}", updatedEntry.getId());
        
        return new SleepResponse(updatedEntry);
    }
    
    @Transactional
    public void softDeleteSleepEntry(Long id, Long authenticatedUserId, boolean isAdmin) {
        SleepEntry existingEntry = sleepEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sleep entry not found"));
        
        // Check if user can delete this entry
        if (!isAdmin && !existingEntry.getUser().getId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot delete sleep entry of another user");
        }
        
        existingEntry.setStatus(SleepEntry.Status.DELETED);
        sleepEntryRepository.save(existingEntry);
        logger.info("Soft deleted sleep entry with ID: {}", id);
    }
}
