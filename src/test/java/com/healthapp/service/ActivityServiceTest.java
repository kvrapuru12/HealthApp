package com.healthapp.service;

import com.healthapp.dto.ActivityCreateRequest;
import com.healthapp.dto.ActivityUpdateRequest;
import com.healthapp.entity.Activity;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
    
    @Mock
    private ActivityRepository activityRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ActivityService activityService;
    
    private User testUser;
    private Activity testActivity;
    private ActivityCreateRequest createRequest;
    private ActivityUpdateRequest updateRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("Gardening");
        testActivity.setCategory("home");
        testActivity.setCaloriesPerMinute(new BigDecimal("4.8"));
        testActivity.setVisibility(Activity.Visibility.PRIVATE);
        testActivity.setCreatedBy(testUser);
        testActivity.setStatus(Activity.Status.ACTIVE);
        testActivity.setCreatedAt(LocalDateTime.now());
        testActivity.setUpdatedAt(LocalDateTime.now());
        
        createRequest = new ActivityCreateRequest();
        createRequest.setName("Gardening");
        createRequest.setCategory("home");
        createRequest.setCaloriesPerMinute(new BigDecimal("4.8"));
        createRequest.setVisibility(Activity.Visibility.PRIVATE);
        
        updateRequest = new ActivityUpdateRequest();
        updateRequest.setName("Updated Gardening");
        updateRequest.setCaloriesPerMinute(new BigDecimal("5.0"));
    }
    
    @Test
    void createActivity_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.findByCreatedByAndNameAndStatus(1L, "Gardening", Activity.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);
        
        // Act
        var result = activityService.createActivity(createRequest, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(testActivity.getId(), result.getId());
        verify(activityRepository).save(any(Activity.class));
    }
    
    @Test
    void createActivity_DuplicateName_ThrowsException() {
        // Arrange
        when(activityRepository.findByCreatedByAndNameAndStatus(1L, "Gardening", Activity.Status.ACTIVE))
                .thenReturn(Optional.of(testActivity));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                activityService.createActivity(createRequest, 1L, false));
    }
    
    @Test
    void getActivityById_Success() {
        // Arrange
        when(activityRepository.findByIdAndStatusAndAccessible(1L, Activity.Status.ACTIVE, 1L))
                .thenReturn(Optional.of(testActivity));
        
        // Act
        var result = activityService.getActivityById(1L, 1L, false);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testActivity.getId(), result.get().getId());
    }
    
    @Test
    void getActivityById_NotFound_ReturnsEmpty() {
        // Arrange
        when(activityRepository.findByIdAndStatusAndAccessible(1L, Activity.Status.ACTIVE, 1L))
                .thenReturn(Optional.empty());
        
        // Act
        var result = activityService.getActivityById(1L, 1L, false);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getActivities_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Activity> page = new PageImpl<>(List.of(testActivity), pageable, 1);
        when(activityRepository.findByUserOrPublicAndStatus(1L, Activity.Status.ACTIVE, pageable))
                .thenReturn(page);
        
        // Act
        var result = activityService.getActivities(null, null, 1, 20, "createdAt", "desc", 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getTotal());
    }
    
    @Test
    void updateActivity_Success() {
        // Arrange
        when(activityRepository.findByIdAndStatus(1L, Activity.Status.ACTIVE))
                .thenReturn(Optional.of(testActivity));
        when(activityRepository.findByCreatedByAndNameAndIdNotAndStatus(1L, "Updated Gardening", 1L, Activity.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);
        
        // Act
        var result = activityService.updateActivity(1L, updateRequest, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals("updated", result.get("message"));
        verify(activityRepository).save(any(Activity.class));
    }
    
    @Test
    void updateActivity_NotFound_ThrowsException() {
        // Arrange
        when(activityRepository.findByIdAndStatus(1L, Activity.Status.ACTIVE))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                activityService.updateActivity(1L, updateRequest, 1L, false));
    }
    
    @Test
    void deleteActivity_Success() {
        // Arrange
        when(activityRepository.findByIdAndStatus(1L, Activity.Status.ACTIVE))
                .thenReturn(Optional.of(testActivity));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);
        
        // Act
        var result = activityService.deleteActivity(1L, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals("deleted", result.get("message"));
        verify(activityRepository).save(any(Activity.class));
    }
}
