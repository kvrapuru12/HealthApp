package com.healthapp.service;

import com.healthapp.dto.applehealth.AppleHealthIngestRequest;
import com.healthapp.dto.applehealth.AppleHealthIngestSampleRequest;
import com.healthapp.entity.AppleHealthSleepSample;
import com.healthapp.entity.AppleHealthStepSample;
import com.healthapp.entity.User;
import com.healthapp.repository.AppleHealthSleepSampleRepository;
import com.healthapp.repository.AppleHealthStepSampleRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AppleHealthIngestServiceTest {

    @Mock
    private AppleHealthStepSampleRepository appleHealthStepSampleRepository;

    @Mock
    private AppleHealthSleepSampleRepository appleHealthSleepSampleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppleHealthIngestService appleHealthIngestService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
    }

    @Test
    void ingest_NewSample_UpsertsAndReturnsAffectedDate() {
        var request = baseRequest(singleSample("ext-1", "2026-04-16T07:00:00Z", 8432));

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appleHealthStepSampleRepository.findByUserIdAndExternalSampleId(42L, "ext-1"))
                .thenReturn(Optional.empty());

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getUnchanged());
        assertEquals(0, response.getRejected());
        assertEquals(List.of("2026-04-15"), response.getAffectedLocalDates());
        assertEquals(1, response.getResults().size());
        assertEquals("UPSERTED", response.getResults().get(0).getStatus().name());
        ArgumentCaptor<AppleHealthStepSample> captor = ArgumentCaptor.forClass(AppleHealthStepSample.class);
        verify(appleHealthStepSampleRepository).save(captor.capture());
        assertNotNull(captor.getValue());
        assertEquals("ext-1", captor.getValue().getExternalSampleId());
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_UnchangedExistingSample_ReturnsUnchangedWithoutSave() {
        var sample = singleSample("ext-1", "2026-04-16T07:00:00Z", 8432);
        var request = baseRequest(sample);

        AppleHealthStepSample existing = new AppleHealthStepSample(
                user,
                "ext-1",
                LocalDate.of(2026, 4, 15),
                LocalDateTime.parse("2026-04-15T07:00:00"),
                LocalDateTime.parse("2026-04-16T07:00:00"),
                8432
        );

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appleHealthStepSampleRepository.findByUserIdAndExternalSampleId(42L, "ext-1"))
                .thenReturn(Optional.of(existing));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getUnchanged());
        assertEquals(0, response.getRejected());
        assertTrue(response.getAffectedLocalDates().isEmpty());
        verify(appleHealthStepSampleRepository, never()).save(any(AppleHealthStepSample.class));
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_DateShiftOnExistingSample_TracksOldAndNewAffectedDates() {
        var request = baseRequest(singleSample("ext-1", "2026-04-17T07:00:00Z", 9000));

        AppleHealthStepSample existing = new AppleHealthStepSample(
                user,
                "ext-1",
                LocalDate.of(2026, 4, 16),
                LocalDateTime.parse("2026-04-15T07:00:00"),
                LocalDateTime.parse("2026-04-16T07:00:00"),
                8432
        );

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appleHealthStepSampleRepository.findByUserIdAndExternalSampleId(42L, "ext-1"))
                .thenReturn(Optional.of(existing));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getAccepted());
        assertEquals(List.of("2026-04-15", "2026-04-16"), response.getAffectedLocalDates());
        verify(appleHealthStepSampleRepository).save(existing);
        assertEquals(LocalDate.of(2026, 4, 15), existing.getLocalDate());
        assertEquals(9000, existing.getStepCount());
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_UnsupportedVersion_Throws() {
        AppleHealthIngestRequest request = new AppleHealthIngestRequest();
        request.setClientIngestSchemaVersion(99);
        request.setAnchorTimeZone("America/Los_Angeles");
        request.setSamples(List.of(singleSample("ext-1", "2026-04-16T07:00:00Z", 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> appleHealthIngestService.ingest(42L, request));

        assertTrue(ex.getMessage().contains("Unsupported clientIngestSchemaVersion"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void ingest_V2MissingLocalDate_IsRejectedPerSample() {
        AppleHealthIngestSampleRequest sample = singleSample("ext-v2-missing-local-date", "2026-04-17T23:00:00Z", 289);
        sample.setLocalDate(null);

        AppleHealthIngestRequest request = new AppleHealthIngestRequest();
        request.setClientIngestSchemaVersion(2);
        request.setAnchorTimeZone("Europe/London");
        request.setSamples(List.of(sample));

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(0, response.getAccepted());
        assertEquals(0, response.getUnchanged());
        assertEquals(1, response.getRejected());
        assertEquals("REJECTED", response.getResults().get(0).getStatus().name());
        assertTrue(response.getResults().get(0).getMessage().contains("sample.localDate is required"));
        verifyNoInteractions(appleHealthStepSampleRepository);
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_V2WithLocalDateMatchingStartDateInAnchorZone_IsAccepted() {
        AppleHealthIngestSampleRequest sample = singleSample("ext-v2-local-date", "2026-04-17T23:00:00Z", 289);
        sample.setStart(OffsetDateTime.parse("2026-04-16T23:00:00Z"));
        sample.setLocalDate(LocalDate.of(2026, 4, 17));

        AppleHealthIngestRequest request = new AppleHealthIngestRequest();
        request.setClientIngestSchemaVersion(2);
        request.setAnchorTimeZone("Europe/London");
        request.setSamples(List.of(sample));

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appleHealthStepSampleRepository.findByUserIdAndExternalSampleId(42L, "ext-v2-local-date"))
                .thenReturn(Optional.empty());

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getAccepted());
        assertEquals(List.of("2026-04-17"), response.getAffectedLocalDates());

        ArgumentCaptor<AppleHealthStepSample> captor = ArgumentCaptor.forClass(AppleHealthStepSample.class);
        verify(appleHealthStepSampleRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 4, 17), captor.getValue().getLocalDate());
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_V2WithWildlyInconsistentLocalDate_IsRejectedPerSample() {
        AppleHealthIngestSampleRequest sample = singleSample("ext-v2-bad-date", "2026-04-17T23:00:00Z", 289);
        sample.setLocalDate(LocalDate.of(2026, 4, 25));

        AppleHealthIngestRequest request = new AppleHealthIngestRequest();
        request.setClientIngestSchemaVersion(2);
        request.setAnchorTimeZone("Europe/London");
        request.setSamples(List.of(sample));

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getRejected());
        assertEquals("REJECTED", response.getResults().get(0).getStatus().name());
        assertTrue(response.getResults().get(0).getMessage().contains("must match start date"));
        verifyNoInteractions(appleHealthStepSampleRepository);
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_SleepNew_UpsertsWakeDayLocalDate() {
        AppleHealthIngestSampleRequest sample = sleepSample(
                "sleep-ext-1",
                "2026-04-15T22:00:00Z",
                "2026-04-16T06:00:00Z",
                LocalDate.of(2026, 4, 16),
                "core");
        var request = baseRequest(sample);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appleHealthSleepSampleRepository.findByUserIdAndExternalSampleId(42L, "sleep-ext-1"))
                .thenReturn(Optional.empty());

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getAccepted());
        assertEquals(List.of("2026-04-16"), response.getAffectedLocalDates());
        ArgumentCaptor<AppleHealthSleepSample> captor = ArgumentCaptor.forClass(AppleHealthSleepSample.class);
        verify(appleHealthSleepSampleRepository).save(captor.capture());
        assertEquals("CORE", captor.getValue().getSleepStage());
        assertEquals(LocalDate.of(2026, 4, 16), captor.getValue().getLocalDate());
        verifyNoInteractions(appleHealthStepSampleRepository);
    }

    @Test
    void ingest_SleepLocalDateMustMatchEnd_NotStart() {
        AppleHealthIngestSampleRequest sample = sleepSample(
                "sleep-ext-2",
                "2026-04-15T22:00:00Z",
                "2026-04-16T06:00:00Z",
                LocalDate.of(2026, 4, 15),
                "deep");
        var request = baseRequest(sample);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getRejected());
        assertTrue(response.getResults().get(0).getMessage().contains("end date"));
        verifyNoInteractions(appleHealthSleepSampleRepository);
        verifyNoInteractions(appleHealthStepSampleRepository);
    }

    @Test
    void ingest_SleepMissingStage_Rejected() {
        AppleHealthIngestSampleRequest sample = sleepSample(
                "sleep-ext-3",
                "2026-04-15T22:00:00Z",
                "2026-04-16T06:00:00Z",
                LocalDate.of(2026, 4, 16),
                null);
        var request = baseRequest(sample);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getRejected());
        assertTrue(response.getResults().get(0).getMessage().contains("sleepStage"));
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_SleepInvalidStage_Rejected() {
        AppleHealthIngestSampleRequest sample = sleepSample(
                "sleep-ext-4",
                "2026-04-15T22:00:00Z",
                "2026-04-16T06:00:00Z",
                LocalDate.of(2026, 4, 16),
                "UNKNOWN_STAGE");
        var request = baseRequest(sample);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getRejected());
        assertTrue(response.getResults().get(0).getMessage().contains("sleepStage"));
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_SleepUnchanged_NoSave() {
        AppleHealthIngestSampleRequest sample = sleepSample(
                "sleep-ext-5",
                "2026-04-15T22:00:00Z",
                "2026-04-16T06:00:00Z",
                LocalDate.of(2026, 4, 16),
                "rem");
        var request = baseRequest(sample);

        AppleHealthSleepSample existing = new AppleHealthSleepSample(
                user,
                "sleep-ext-5",
                LocalDate.of(2026, 4, 16),
                LocalDateTime.parse("2026-04-15T22:00:00"),
                LocalDateTime.parse("2026-04-16T06:00:00"),
                "REM"
        );

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(appleHealthSleepSampleRepository.findByUserIdAndExternalSampleId(42L, "sleep-ext-5"))
                .thenReturn(Optional.of(existing));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getUnchanged());
        verify(appleHealthSleepSampleRepository, never()).save(any(AppleHealthSleepSample.class));
        verifyNoInteractions(appleHealthStepSampleRepository);
    }

    @Test
    void ingest_UnknownMetric_Rejected() {
        AppleHealthIngestSampleRequest sample = singleSample("x-1", "2026-04-16T07:00:00Z", 100);
        sample.setMetric("DISTANCE");
        var request = baseRequest(sample);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getRejected());
        assertTrue(response.getResults().get(0).getMessage().contains("STEPS and SLEEP"));
        verifyNoInteractions(appleHealthStepSampleRepository);
        verifyNoInteractions(appleHealthSleepSampleRepository);
    }

    @Test
    void ingest_StepsMissingValue_Rejected() {
        AppleHealthIngestSampleRequest sample = singleSample("ext-null-val", "2026-04-16T07:00:00Z", 0);
        sample.setValue(null);
        var request = baseRequest(sample);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var response = appleHealthIngestService.ingest(42L, request);

        assertEquals(1, response.getRejected());
        assertTrue(response.getResults().get(0).getMessage().contains("value"));
        verifyNoInteractions(appleHealthStepSampleRepository);
    }

    private AppleHealthIngestRequest baseRequest(AppleHealthIngestSampleRequest sample) {
        AppleHealthIngestRequest request = new AppleHealthIngestRequest();
        request.setClientIngestSchemaVersion(2);
        request.setAnchorTimeZone("UTC");
        request.setSamples(List.of(sample));
        return request;
    }

    private AppleHealthIngestSampleRequest singleSample(String extId, String endIso, int value) {
        AppleHealthIngestSampleRequest sample = new AppleHealthIngestSampleRequest();
        sample.setMetric("STEPS");
        sample.setExternalSampleId(extId);
        sample.setStart(OffsetDateTime.parse("2026-04-15T07:00:00Z"));
        sample.setEnd(OffsetDateTime.parse(endIso));
        sample.setLocalDate(sample.getStart().toLocalDate());
        sample.setValue(value);
        return sample;
    }

    private AppleHealthIngestSampleRequest sleepSample(
            String extId,
            String startIso,
            String endIso,
            LocalDate localDate,
            String sleepStage) {
        AppleHealthIngestSampleRequest sample = new AppleHealthIngestSampleRequest();
        sample.setMetric("SLEEP");
        sample.setExternalSampleId(extId);
        sample.setStart(OffsetDateTime.parse(startIso));
        sample.setEnd(OffsetDateTime.parse(endIso));
        sample.setLocalDate(localDate);
        sample.setSleepStage(sleepStage);
        return sample;
    }
}
