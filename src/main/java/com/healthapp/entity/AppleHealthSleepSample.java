package com.healthapp.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apple_health_sleep_samples", indexes = {
        @Index(name = "idx_apple_health_sleep_user_local_date", columnList = "user_id, local_date")
})
@EntityListeners(AuditingEntityListener.class)
public class AppleHealthSleepSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "external_sample_id", nullable = false, length = 512)
    private String externalSampleId;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Column(name = "period_start_utc", nullable = false)
    private LocalDateTime periodStartUtc;

    @Column(name = "period_end_utc", nullable = false)
    private LocalDateTime periodEndUtc;

    @Column(name = "sleep_stage", nullable = false, length = 32)
    private String sleepStage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AppleHealthSleepSample() {
    }

    public AppleHealthSleepSample(User user, String externalSampleId, LocalDate localDate,
                                  LocalDateTime periodStartUtc, LocalDateTime periodEndUtc, String sleepStage) {
        this.user = user;
        this.externalSampleId = externalSampleId;
        this.localDate = localDate;
        this.periodStartUtc = periodStartUtc;
        this.periodEndUtc = periodEndUtc;
        this.sleepStage = sleepStage;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getExternalSampleId() {
        return externalSampleId;
    }

    public void setExternalSampleId(String externalSampleId) {
        this.externalSampleId = externalSampleId;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public LocalDateTime getPeriodStartUtc() {
        return periodStartUtc;
    }

    public void setPeriodStartUtc(LocalDateTime periodStartUtc) {
        this.periodStartUtc = periodStartUtc;
    }

    public LocalDateTime getPeriodEndUtc() {
        return periodEndUtc;
    }

    public void setPeriodEndUtc(LocalDateTime periodEndUtc) {
        this.periodEndUtc = periodEndUtc;
    }

    public String getSleepStage() {
        return sleepStage;
    }

    public void setSleepStage(String sleepStage) {
        this.sleepStage = sleepStage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
