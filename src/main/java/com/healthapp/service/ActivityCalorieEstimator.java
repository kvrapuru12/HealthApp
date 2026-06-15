package com.healthapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class ActivityCalorieEstimator {

    private ActivityCalorieEstimator() {}

    public static BigDecimal estimateCaloriesPerMinute(String activityName, String category) {
        String name = activityName != null ? activityName.toLowerCase(Locale.ROOT) : "";
        String cat = category != null ? category.toLowerCase(Locale.ROOT) : "general";

        if (name.contains("spin") || name.contains("indoor cycling")) {
            return bd(12.0);
        }
        if (name.contains("hiit") || name.contains("interval") || name.contains("tabata")
                || name.contains("crossfit") || name.contains("circuit")) {
            return bd(10.0);
        }
        if (name.contains("run") || name.contains("jog") || name.contains("sprint")) {
            return bd(10.0);
        }
        if (name.contains("uphill") || name.contains("backpack")
                || name.contains("hike") || name.contains("hiking")) {
            return bd(9.0);
        }
        if (name.contains("tennis") || name.contains("basketball") || name.contains("football")
                || name.contains("soccer")) {
            return bd(8.0);
        }
        if (name.contains("brisk") && name.contains("walk")) {
            return bd(5.0);
        }
        if (name.contains("walk")) {
            return bd(4.0);
        }
        if (name.contains("swim")) {
            return bd(8.0);
        }
        if (name.contains("row") || name.contains("rowing")) {
            return bd(8.0);
        }
        if (name.contains("badminton")) {
            return bd(8.0);
        }
        if (name.contains("bike") || name.contains("cycle") || name.contains("cycling")) {
            return bd(7.0);
        }
        if (name.contains("yoga") || name.contains("stretch") || name.contains("pilates")) {
            return bd(3.5);
        }
        if (name.contains("weight") || name.contains("lift") || name.contains("gym")) {
            return bd(6.0);
        }
        if (name.contains("climb")) {
            return bd(8.0);
        }
        if (name.contains("clean") || name.contains("garden") || name.contains("housework")) {
            return bd(3.0);
        }

        return switch (cat) {
            case "cardio" -> bd(5.0);
            case "sports" -> bd(8.0);
            case "strength" -> bd(6.0);
            case "flexibility" -> bd(3.5);
            case "outdoor" -> bd(7.0);
            case "home" -> bd(3.0);
            default -> bd(4.0);
        };
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
