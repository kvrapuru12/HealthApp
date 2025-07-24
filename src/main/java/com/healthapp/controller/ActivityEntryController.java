package com.healthapp.controller;

import com.healthapp.entity.ActivityEntry;
import com.healthapp.service.ActivityEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/activity-entries")
@Tag(name = "Activity Entry Management", description = "APIs for managing activity entries and calorie burn tracking")
@CrossOrigin(origins = "*")
public class ActivityEntryController {
    
    @Autowired
    private ActivityEntryService activityEntryService;
    
    @GetMapping
    @Operation(summary = "Get all activity entries", description = "Retrieve a list of all activity entries")
    public ResponseEntity<List<ActivityEntry>> getAllActivityEntries() {
        List<ActivityEntry> activityEntries = activityEntryService.getAllActivityEntries();
        return ResponseEntity.ok(activityEntries);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get activity entry by ID", description = "Retrieve a specific activity entry by its ID")
    public ResponseEntity<ActivityEntry> getActivityEntryById(@PathVariable Long id) {
        return activityEntryService.getActivityEntryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get activity entries by user", description = "Retrieve all activity entries for a specific user")
    public ResponseEntity<List<ActivityEntry>> getActivityEntriesByUserId(@PathVariable Long userId) {
        List<ActivityEntry> activityEntries = activityEntryService.getActivityEntriesByUserId(userId);
        return ResponseEntity.ok(activityEntries);
    }
    
    @GetMapping("/user/{userId}/date/{date}")
    @Operation(summary = "Get activity entries by user and date", description = "Retrieve activity entries for a specific user on a specific date")
    public ResponseEntity<List<ActivityEntry>> getActivityEntriesByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ActivityEntry> activityEntries = activityEntryService.getActivityEntriesByUserAndDate(userId, date);
        return ResponseEntity.ok(activityEntries);
    }
    
    @PostMapping
    @Operation(summary = "Create a new activity entry", description = "Create a new activity entry for calorie burn tracking")
    public ResponseEntity<ActivityEntry> createActivityEntry(@RequestBody ActivityEntry activityEntry) {
        try {
            ActivityEntry savedActivityEntry = activityEntryService.createActivityEntry(activityEntry);
            return ResponseEntity.ok(savedActivityEntry);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update activity entry", description = "Update an existing activity entry")
    public ResponseEntity<ActivityEntry> updateActivityEntry(@PathVariable Long id, @RequestBody ActivityEntry activityEntryDetails) {
        try {
            ActivityEntry updatedActivityEntry = activityEntryService.updateActivityEntry(id, activityEntryDetails);
            return ResponseEntity.ok(updatedActivityEntry);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activity entry", description = "Delete an activity entry")
    public ResponseEntity<?> deleteActivityEntry(@PathVariable Long id) {
        try {
            activityEntryService.deleteActivityEntry(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}/calories-burned/{date}")
    @Operation(summary = "Get total calories burned by user and date", description = "Get total calories burned by a user on a specific date")
    public ResponseEntity<Integer> getTotalCaloriesBurnedByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Integer totalCaloriesBurned = activityEntryService.getTotalCaloriesBurnedByUserAndDate(userId, date);
        return ResponseEntity.ok(totalCaloriesBurned);
    }
    
    @GetMapping("/user/{userId}/duration/{date}")
    @Operation(summary = "Get total activity duration by user and date", description = "Get total activity duration in minutes for a user on a specific date")
    public ResponseEntity<Integer> getTotalDurationByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Integer totalDuration = activityEntryService.getTotalDurationByUserAndDate(userId, date);
        return ResponseEntity.ok(totalDuration);
    }
    
    @GetMapping("/user/{userId}/steps/{date}")
    @Operation(summary = "Get total steps by user and date", description = "Get total steps for a user on a specific date")
    public ResponseEntity<Integer> getTotalStepsByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Integer totalSteps = activityEntryService.getTotalStepsByUserAndDate(userId, date);
        return ResponseEntity.ok(totalSteps);
    }
    
    @PostMapping("/calculate-calories")
    @Operation(summary = "Calculate calories burned", description = "Calculate calories burned for an activity based on type, duration, weight, and intensity")
    public ResponseEntity<Integer> calculateCaloriesBurned(
            @RequestParam String activityType,
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) Double weightKg,
            @RequestParam String intensityLevel) {
        try {
            Integer caloriesBurned = activityEntryService.calculateCaloriesBurned(activityType, durationMinutes, weightKg, intensityLevel);
            return ResponseEntity.ok(caloriesBurned);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 