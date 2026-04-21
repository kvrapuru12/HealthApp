package com.healthapp.applehealth;

import java.util.Set;

/**
 * Normalized sleep stage strings from clients (HealthKit sleep analysis mapped by the iOS app).
 */
public final class AppleHealthSleepStageValidator {

    private static final Set<String> ALLOWED = Set.of(
            "AWAKE",
            "IN_BED",
            "ASLEEP",
            "ASLEEP_UNSPECIFIED",
            "CORE",
            "DEEP",
            "REM"
    );

    private AppleHealthSleepStageValidator() {
    }

    public static boolean isAllowed(String normalizedUpperStage) {
        return normalizedUpperStage != null && ALLOWED.contains(normalizedUpperStage);
    }
}
