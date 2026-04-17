package com.healthapp.service;

import com.healthapp.entity.StepEntry;
import com.healthapp.repository.AppleHealthStepSampleRepository;
import com.healthapp.repository.StepEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardDailyServiceTest {

    @Mock
    private AppleHealthStepSampleRepository appleHealthStepSampleRepository;

    @Mock
    private StepEntryRepository stepEntryRepository;

    @InjectMocks
    private DashboardDailyService dashboardDailyService;

    private static final Long USER_ID = 42L;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 16);

    @BeforeEach
    void setUp() {
        when(stepEntryRepository.sumStepCountByUserIdAndDateRangeHalfOpen(
                eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class), eq(StepEntry.Status.ACTIVE)))
                .thenReturn(200);
    }

    @Test
    void getDaily_ApplePresent_PrefersAppleAndSetsConflictFlags() {
        when(appleHealthStepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(1L);
        when(appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(8432);

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals("PREFER_APPLE_HEALTH_IF_PRESENT", response.getSteps().getMergePolicy());
        assertEquals(8432, response.getSteps().getDisplayedSteps());
        assertTrue(response.getSteps().getConflictFlags().isManualVsAppleMismatch());
        assertTrue(response.getSteps().getConflictFlags().isManualIgnoredForDisplay());
        assertEquals(2, response.getSteps().getBySource().size());
        assertEquals("APPLE_HEALTH", response.getSteps().getBySource().get(0).getSource());
        assertEquals(8432, response.getSteps().getBySource().get(0).getSteps());
        assertEquals("MANUAL_APP", response.getSteps().getBySource().get(1).getSource());
        assertEquals(200, response.getSteps().getBySource().get(1).getSteps());
    }

    @Test
    void getDaily_AppleMissing_FallsBackToManualWithoutConflicts() {
        when(appleHealthStepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0L);
        when(appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0);

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals(200, response.getSteps().getDisplayedSteps());
        assertFalse(response.getSteps().getConflictFlags().isManualVsAppleMismatch());
        assertFalse(response.getSteps().getConflictFlags().isManualIgnoredForDisplay());
    }

    @Test
    void getDaily_InvalidTimezone_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dashboardDailyService.getDaily(USER_ID, DATE, "bad/timezone"));

        assertTrue(ex.getMessage().contains("Invalid timeZone"));
    }
}
