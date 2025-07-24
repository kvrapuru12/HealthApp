package com.healthapp.service;

import com.healthapp.entity.ActivityEntry;
import com.healthapp.repository.ActivityEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityEntryService {
    
    @Autowired
    private ActivityEntryRepository activityEntryRepository;
    
    public List<ActivityEntry> getAllActivityEntries() {
        return activityEntryRepository.findAll();
    }
    
    public Optional<ActivityEntry> getActivityEntryById(Long id) {
        return activityEntryRepository.findById(id);
    }
    
    public List<ActivityEntry> getActivityEntriesByUserId(Long userId) {
        return activityEntryRepository.findByUserId(userId);
    }
    
    public List<ActivityEntry> getActivityEntriesByUserAndDate(Long userId, LocalDate date) {
        return activityEntryRepository.findByUserIdAndActivityDate(userId, date);
    }
    
    public ActivityEntry createActivityEntry(ActivityEntry activityEntry) {
        if (activityEntry.getActivityDate() == null) {
            activityEntry.setActivityDate(LocalDate.now());
        }
        return activityEntryRepository.save(activityEntry);
    }
    
    public ActivityEntry updateActivityEntry(Long id, ActivityEntry activityEntryDetails) {
        return activityEntryRepository.findById(id)
                .map(activityEntry -> {
                    activityEntry.setActivityName(activityEntryDetails.getActivityName());
                    activityEntry.setCaloriesBurned(activityEntryDetails.getCaloriesBurned());
                    activityEntry.setDurationMinutes(activityEntryDetails.getDurationMinutes());
                    activityEntry.setActivityType(activityEntryDetails.getActivityType());
                    activityEntry.setIntensityLevel(activityEntryDetails.getIntensityLevel());
                    activityEntry.setActivityDate(activityEntryDetails.getActivityDate());
                    activityEntry.setStartTime(activityEntryDetails.getStartTime());
                    activityEntry.setEndTime(activityEntryDetails.getEndTime());
                    activityEntry.setDistanceKm(activityEntryDetails.getDistanceKm());
                    activityEntry.setSteps(activityEntryDetails.getSteps());
                    activityEntry.setNotes(activityEntryDetails.getNotes());
                    return activityEntryRepository.save(activityEntry);
                })
                .orElseThrow(() -> new RuntimeException("Activity entry not found"));
    }
    
    public void deleteActivityEntry(Long id) {
        if (!activityEntryRepository.existsById(id)) {
            throw new RuntimeException("Activity entry not found");
        }
        activityEntryRepository.deleteById(id);
    }
    
    public Integer getTotalCaloriesBurnedByUserAndDate(Long userId, LocalDate date) {
        Integer totalCaloriesBurned = activityEntryRepository.sumCaloriesBurnedByUserAndDate(userId, date);
        return totalCaloriesBurned != null ? totalCaloriesBurned : 0;
    }
    
    public Integer getTotalCaloriesBurnedByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Integer totalCaloriesBurned = activityEntryRepository.sumCaloriesBurnedByUserAndDateRange(userId, startDate, endDate);
        return totalCaloriesBurned != null ? totalCaloriesBurned : 0;
    }
    
    public Integer getTotalDurationByUserAndDate(Long userId, LocalDate date) {
        Integer totalDuration = activityEntryRepository.sumDurationByUserAndDate(userId, date);
        return totalDuration != null ? totalDuration : 0;
    }
    
    public Integer getTotalStepsByUserAndDate(Long userId, LocalDate date) {
        Integer totalSteps = activityEntryRepository.sumStepsByUserAndDate(userId, date);
        return totalSteps != null ? totalSteps : 0;
    }
    
    public Integer calculateCaloriesBurned(String activityType, Integer durationMinutes, Double weightKg, String intensityLevel) {
        // Basic calorie calculation based on activity type and duration
        // This is a simplified calculation - in a real app, you'd use more sophisticated formulas
        
        double caloriesPerMinute = 0;
        
        switch (activityType.toUpperCase()) {
            case "RUNNING":
                caloriesPerMinute = intensityLevel.equals("HIGH") ? 12.0 : 8.0;
                break;
            case "WALKING":
                caloriesPerMinute = intensityLevel.equals("HIGH") ? 6.0 : 4.0;
                break;
            case "CYCLING":
                caloriesPerMinute = intensityLevel.equals("HIGH") ? 10.0 : 6.0;
                break;
            case "SWIMMING":
                caloriesPerMinute = intensityLevel.equals("HIGH") ? 9.0 : 6.0;
                break;
            case "WEIGHT_TRAINING":
                caloriesPerMinute = intensityLevel.equals("HIGH") ? 8.0 : 5.0;
                break;
            case "YOGA":
                caloriesPerMinute = 3.0;
                break;
            default:
                caloriesPerMinute = 5.0; // Default moderate activity
        }
        
        // Adjust for weight (heavier people burn more calories)
        double weightFactor = weightKg != null ? weightKg / 70.0 : 1.0;
        
        return (int) (caloriesPerMinute * durationMinutes * weightFactor);
    }
} 