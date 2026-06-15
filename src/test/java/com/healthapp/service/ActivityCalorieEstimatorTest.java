package com.healthapp.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityCalorieEstimatorTest {

    @Test
    void walk30MinutesReasonableBurn() {
        BigDecimal cpm = ActivityCalorieEstimator.estimateCaloriesPerMinute("morning walk", "cardio");
        assertEquals(new BigDecimal("4.00"), cpm);
        assertEquals(120.0, cpm.doubleValue() * 30, 0.01);
    }

    @Test
    void run30MinutesHigherBurn() {
        BigDecimal cpm = ActivityCalorieEstimator.estimateCaloriesPerMinute("easy run", "cardio");
        assertEquals(new BigDecimal("10.00"), cpm);
        assertEquals(300.0, cpm.doubleValue() * 30, 0.01);
    }

    @Test
    void briskWalkHigherThanCasualWalk() {
        BigDecimal brisk = ActivityCalorieEstimator.estimateCaloriesPerMinute("brisk walk", "cardio");
        BigDecimal walk = ActivityCalorieEstimator.estimateCaloriesPerMinute("walk", "cardio");
        assertEquals(true, brisk.compareTo(walk) > 0);
    }

    @Test
    void hiit30MinutesHighBurn() {
        BigDecimal cpm = ActivityCalorieEstimator.estimateCaloriesPerMinute("HIIT workout", "cardio");
        assertEquals(new BigDecimal("10.00"), cpm);
        assertEquals(300.0, cpm.doubleValue() * 30, 0.01);
    }

    @Test
    void spinClassHigherThanCasualCycling() {
        BigDecimal spin = ActivityCalorieEstimator.estimateCaloriesPerMinute("spin class", "cardio");
        BigDecimal cycle = ActivityCalorieEstimator.estimateCaloriesPerMinute("cycling", "cardio");
        assertEquals(new BigDecimal("12.00"), spin);
        assertEquals(true, spin.compareTo(cycle) > 0);
        assertEquals(540.0, spin.doubleValue() * 45, 0.01);
    }

    @Test
    void uphillHikeHigherThanCasualWalk() {
        BigDecimal hike = ActivityCalorieEstimator.estimateCaloriesPerMinute("walking uphill with backpack", "outdoor");
        BigDecimal walk = ActivityCalorieEstimator.estimateCaloriesPerMinute("walk", "cardio");
        assertEquals(new BigDecimal("9.00"), hike);
        assertEquals(true, hike.compareTo(walk) > 0);
        assertEquals(810.0, hike.doubleValue() * 90, 0.01);
    }
}
