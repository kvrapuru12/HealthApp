package com.healthapp.service;

import com.healthapp.dto.*;
import com.healthapp.entity.MenstrualCycle;
import com.healthapp.entity.User;
import com.healthapp.repository.MenstrualCycleRepository;
import com.healthapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MenstrualCycleService {
    
    private static final Logger logger = LoggerFactory.getLogger(MenstrualCycleService.class);
    
    @Autowired
    private MenstrualCycleRepository menstrualCycleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new menstrual cycle entry
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public MenstrualCycleCreateResponse createCycle(MenstrualCycleCreateRequest request, Long authenticatedUserId) {
        // Validate user access
        if (!request.getUserId().equals(authenticatedUserId)) {
            throw new IllegalArgumentException("Users can only create cycles for themselves");
        }
        
        // Validate user exists
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate period start date
        if (request.getPeriodStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Period start date cannot be in the future");
        }
        
        // Create cycle
        MenstrualCycle cycle = new MenstrualCycle();
        cycle.setUserId(request.getUserId());
        cycle.setPeriodStartDate(request.getPeriodStartDate());
        cycle.setCycleLength(request.getCycleLength());
        cycle.setPeriodDuration(request.getPeriodDuration());
        cycle.setIsCycleRegular(request.getIsCycleRegular());
        cycle.setStatus(MenstrualCycle.Status.ACTIVE);
        
        MenstrualCycle savedCycle = menstrualCycleRepository.save(cycle);
        
        logger.info("Created menstrual cycle: {} for user: {}", savedCycle.getId(), authenticatedUserId);
        
        return new MenstrualCycleCreateResponse(savedCycle.getId(), savedCycle.getCreatedAt());
    }
    
    /**
     * Get cycle details by ID
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public MenstrualCycleResponse getCycle(Long id, Long authenticatedUserId) {
        Optional<MenstrualCycle> cycle = menstrualCycleRepository.findByIdAndUserIdAndStatus(
                id, authenticatedUserId, MenstrualCycle.Status.ACTIVE);
        
        if (cycle.isEmpty()) {
            throw new IllegalArgumentException("Cycle not found or access denied");
        }
        
        return new MenstrualCycleResponse(cycle.get());
    }
    
    /**
     * Get paginated list of cycles
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public MenstrualCyclePaginatedResponse getCycles(Long userId, LocalDate fromDate, LocalDate toDate,
                                                   Integer page, Integer limit, Long authenticatedUserId) {
        // Validate user access
        if (!userId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("Users can only view their own cycles");
        }
        
        // Set defaults
        page = (page == null || page < 1) ? 1 : page;
        limit = (limit == null || limit < 1) ? 20 : Math.min(limit, 100);
        
        // Create pageable
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "periodStartDate"));
        
        Page<MenstrualCycle> cyclesPage;
        
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new IllegalArgumentException("From date must be before or equal to to date");
            }
            cyclesPage = menstrualCycleRepository.findByUserIdAndStatusAndPeriodStartDateBetween(
                    userId, MenstrualCycle.Status.ACTIVE, fromDate, toDate, pageable);
        } else {
            cyclesPage = menstrualCycleRepository.findByUserIdAndStatusOrderByPeriodStartDateDesc(
                    userId, MenstrualCycle.Status.ACTIVE, pageable);
        }
        
        List<MenstrualCycleResponse> cycles = cyclesPage.getContent().stream()
                .map(MenstrualCycleResponse::new)
                .toList();
        
        return new MenstrualCyclePaginatedResponse(cycles, page, limit, cyclesPage.getTotalElements());
    }
    
    /**
     * Update cycle
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void updateCycle(Long id, MenstrualCycleUpdateRequest request, Long authenticatedUserId) {
        Optional<MenstrualCycle> cycleOpt = menstrualCycleRepository.findByIdAndUserIdAndStatus(
                id, authenticatedUserId, MenstrualCycle.Status.ACTIVE);
        
        if (cycleOpt.isEmpty()) {
            throw new IllegalArgumentException("Cycle not found or access denied");
        }
        
        MenstrualCycle cycle = cycleOpt.get();
        
        // Update fields if provided
        if (request.getPeriodStartDate() != null) {
            if (request.getPeriodStartDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Period start date cannot be in the future");
            }
            cycle.setPeriodStartDate(request.getPeriodStartDate());
        }
        
        if (request.getCycleLength() != null) {
            cycle.setCycleLength(request.getCycleLength());
        }
        
        if (request.getPeriodDuration() != null) {
            cycle.setPeriodDuration(request.getPeriodDuration());
        }
        
        if (request.getIsCycleRegular() != null) {
            cycle.setIsCycleRegular(request.getIsCycleRegular());
        }
        
        menstrualCycleRepository.save(cycle);
        
        logger.info("Updated menstrual cycle: {} for user: {}", id, authenticatedUserId);
    }
    
    /**
     * Soft delete cycle
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void deleteCycle(Long id, Long authenticatedUserId) {
        Optional<MenstrualCycle> cycleOpt = menstrualCycleRepository.findByIdAndUserIdAndStatus(
                id, authenticatedUserId, MenstrualCycle.Status.ACTIVE);
        
        if (cycleOpt.isEmpty()) {
            throw new IllegalArgumentException("Cycle not found or access denied");
        }
        
        MenstrualCycle cycle = cycleOpt.get();
        cycle.setStatus(MenstrualCycle.Status.DELETED);
        menstrualCycleRepository.save(cycle);
        
        logger.info("Deleted menstrual cycle: {} for user: {}", id, authenticatedUserId);
    }
    
    /**
     * Get current cycle phase
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CyclePhaseResponse getCurrentPhase(Long authenticatedUserId) {
        // Get most recent cycle
        Optional<MenstrualCycle> recentCycle = menstrualCycleRepository.findFirstByUserIdAndStatusOrderByPeriodStartDateDesc(
                authenticatedUserId, MenstrualCycle.Status.ACTIVE);
        
        if (recentCycle.isEmpty()) {
            throw new IllegalArgumentException("No menstrual cycle data found. Please log your first period.");
        }
        
        MenstrualCycle cycle = recentCycle.get();
        LocalDate today = LocalDate.now();
        LocalDate periodStart = cycle.getPeriodStartDate();
        int cycleLength = cycle.getCycleLength();
        int periodDuration = cycle.getPeriodDuration();
        
        // Calculate cycle day
        long daysSincePeriodStart = ChronoUnit.DAYS.between(periodStart, today);
        int cycleDay = (int) (daysSincePeriodStart % cycleLength) + 1;
        
        // Calculate next period
        LocalDate nextPeriod = periodStart.plusDays(cycleLength);
        
        // Determine phase
        String phase;
        LocalDate phaseStartDate;
        LocalDate phaseEndDate;
        int daysInPhase;
        
        if (cycleDay <= periodDuration) {
            // Menstrual phase
            phase = "menstrual";
            phaseStartDate = periodStart;
            phaseEndDate = periodStart.plusDays(periodDuration - 1);
            daysInPhase = cycleDay;
        } else if (cycleDay <= 14) {
            // Follicular phase
            phase = "follicular";
            phaseStartDate = periodStart.plusDays(periodDuration);
            phaseEndDate = periodStart.plusDays(13);
            daysInPhase = cycleDay - periodDuration;
        } else if (cycleDay <= 16) {
            // Ovulatory phase
            phase = "ovulatory";
            phaseStartDate = periodStart.plusDays(14);
            phaseEndDate = periodStart.plusDays(15);
            daysInPhase = cycleDay - 14;
        } else {
            // Luteal phase
            phase = "luteal";
            phaseStartDate = periodStart.plusDays(16);
            phaseEndDate = nextPeriod.minusDays(1);
            daysInPhase = cycleDay - 16;
        }
        
        return new CyclePhaseResponse(phase, phaseStartDate, phaseEndDate, daysInPhase, cycleDay, nextPeriod);
    }
    
    /**
     * Get AI food recommendations based on current phase
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CycleSyncFoodResponse getFoodRecommendations(Long authenticatedUserId) {
        CyclePhaseResponse phaseResponse = getCurrentPhase(authenticatedUserId);
        String phase = phaseResponse.getPhase();
        
        List<String> recommendedFoods;
        List<String> avoid;
        String reasoning;
        
        switch (phase) {
            case "menstrual":
                recommendedFoods = List.of("Iron-rich foods", "Dark chocolate", "Nuts and seeds", "Leafy greens", "Lean protein");
                avoid = List.of("Caffeine", "Alcohol", "Processed foods", "High sodium foods");
                reasoning = "During menstruation, focus on iron-rich foods to replenish lost nutrients and reduce inflammation.";
                break;
            case "follicular":
                recommendedFoods = List.of("Complex carbohydrates", "Fresh fruits", "Vegetables", "Lean protein", "Healthy fats");
                avoid = List.of("Excessive sugar", "Processed foods", "Alcohol");
                reasoning = "In this high-energy phase, your body benefits from complex carbs and nutrient-rich foods to support follicle development.";
                break;
            case "ovulatory":
                recommendedFoods = List.of("Omega-3 rich foods", "Antioxidant-rich foods", "Lean protein", "Whole grains", "Fresh vegetables");
                avoid = List.of("Processed foods", "Excessive caffeine", "Alcohol");
                reasoning = "During ovulation, focus on foods that support hormone balance and provide sustained energy.";
                break;
            case "luteal":
                recommendedFoods = List.of("Complex carbohydrates", "Magnesium-rich foods", "B vitamins", "Healthy fats", "Protein");
                avoid = List.of("Excessive salt", "Caffeine", "Alcohol", "Refined sugars");
                reasoning = "In the luteal phase, focus on foods that support mood stability and reduce PMS symptoms.";
                break;
            default:
                recommendedFoods = List.of("Balanced diet", "Fresh fruits", "Vegetables", "Lean protein");
                avoid = List.of("Processed foods", "Excessive sugar", "Alcohol");
                reasoning = "Maintain a balanced diet with plenty of nutrients.";
        }
        
        return new CycleSyncFoodResponse(phase, recommendedFoods, avoid, reasoning);
    }
    
    /**
     * Get AI activity recommendations based on current phase
     */
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CycleSyncActivityResponse getActivityRecommendations(Long authenticatedUserId) {
        CyclePhaseResponse phaseResponse = getCurrentPhase(authenticatedUserId);
        String phase = phaseResponse.getPhase();
        
        List<String> recommendedWorkouts;
        List<String> avoid;
        String note;
        
        switch (phase) {
            case "menstrual":
                recommendedWorkouts = List.of("Gentle yoga", "Walking", "Light stretching", "Swimming", "Low-impact cardio");
                avoid = List.of("High-intensity workouts", "Heavy strength training", "Long-distance running", "Intense cardio");
                note = "Hormones are at their lowest. Light movement helps reduce cramps and bloating while conserving energy.";
                break;
            case "follicular":
                recommendedWorkouts = List.of("Strength training", "Cardio", "High-intensity workouts", "Running", "Cycling");
                avoid = List.of("Over-exercising", "Ignoring rest days");
                note = "Energy levels are high. This is the best time for intense workouts and building strength.";
                break;
            case "ovulatory":
                recommendedWorkouts = List.of("Moderate cardio", "Strength training", "Yoga", "Pilates", "Dancing");
                avoid = List.of("Over-exercising", "Ignoring hydration");
                note = "Peak energy and coordination. Focus on balanced workouts and stay hydrated.";
                break;
            case "luteal":
                recommendedWorkouts = List.of("Moderate cardio", "Yoga", "Pilates", "Walking", "Light strength training");
                avoid = List.of("High-intensity workouts", "Heavy lifting", "Long endurance sessions");
                note = "Energy may be lower. Focus on moderate exercise and stress-reducing activities.";
                break;
            default:
                recommendedWorkouts = List.of("Walking", "Yoga", "Swimming", "Light cardio");
                avoid = List.of("Over-exercising", "Ignoring body signals");
                note = "Listen to your body and choose activities that feel good.";
        }
        
        return new CycleSyncActivityResponse(phase, recommendedWorkouts, avoid, note);
    }
}
