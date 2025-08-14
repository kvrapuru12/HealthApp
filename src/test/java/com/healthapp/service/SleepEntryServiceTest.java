package com.healthapp.service;

import com.healthapp.dto.SleepCreateRequest;
import com.healthapp.dto.SleepUpdateRequest;
import com.healthapp.entity.SleepEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.SleepEntryRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SleepEntryServiceTest {

    @Mock
    private SleepEntryRepository sleepEntryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SleepEntryService sleepEntryService;

    private User testUser;
    private SleepEntry testSleepEntry;
    private SleepCreateRequest createRequest;
    private SleepUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setEmail("test@example.com");

        testSleepEntry = new SleepEntry();
        testSleepEntry.setId(1L);
        testSleepEntry.setUser(testUser);
        testSleepEntry.setLoggedAt(LocalDateTime.now());
        testSleepEntry.setHours(new BigDecimal("7.5"));
        testSleepEntry.setStatus(SleepEntry.Status.ACTIVE);

        createRequest = new SleepCreateRequest();
        createRequest.setUserId(1L);
        createRequest.setLoggedAt(LocalDateTime.now());
        createRequest.setHours(new BigDecimal("7.5"));
        createRequest.setNote("Test sleep");

        updateRequest = new SleepUpdateRequest();
        updateRequest.setHours(new BigDecimal("8.0"));
        updateRequest.setNote("Updated note");
    }

    @Test
    void testGetSleepEntryById() {
        when(sleepEntryRepository.findById(1L)).thenReturn(Optional.of(testSleepEntry));

        var result = sleepEntryService.getSleepEntryById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(sleepEntryRepository).findById(1L);
    }

    @Test
    void testGetSleepEntryByIdNotFound() {
        when(sleepEntryRepository.findById(1L)).thenReturn(Optional.empty());

        var result = sleepEntryService.getSleepEntryById(1L);

        assertFalse(result.isPresent());
        verify(sleepEntryRepository).findById(1L);
    }

    @Test
    void testCreateSleepEntry() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sleepEntryRepository.existsByUserIdAndTimeRangeAndStatus(any(), any(), any(), any())).thenReturn(false);
        when(sleepEntryRepository.save(any())).thenReturn(testSleepEntry);

        var result = sleepEntryService.createSleepEntry(createRequest, 1L, false);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).findById(1L);
        verify(sleepEntryRepository).save(any());
    }

    @Test
    void testUpdateSleepEntry() {
        when(sleepEntryRepository.findById(1L)).thenReturn(Optional.of(testSleepEntry));
        when(sleepEntryRepository.save(any())).thenReturn(testSleepEntry);

        var result = sleepEntryService.updateSleepEntry(1L, updateRequest, 1L, false);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(sleepEntryRepository).findById(1L);
        verify(sleepEntryRepository).save(any());
    }

    @Test
    void testSoftDeleteSleepEntry() {
        when(sleepEntryRepository.findById(1L)).thenReturn(Optional.of(testSleepEntry));
        when(sleepEntryRepository.save(any())).thenReturn(testSleepEntry);

        assertDoesNotThrow(() -> sleepEntryService.softDeleteSleepEntry(1L, 1L, false));

        verify(sleepEntryRepository).findById(1L);
        verify(sleepEntryRepository).save(any());
        assertEquals(SleepEntry.Status.DELETED, testSleepEntry.getStatus());
    }
}
