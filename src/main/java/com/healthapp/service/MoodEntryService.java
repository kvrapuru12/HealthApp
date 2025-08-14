package com.healthapp.service;

import com.healthapp.dto.MoodCreateRequest;
import com.healthapp.dto.MoodResponse;
import com.healthapp.entity.MoodEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.MoodEntryRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

@Service
@Transactional
public class MoodEntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MoodEntryService.class);
    
    @Autowired
    private MoodEntryRepository moodEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public Optional<MoodResponse> getMoodEntryById(Long id) {
        return moodEntryRepository.findById(id)
                .map(MoodResponse::new);
    }
    
    @Transactional(readOnly = true)
    public List<MoodResponse> getMoodEntriesByUserId(Long userId) {
        List<MoodEntry> moodEntries = moodEntryRepository.findByUserId(userId);
        return moodEntries.stream()
                .map(MoodResponse::new)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public MoodResponse createMoodEntry(MoodCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
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
        
        if (moodEntryRepository.existsByUserIdAndTimeRange(
                user.getId(), fiveMinutesBefore, fiveMinutesAfter)) {
            throw new IllegalArgumentException("Duplicate mood entry detected within ±5 minutes for the same user");
        }
        
        // Create and save the mood entry
        MoodEntry moodEntry = request.toEntity(user);
        MoodEntry savedMoodEntry = moodEntryRepository.save(moodEntry);
        
        logger.info("Created mood entry with ID: {} for user: {}", savedMoodEntry.getId(), user.getId());
        
        return new MoodResponse(savedMoodEntry);
    }
    

    
    @Transactional
    public MoodResponse updateMoodEntry(Long id, MoodCreateRequest request, Long authenticatedUserId, boolean isAdmin) {
        MoodEntry existingEntry = moodEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood entry not found"));
        
        // Check if user can update this entry
        if (!isAdmin && !existingEntry.getUser().getId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot update mood entry of another user");
        }
        
        // Update fields
        existingEntry.setLoggedAt(request.getLoggedAt());
        existingEntry.setMood(request.getMood());
        existingEntry.setIntensity(request.getIntensity());
        existingEntry.setNote(request.getNote());
        
        MoodEntry updatedEntry = moodEntryRepository.save(existingEntry);
        logger.info("Updated mood entry with ID: {}", updatedEntry.getId());
        
        return new MoodResponse(updatedEntry);
    }
    
    @Transactional
    public void softDeleteMoodEntry(Long id, Long authenticatedUserId, boolean isAdmin) {
        MoodEntry existingEntry = moodEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood entry not found"));
        
        // Check if user can delete this entry
        if (!isAdmin && !existingEntry.getUser().getId().equals(authenticatedUserId)) {
            throw new SecurityException("Cannot delete mood entry of another user");
        }
        
        existingEntry.setStatus(MoodEntry.Status.DELETED);
        moodEntryRepository.save(existingEntry);
        logger.info("Soft deleted mood entry with ID: {}", id);
    }
}
