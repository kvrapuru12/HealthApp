package com.healthapp.service;

import com.healthapp.dto.applehealth.AppleHealthIngestRequest;
import com.healthapp.dto.applehealth.AppleHealthIngestResponse;
import com.healthapp.dto.applehealth.AppleHealthIngestSampleRequest;
import com.healthapp.dto.applehealth.AppleHealthIngestSampleResult;
import com.healthapp.entity.AppleHealthStepSample;
import com.healthapp.entity.User;
import com.healthapp.repository.AppleHealthStepSampleRepository;
import com.healthapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

@Service
public class AppleHealthIngestService {

    private static final int MAX_STEP_VALUE = 1_000_000;

    private final AppleHealthStepSampleRepository appleHealthStepSampleRepository;
    private final UserRepository userRepository;

    public AppleHealthIngestService(AppleHealthStepSampleRepository appleHealthStepSampleRepository,
                                    UserRepository userRepository) {
        this.appleHealthStepSampleRepository = appleHealthStepSampleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AppleHealthIngestResponse ingest(Long authenticatedUserId, AppleHealthIngestRequest request) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        Integer schemaVersion = request.getClientIngestSchemaVersion();
        if (schemaVersion == null) {
            throw new IllegalArgumentException("clientIngestSchemaVersion is required");
        }

        int clientSchemaVersion = schemaVersion;
        if (clientSchemaVersion != AppleHealthIngestRequest.SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported clientIngestSchemaVersion: " + request.getClientIngestSchemaVersion());
        }

        final ZoneId anchorZone;
        try {
            anchorZone = ZoneId.of(request.getAnchorTimeZone());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid anchorTimeZone: " + request.getAnchorTimeZone());
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<AppleHealthIngestSampleRequest> samples = request.getSamples() != null
                ? request.getSamples()
                : List.of();

        AppleHealthIngestResponse response = new AppleHealthIngestResponse();
        TreeSet<String> affectedDates = new TreeSet<>();

        for (AppleHealthIngestSampleRequest sample : samples) {
            AppleHealthIngestSampleResult row = processOneSample(user, sample, anchorZone, affectedDates, clientSchemaVersion);
            response.getResults().add(row);
            switch (row.getStatus()) {
                case UPSERTED -> response.setAccepted(response.getAccepted() + 1);
                case UNCHANGED -> response.setUnchanged(response.getUnchanged() + 1);
                case REJECTED -> response.setRejected(response.getRejected() + 1);
            }
        }

        response.setAffectedLocalDates(new ArrayList<>(affectedDates));
        return response;
    }

    private AppleHealthIngestSampleResult processOneSample(
            User user,
            AppleHealthIngestSampleRequest sample,
            ZoneId anchorZone,
            TreeSet<String> affectedDates,
            int clientSchemaVersion) {

        String extId = sample.getExternalSampleId();
        if (extId == null || extId.isBlank()) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "externalSampleId is required");
        }

        if (!"STEPS".equalsIgnoreCase(sample.getMetric())) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "Only metric STEPS is supported");
        }

        if (sample.getStart() == null || sample.getEnd() == null) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "start and end are required");
        }

        if (sample.getEnd().toInstant().isBefore(sample.getStart().toInstant())) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "end must not be before start");
        }

        if (sample.getValue() == null || sample.getValue() < 0 || sample.getValue() > MAX_STEP_VALUE) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "value must be between 0 and " + MAX_STEP_VALUE);
        }

        if (sample.getLocalDate() == null) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "sample.localDate is required for clientIngestSchemaVersion 2");
        }

        LocalDate localDate = sample.getLocalDate();

        LocalDate derivedFromEnd = sample.getEnd().atZoneSameInstant(anchorZone).toLocalDate();
        if (!localDate.equals(derivedFromEnd)) {
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.REJECTED,
                    "localDate must match end date in anchorTimeZone");
        }

        LocalDateTime startUtc = LocalDateTime.ofInstant(sample.getStart().toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofInstant(sample.getEnd().toInstant(), ZoneOffset.UTC);

        var existing = appleHealthStepSampleRepository.findByUserIdAndExternalSampleId(user.getId(), extId);
        if (existing.isPresent()) {
            AppleHealthStepSample e = existing.get();
            LocalDate oldLocalDate = e.getLocalDate();
            if (Objects.equals(e.getStepCount(), sample.getValue())
                    && Objects.equals(e.getLocalDate(), localDate)
                    && Objects.equals(e.getPeriodStartUtc(), startUtc)
                    && Objects.equals(e.getPeriodEndUtc(), endUtc)) {
                return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.UNCHANGED, null);
            }
            e.setLocalDate(localDate);
            e.setPeriodStartUtc(startUtc);
            e.setPeriodEndUtc(endUtc);
            e.setStepCount(sample.getValue());
            appleHealthStepSampleRepository.save(e);
            if (!Objects.equals(oldLocalDate, localDate)) {
                affectedDates.add(oldLocalDate.toString());
            }
            affectedDates.add(localDate.toString());
            return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.UPSERTED, null);
        }

        AppleHealthStepSample created = new AppleHealthStepSample(
                user, extId, localDate, startUtc, endUtc, sample.getValue());
        appleHealthStepSampleRepository.save(created);
        affectedDates.add(localDate.toString());
        return new AppleHealthIngestSampleResult(extId, AppleHealthIngestSampleResult.Status.UPSERTED, null);
    }
}
