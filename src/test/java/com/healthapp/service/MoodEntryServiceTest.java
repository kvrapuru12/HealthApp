package com.healthapp.service;

import com.healthapp.dto.MoodCreateRequest;
import com.healthapp.dto.MoodResponse;
import com.healthapp.entity.MoodEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.MoodEntryRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodEntryServiceTest {
    
    @Mock
    private MoodEntryRepository moodEntryRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private MoodEntryService moodEntryService;
    
    private User testUser;
    private MoodCreateRequest testRequest;
    private MoodEntry testMoodEntry;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testRequest = new MoodCreateRequest();
        testRequest.setUserId(1L);
        testRequest.setLoggedAt(LocalDateTime.now());
        testRequest.setMood(MoodEntry.Mood.HAPPY);
        testRequest.setIntensity(8);
        testRequest.setNote("Feeling great today!");
        
        testMoodEntry = new MoodEntry();
        testMoodEntry.setId(1L);
        testMoodEntry.setUser(testUser);
        testMoodEntry.setLoggedAt(testRequest.getLoggedAt());
        testMoodEntry.setMood(testRequest.getMood());
        testMoodEntry.setIntensity(testRequest.getIntensity());
        testMoodEntry.setNote(testRequest.getNote());
    }
    
    @Test
    void createMoodEntry_Success() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(moodEntryRepository.existsByUserIdAndTimeRange(any(), any(), any())).thenReturn(false);
        when(moodEntryRepository.save(any())).thenReturn(testMoodEntry);
        
        // Act
        MoodResponse result = moodEntryService.createMoodEntry(testRequest, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(testMoodEntry.getId(), result.getId());
        assertEquals(testMoodEntry.getMood(), result.getMood());
        verify(moodEntryRepository).save(any(MoodEntry.class));
    }
    
    @Test
    void createMoodEntry_UserNotFound() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            moodEntryService.createMoodEntry(testRequest, 1L, false);
        });
    }
    
    @Test
    void createMoodEntry_DuplicateDetected() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(moodEntryRepository.existsByUserIdAndTimeRange(any(), any(), any())).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            moodEntryService.createMoodEntry(testRequest, 1L, false);
        });
    }
    
    @Test
    void createMoodEntry_FutureTimestampTooFar() {
        // Arrange
        testRequest.setLoggedAt(LocalDateTime.now().plus(15, java.time.temporal.ChronoUnit.MINUTES));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            moodEntryService.createMoodEntry(testRequest, 1L, false);
        });
    }
    
    @Test
    void createMoodEntry_AdminCanCreateForAnyUser() {
        // Arrange
        testRequest.setUserId(999L);
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(moodEntryRepository.existsByUserIdAndTimeRange(any(), any(), any())).thenReturn(false);
        when(moodEntryRepository.save(any())).thenReturn(testMoodEntry);
        
        // Act
        MoodResponse result = moodEntryService.createMoodEntry(testRequest, 999L, true);
        
        // Assert
        assertNotNull(result);
        verify(moodEntryRepository).save(any(MoodEntry.class));
    }
    
    @Test
    void createMoodEntry_NonAdminCannotCreateForOtherUser() {
        // Arrange
        testRequest.setUserId(999L);
        
        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            moodEntryService.createMoodEntry(testRequest, 1L, false);
        });
    }
}
