package com.healthapp.service;

import com.healthapp.dto.applehealth.DashboardDailyConflictFlags;
import com.healthapp.dto.applehealth.DashboardDailyResponse;
import com.healthapp.dto.applehealth.DashboardDailySourceSteps;
import com.healthapp.dto.applehealth.DashboardDailyStepsSection;
import com.healthapp.entity.StepEntry;
import com.healthapp.repository.AppleHealthStepSampleRepository;
import com.healthapp.repository.StepEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public static final String MERGE_POLICY = "PREFER_APPLE_HEALTH_IF_PRESENT";

    private final AppleHealthStepSampleRepository appleHealthStepSampleRepository;
    private final StepEntryRepository stepEntryRepository;

    public DashboardDailyService(AppleHealthStepSampleRepository appleHealthStepSampleRepository,
                                   StepEntryRepository stepEntryRepository) {
        this.appleHealthStepSampleRepository = appleHealthStepSampleRepository;
        this.stepEntryRepository = stepEntryRepository;
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
        int displayed = appleHasData ? appleSteps : manualSteps;

        boolean mismatch = appleHasData && manualSteps > 0 && appleSteps != manualSteps;
        boolean manualIgnored = appleHasData && manualSteps > 0 && appleSteps != manualSteps;

        DashboardDailyResponse response = new DashboardDailyResponse();
        response.setLocalDate(localDate.toString());
        response.setTimeZone(timeZoneId);
        response.setGeneratedAt(OffsetDateTime.now());

        DashboardDailyStepsSection steps = new DashboardDailyStepsSection();
        steps.setMergePolicy(MERGE_POLICY);
        steps.setDisplayedSteps(displayed);

        List<DashboardDailySourceSteps> bySource = new ArrayList<>();
        bySource.add(new DashboardDailySourceSteps("APPLE_HEALTH", appleSteps));
        bySource.add(new DashboardDailySourceSteps("MANUAL_APP", manualSteps));
        steps.setBySource(bySource);

        steps.setConflictFlags(new DashboardDailyConflictFlags(mismatch, manualIgnored));
        response.setSteps(steps);

        return response;
    }
}
