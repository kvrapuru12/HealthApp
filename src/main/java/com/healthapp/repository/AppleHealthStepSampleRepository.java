package com.healthapp.repository;

import com.healthapp.entity.AppleHealthStepSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AppleHealthStepSampleRepository extends JpaRepository<AppleHealthStepSample, Long> {

    @Query("SELECT a FROM AppleHealthStepSample a WHERE a.user.id = :userId AND a.externalSampleId = :externalSampleId")
    Optional<AppleHealthStepSample> findByUserIdAndExternalSampleId(
            @Param("userId") Long userId,
            @Param("externalSampleId") String externalSampleId);

    @Query("SELECT COALESCE(SUM(a.stepCount), 0) FROM AppleHealthStepSample a WHERE a.user.id = :userId AND a.localDate = :localDate")
    Integer sumStepCountByUserIdAndLocalDate(@Param("userId") Long userId, @Param("localDate") LocalDate localDate);

    @Query("SELECT COUNT(a) FROM AppleHealthStepSample a WHERE a.user.id = :userId AND a.localDate = :localDate")
    long countByUserIdAndLocalDate(@Param("userId") Long userId, @Param("localDate") LocalDate localDate);
}
