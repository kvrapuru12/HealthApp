package com.healthapp.service;

import com.healthapp.entity.StepEntry;
import com.healthapp.repository.AppleHealthSleepSampleRepository;
import com.healthapp.repository.AppleHealthStepSampleRepository;
import com.healthapp.repository.SleepEntryRepository;
import com.healthapp.repository.StepEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardDailyServiceTest {

    @Mock
    private AppleHealthStepSampleRepository appleHealthStepSampleRepository;

    @Mock
    private AppleHealthSleepSampleRepository appleHealthSleepSampleRepository;

    @Mock
    private StepEntryRepository stepEntryRepository;

    @Mock
    private SleepEntryRepository sleepEntryRepository;

    @InjectMocks
    private DashboardDailyService dashboardDailyService;

    private static final Long USER_ID = 42L;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 16);

    @Test
    void getDaily_BothStepSourcesPresent_SumsAndMarksResolvedSourceBoth() {
        when(stepEntryRepository.sumStepCountByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(StepEntry.Status.ACTIVE)))
                .thenReturn(200);
        when(appleHealthStepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(1L);
        when(appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(8432);

        stubSleepEmpty();

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals("SUM_WHEN_BOTH_PRESENT", response.getSteps().getMergePolicy());
        assertEquals(8632, response.getSteps().getDisplayedSteps());
        assertEquals("BOTH", response.getSteps().getResolvedSource());
        assertTrue(response.getSteps().getConflictFlags().isManualVsAppleMismatch());
        assertFalse(response.getSteps().getConflictFlags().isManualIgnoredForDisplay());
        assertEquals(2, response.getSteps().getBySource().size());
        assertEquals("APPLE_HEALTH", response.getSteps().getBySource().get(0).getSource());
        assertEquals(8432, response.getSteps().getBySource().get(0).getSteps());
        assertEquals("MANUAL_APP", response.getSteps().getBySource().get(1).getSource());
        assertEquals(200, response.getSteps().getBySource().get(1).getSteps());
    }

    @Test
    void getDaily_AppleMissing_FallsBackToManualWithoutConflicts() {
        when(stepEntryRepository.sumStepCountByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(StepEntry.Status.ACTIVE)))
                .thenReturn(200);
        when(appleHealthStepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0L);
        when(appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0);

        stubSleepEmpty();

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals(200, response.getSteps().getDisplayedSteps());
        assertEquals("MANUAL_APP", response.getSteps().getResolvedSource());
        assertFalse(response.getSteps().getConflictFlags().isManualVsAppleMismatch());
        assertFalse(response.getSteps().getConflictFlags().isManualIgnoredForDisplay());
    }

    @Test
    void getDaily_OnlyAppleStepsPresent_UsesAppleAndMarksResolvedSourceApple() {
        when(stepEntryRepository.sumStepCountByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(StepEntry.Status.ACTIVE)))
                .thenReturn(0);
        when(appleHealthStepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(1L);
        when(appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(8432);

        stubSleepEmpty();

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals(8432, response.getSteps().getDisplayedSteps());
        assertEquals("APPLE_HEALTH", response.getSteps().getResolvedSource());
        assertFalse(response.getSteps().getConflictFlags().isManualIgnoredForDisplay());
    }

    @Test
    void getDaily_BothSleepSourcesPresent_SumsAndMarksResolvedSourceBoth() {
        stubStepsEmpty();

        when(sleepEntryRepository.sumHoursByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(new BigDecimal("7.0")));

        when(appleHealthSleepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(3L);
        when(appleHealthSleepSampleRepository.sumAsleepSecondsByUserIdAndLocalDate(USER_ID, DATE))
                .thenReturn(BigInteger.valueOf(8 * 3600));

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals(new BigDecimal("15.00"), response.getSleep().getDisplayedSleepHours());
        assertEquals("BOTH", response.getSleep().getResolvedSource());
        assertTrue(response.getSleep().getConflictFlags().isManualVsAppleMismatch());
        assertFalse(response.getSleep().getConflictFlags().isManualIgnoredForDisplay());
        assertEquals(new BigDecimal("8.00"), response.getSleep().getBySource().get(0).getHours());
        assertEquals(new BigDecimal("7.00"), response.getSleep().getBySource().get(1).getHours());
    }

    @Test
    void getDaily_AppleSleepMissing_UsesManualSleepHours() {
        stubStepsEmpty();

        when(sleepEntryRepository.sumHoursByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(new BigDecimal("6.5")));

        when(appleHealthSleepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0L);
        when(appleHealthSleepSampleRepository.sumAsleepSecondsByUserIdAndLocalDate(USER_ID, DATE))
                .thenReturn(BigInteger.ZERO);

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals(new BigDecimal("6.50"), response.getSleep().getDisplayedSleepHours());
        assertEquals("MANUAL_APP", response.getSleep().getResolvedSource());
        assertFalse(response.getSleep().getConflictFlags().isManualVsAppleMismatch());
    }

    @Test
    void getDaily_OnlyAppleSleepPresent_UsesAppleAndMarksResolvedSourceApple() {
        stubStepsEmpty();

        when(sleepEntryRepository.sumHoursByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(BigDecimal.ZERO));

        when(appleHealthSleepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(3L);
        when(appleHealthSleepSampleRepository.sumAsleepSecondsByUserIdAndLocalDate(USER_ID, DATE))
                .thenReturn(BigInteger.valueOf(8 * 3600));

        var response = dashboardDailyService.getDaily(USER_ID, DATE, "UTC");

        assertEquals(new BigDecimal("8.00"), response.getSleep().getDisplayedSleepHours());
        assertEquals("APPLE_HEALTH", response.getSleep().getResolvedSource());
        assertFalse(response.getSleep().getConflictFlags().isManualIgnoredForDisplay());
    }

    @Test
    void getDaily_InvalidTimezone_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dashboardDailyService.getDaily(USER_ID, DATE, "bad/timezone"));

        assertTrue(ex.getMessage().contains("Invalid timeZone"));
    }

    private void stubSleepEmpty() {
        when(appleHealthSleepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0L);
        when(appleHealthSleepSampleRepository.sumAsleepSecondsByUserIdAndLocalDate(USER_ID, DATE))
                .thenReturn(BigInteger.ZERO);
        when(sleepEntryRepository.sumHoursByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
    }

    private void stubStepsEmpty() {
        when(stepEntryRepository.sumStepCountByUserIdAndDateRangeHalfOpen(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(StepEntry.Status.ACTIVE)))
                .thenReturn(0);
        when(appleHealthStepSampleRepository.countByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0L);
        when(appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(USER_ID, DATE)).thenReturn(0);
    }
}
