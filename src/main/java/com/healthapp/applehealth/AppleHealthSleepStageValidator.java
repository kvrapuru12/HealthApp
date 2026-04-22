package com.healthapp.applehealth;

import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Normalized sleep stage strings from clients (HealthKit sleep analysis mapped by the iOS app).
 */
public final class AppleHealthSleepStageValidator {

    private static final Map<String, String> STAGE_ALIASES = Map.of(
            "ASLEEP_REM", "REM",
            "ASLEEP_CORE", "CORE",
            "ASLEEP_DEEP", "DEEP",
            "ASLEEP_UNSPECIFIED", "ASLEEP_UNSPECIFIED"
    );

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

    public static String normalize(String rawStage) {
        if (rawStage == null || rawStage.isBlank()) {
            return null;
        }
        String upper = rawStage.trim().toUpperCase(Locale.ROOT);
        String canonical = STAGE_ALIASES.getOrDefault(upper, upper);
        return ALLOWED.contains(canonical) ? canonical : null;
    }
}
