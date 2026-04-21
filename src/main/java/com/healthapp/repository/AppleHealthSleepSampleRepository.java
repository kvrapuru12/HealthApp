package com.healthapp.repository;

import com.healthapp.entity.AppleHealthSleepSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Optional;

public interface AppleHealthSleepSampleRepository extends JpaRepository<AppleHealthSleepSample, Long> {

    @Query("SELECT a FROM AppleHealthSleepSample a WHERE a.user.id = :userId AND a.externalSampleId = :externalSampleId")
    Optional<AppleHealthSleepSample> findByUserIdAndExternalSampleId(
            @Param("userId") Long userId,
            @Param("externalSampleId") String externalSampleId);

    /**
     * Total asleep duration in whole seconds for stages counted as sleep (excludes AWAKE, IN_BED).
     * Stages: ASLEEP, ASLEEP_UNSPECIFIED, CORE, DEEP, REM (case-insensitive in SQL).
     * Native query: MySQL {@code TIMESTAMPDIFF}; not for H2 or other dialects without an equivalent.
     */
    @Query(value = """
            SELECT COALESCE(SUM(TIMESTAMPDIFF(MICROSECOND, period_start_utc, period_end_utc) DIV 1000000), 0)
            FROM apple_health_sleep_samples
            WHERE user_id = :userId AND local_date = :localDate
            AND UPPER(sleep_stage) IN ('ASLEEP', 'ASLEEP_UNSPECIFIED', 'CORE', 'DEEP', 'REM')
            """, nativeQuery = true)
    BigInteger sumAsleepSecondsByUserIdAndLocalDate(@Param("userId") Long userId, @Param("localDate") LocalDate localDate);

    @Query("SELECT COUNT(a) FROM AppleHealthSleepSample a WHERE a.user.id = :userId AND a.localDate = :localDate")
    long countByUserIdAndLocalDate(@Param("userId") Long userId, @Param("localDate") LocalDate localDate);
}
