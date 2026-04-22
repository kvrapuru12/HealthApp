package com.healthapp.service;

import com.healthapp.dto.applehealth.DashboardDailyConflictFlags;
import com.healthapp.dto.applehealth.DashboardDailyResponse;
import com.healthapp.dto.applehealth.DashboardDailySourceSleep;
import com.healthapp.dto.applehealth.DashboardDailySourceSteps;
import com.healthapp.dto.applehealth.DashboardDailySleepSection;
import com.healthapp.dto.applehealth.DashboardDailyStepsSection;
import com.healthapp.entity.SleepEntry;
import com.healthapp.entity.StepEntry;
import com.healthapp.repository.AppleHealthSleepSampleRepository;
import com.healthapp.repository.AppleHealthStepSampleRepository;
import com.healthapp.repository.SleepEntryRepository;
import com.healthapp.repository.StepEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DashboardDailyService {

    public static final String MERGE_POLICY = "SUM_WHEN_BOTH_PRESENT";
    private static final String SOURCE_NONE = "NONE";
    private static final String SOURCE_APPLE = "APPLE_HEALTH";
    private static final String SOURCE_MANUAL = "MANUAL_APP";
    private static final String SOURCE_BOTH = "BOTH";

    private static final int SLEEP_HOURS_SCALE = 2;

    private final AppleHealthStepSampleRepository appleHealthStepSampleRepository;
    private final AppleHealthSleepSampleRepository appleHealthSleepSampleRepository;
    private final StepEntryRepository stepEntryRepository;
    private final SleepEntryRepository sleepEntryRepository;

    public DashboardDailyService(AppleHealthStepSampleRepository appleHealthStepSampleRepository,
                                 AppleHealthSleepSampleRepository appleHealthSleepSampleRepository,
                                 StepEntryRepository stepEntryRepository,
                                 SleepEntryRepository sleepEntryRepository) {
        this.appleHealthStepSampleRepository = appleHealthStepSampleRepository;
        this.appleHealthSleepSampleRepository = appleHealthSleepSampleRepository;
        this.stepEntryRepository = stepEntryRepository;
        this.sleepEntryRepository = sleepEntryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDailyResponse getDaily(Long userId, LocalDate localDate, String timeZoneId) {
        final ZoneId zone;
        try {
            zone = ZoneId.of(timeZoneId);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timeZone: " + timeZoneId);
        }

        LocalDateTime fromUtc = LocalDateTime.ofInstant(localDate.atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
        LocalDateTime toUtcExclusive = LocalDateTime.ofInstant(
                localDate.plusDays(1).atStartOfDay(zone).toInstant(), ZoneOffset.UTC);

        int manualSteps = Objects.requireNonNullElse(
                stepEntryRepository.sumStepCountByUserIdAndDateRangeHalfOpen(
                        userId, fromUtc, toUtcExclusive, StepEntry.Status.ACTIVE),
                0);

        long appleRowCount = appleHealthStepSampleRepository.countByUserIdAndLocalDate(userId, localDate);
        int appleSteps = Objects.requireNonNullElse(
                appleHealthStepSampleRepository.sumStepCountByUserIdAndLocalDate(userId, localDate),
                0);

        boolean appleHasData = appleRowCount > 0;
        boolean manualHasData = manualSteps > 0;
        int displayed = appleHasData && manualHasData
                ? appleSteps + manualSteps
                : (appleHasData ? appleSteps : manualSteps);
        String resolvedStepsSource = resolveSource(appleHasData, manualHasData);

        boolean mismatch = appleHasData && manualHasData && appleSteps != manualSteps;
        boolean manualIgnored = false;

        DashboardDailyResponse response = new DashboardDailyResponse();
        response.setLocalDate(localDate.toString());
        response.setTimeZone(timeZoneId);
        response.setGeneratedAt(OffsetDateTime.now());

        DashboardDailyStepsSection steps = new DashboardDailyStepsSection();
        steps.setMergePolicy(MERGE_POLICY);
        steps.setDisplayedSteps(displayed);
        steps.setResolvedSource(resolvedStepsSource);

        List<DashboardDailySourceSteps> bySource = new ArrayList<>();
        bySource.add(new DashboardDailySourceSteps(SOURCE_APPLE, appleSteps));
        bySource.add(new DashboardDailySourceSteps(SOURCE_MANUAL, manualSteps));
        steps.setBySource(bySource);

        steps.setConflictFlags(new DashboardDailyConflictFlags(mismatch, manualIgnored));
        response.setSteps(steps);

        BigDecimal manualSleepHours = sleepEntryRepository
                .sumHoursByUserIdAndDateRangeHalfOpen(userId, fromUtc, toUtcExclusive, SleepEntry.Status.ACTIVE)
                .orElse(BigDecimal.ZERO)
                .setScale(SLEEP_HOURS_SCALE, RoundingMode.HALF_UP);

        long appleSleepRowCount = appleHealthSleepSampleRepository.countByUserIdAndLocalDate(userId, localDate);
        BigInteger asleepSecondsBi = appleHealthSleepSampleRepository.sumAsleepSecondsByUserIdAndLocalDate(userId, localDate);
        long asleepSeconds = asleepSecondsBi != null ? asleepSecondsBi.longValue() : 0L;
        BigDecimal appleSleepHours = BigDecimal.valueOf(asleepSeconds)
                .divide(BigDecimal.valueOf(3600), SLEEP_HOURS_SCALE + 2, RoundingMode.HALF_UP)
                .setScale(SLEEP_HOURS_SCALE, RoundingMode.HALF_UP);

        boolean appleSleepPresent = appleSleepRowCount > 0;
        boolean manualSleepPresent = manualSleepHours.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal displayedSleepHours = appleSleepPresent && manualSleepPresent
                ? appleSleepHours.add(manualSleepHours).setScale(SLEEP_HOURS_SCALE, RoundingMode.HALF_UP)
                : (appleSleepPresent ? appleSleepHours : manualSleepHours);
        String resolvedSleepSource = resolveSource(appleSleepPresent, manualSleepPresent);

        boolean sleepMismatch = appleSleepPresent
                && manualSleepPresent
                && manualSleepHours.compareTo(appleSleepHours) != 0;
        boolean sleepManualIgnored = false;

        DashboardDailySleepSection sleep = new DashboardDailySleepSection();
        sleep.setMergePolicy(MERGE_POLICY);
        sleep.setDisplayedSleepHours(displayedSleepHours);
        sleep.setResolvedSource(resolvedSleepSource);

        List<DashboardDailySourceSleep> sleepBySource = new ArrayList<>();
        sleepBySource.add(new DashboardDailySourceSleep(SOURCE_APPLE, appleSleepHours));
        sleepBySource.add(new DashboardDailySourceSleep(SOURCE_MANUAL, manualSleepHours));
        sleep.setBySource(sleepBySource);
        sleep.setConflictFlags(new DashboardDailyConflictFlags(sleepMismatch, sleepManualIgnored));
        response.setSleep(sleep);

        return response;
    }

    private static String resolveSource(boolean applePresent, boolean manualPresent) {
        if (applePresent && manualPresent) {
            return SOURCE_BOTH;
        }
        if (applePresent) {
            return SOURCE_APPLE;
        }
        if (manualPresent) {
            return SOURCE_MANUAL;
        }
        return SOURCE_NONE;
    }
}
