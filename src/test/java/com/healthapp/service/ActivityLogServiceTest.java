package com.healthapp.service;

import com.healthapp.dto.ActivityLogCreateRequest;
import com.healthapp.dto.ActivityLogUpdateRequest;
import com.healthapp.entity.Activity;
import com.healthapp.entity.ActivityLog;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityLogRepository;
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
class ActivityLogServiceTest {
    
    @Mock
    private ActivityLogRepository activityLogRepository;
    
    @Mock
    private ActivityRepository activityRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ActivityLogService activityLogService;
    
    private User testUser;
    private Activity testActivity;
    private ActivityLog testActivityLog;
    private ActivityLogCreateRequest createRequest;
    private ActivityLogUpdateRequest updateRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("Gardening");
        testActivity.setCaloriesPerMinute(new BigDecimal("4.8"));
        testActivity.setStatus(Activity.Status.ACTIVE);
        
        testActivityLog = new ActivityLog();
        testActivityLog.setId(1L);
        testActivityLog.setUser(testUser);
        testActivityLog.setActivity(testActivity);
        testActivityLog.setLoggedAt(LocalDateTime.now());
        testActivityLog.setDurationMinutes(45);
        testActivityLog.setCaloriesBurned(new BigDecimal("216.0"));
        testActivityLog.setNote("Backyard cleanup");
        testActivityLog.setStatus(ActivityLog.Status.ACTIVE);
        testActivityLog.setCreatedAt(LocalDateTime.now());
        testActivityLog.setUpdatedAt(LocalDateTime.now());
        
        createRequest = new ActivityLogCreateRequest();
        createRequest.setUserId(1L);
        createRequest.setActivityId(1L);
        createRequest.setLoggedAt(LocalDateTime.now());
        createRequest.setDurationMinutes(45);
        createRequest.setNote("Backyard cleanup");
        
        updateRequest = new ActivityLogUpdateRequest();
        updateRequest.setDurationMinutes(60);
        updateRequest.setNote("Updated duration");
    }
    
    @Test
    void createActivityLog_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.findByIdAndStatusAndAccessible(1L, Activity.Status.ACTIVE, 1L))
                .thenReturn(Optional.of(testActivity));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(testActivityLog);
        
        // Act
        var result = activityLogService.createActivityLog(createRequest, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(testActivityLog.getId(), result.getId());
        assertEquals(testActivityLog.getCaloriesBurned(), result.getCaloriesBurned());
        verify(activityLogRepository).save(any(ActivityLog.class));
    }
    
    @Test
    void createActivityLog_FutureTime_ThrowsException() {
        // Arrange
        createRequest.setLoggedAt(LocalDateTime.now().plusMinutes(15));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                activityLogService.createActivityLog(createRequest, 1L, false));
    }
    
    @Test
    void getActivityLogById_Success() {
        // Arrange
        when(activityLogRepository.findByIdAndStatusAndUser(1L, ActivityLog.Status.ACTIVE, 1L))
                .thenReturn(Optional.of(testActivityLog));
        
        // Act
        var result = activityLogService.getActivityLogById(1L, 1L, false);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testActivityLog.getId(), result.get().getId());
    }
    
    @Test
    void getActivityLogById_NotFound_ReturnsEmpty() {
        // Arrange
        when(activityLogRepository.findByIdAndStatusAndUser(1L, ActivityLog.Status.ACTIVE, 1L))
                .thenReturn(Optional.empty());
        
        // Act
        var result = activityLogService.getActivityLogById(1L, 1L, false);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getActivityLogs_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "loggedAt"));
        Page<ActivityLog> page = new PageImpl<>(List.of(testActivityLog), pageable, 1);
        when(activityLogRepository.findByUserAndStatus(1L, ActivityLog.Status.ACTIVE, pageable))
                .thenReturn(page);
        
        // Act
        var result = activityLogService.getActivityLogs(1L, null, null, 1, 20, "loggedAt", "desc", 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getTotal());
    }
    
    @Test
    void updateActivityLog_Success() {
        // Arrange
        when(activityLogRepository.findByIdAndStatusAndUser(1L, ActivityLog.Status.ACTIVE, 1L))
                .thenReturn(Optional.of(testActivityLog));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(testActivityLog);
        
        // Act
        var result = activityLogService.updateActivityLog(1L, updateRequest, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals("updated", result.get("message"));
        verify(activityLogRepository).save(any(ActivityLog.class));
    }
    
    @Test
    void updateActivityLog_NotFound_ThrowsException() {
        // Arrange
        when(activityLogRepository.findByIdAndStatusAndUser(1L, ActivityLog.Status.ACTIVE, 1L))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                activityLogService.updateActivityLog(1L, updateRequest, 1L, false));
    }
    
    @Test
    void deleteActivityLog_Success() {
        // Arrange
        when(activityLogRepository.findByIdAndStatusAndUser(1L, ActivityLog.Status.ACTIVE, 1L))
                .thenReturn(Optional.of(testActivityLog));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(testActivityLog);
        
        // Act
        var result = activityLogService.deleteActivityLog(1L, 1L, false);
        
        // Assert
        assertNotNull(result);
        assertEquals("deleted", result.get("message"));
        verify(activityLogRepository).save(any(ActivityLog.class));
    }
}
