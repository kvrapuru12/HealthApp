package com.healthapp.controller;

import com.healthapp.entity.Activity;
import com.healthapp.entity.ActivityLog;
import com.healthapp.entity.FoodItem;
import com.healthapp.entity.FoodLog;
import com.healthapp.entity.User;
import com.healthapp.repository.ActivityLogRepository;
import com.healthapp.repository.ActivityRepository;
import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.FoodLogRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserFoodActivityHardDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private FoodLogRepository foodLogRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    void adminHardDelete_removesFoodAndActivityDataForEmail() throws Exception {
        User target = userRepository.save(buildUser("hd-target", "hd-target@example.com", User.UserRole.USER));
        User admin = userRepository.save(buildUser("hd-admin", "hd-admin@example.com", User.UserRole.ADMIN));

        FoodItem item = new FoodItem("HardDel Item", 50, target.getId());
        item.setStatus(FoodItem.FoodStatus.ACTIVE);
        item.setVisibility(FoodItem.FoodVisibility.PRIVATE);
        item = foodItemRepository.save(item);

        FoodLog fl = new FoodLog(target.getId(), item.getId(), LocalDateTime.now().minusHours(1), 100.0, "grams");
        fl.setStatus(FoodLog.FoodLogStatus.ACTIVE);
        foodLogRepository.save(fl);

        Activity act = new Activity(
                "HardDel Activity",
                "cardio",
                new BigDecimal("10.0"),
                Activity.Visibility.PRIVATE,
                target);
        act.setStatus(Activity.Status.ACTIVE);
        act = activityRepository.save(act);

        ActivityLog alog = new ActivityLog(target, act, LocalDateTime.now().minusMinutes(30), 20, null);
        activityLogRepository.save(alog);

        mockMvc.perform(delete("/users/admin/hard-delete-food-activity-data")
                        .param("email", target.getEmail())
                        .with(authentication(auth(admin.getId(), true))))
                .andExpect(status().isOk());

        List<FoodLog> remainingFoodLogs =
                foodLogRepository.findByUserIdAndStatus(target.getId(), FoodLog.FoodLogStatus.ACTIVE);
        assertTrue(remainingFoodLogs.isEmpty());

        List<ActivityLog> remainingActivityLogs =
                activityLogRepository.findByUserAndStatus(target.getId(), ActivityLog.Status.ACTIVE);
        assertTrue(remainingActivityLogs.isEmpty());

        assertTrue(foodItemRepository.findById(item.getId()).isEmpty());
        assertTrue(activityRepository.findById(act.getId()).isEmpty());
    }

    @Test
    void nonAdmin_returnsForbidden() throws Exception {
        User user = userRepository.save(buildUser("hd-plain", "hd-plain@example.com", User.UserRole.USER));

        mockMvc.perform(delete("/users/admin/hard-delete-food-activity-data")
                        .param("email", "anyone@example.com")
                        .with(authentication(auth(user.getId(), false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownEmail_returnsNotFound() throws Exception {
        User admin = userRepository.save(buildUser("hd-admin2", "hd-admin2@example.com", User.UserRole.ADMIN));

        mockMvc.perform(delete("/users/admin/hard-delete-food-activity-data")
                        .param("email", "nosuchuser-ever@example.com")
                        .with(authentication(auth(admin.getId(), true))))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankEmail_returnsBadRequest() throws Exception {
        User admin = userRepository.save(buildUser("hd-admin3", "hd-admin3@example.com", User.UserRole.ADMIN));

        mockMvc.perform(delete("/users/admin/hard-delete-food-activity-data")
                        .param("email", "   ")
                        .with(authentication(auth(admin.getId(), true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminHardDelete_preservesPublicFoodItemStillReferencedByAnotherUser() throws Exception {
        User creator = userRepository.save(buildUser("hd-creator-fi", "hd-creator-fi@example.com", User.UserRole.USER));
        User consumer = userRepository.save(buildUser("hd-consumer-fi", "hd-consumer-fi@example.com", User.UserRole.USER));
        User admin = userRepository.save(buildUser("hd-admin-fi", "hd-admin-fi@example.com", User.UserRole.ADMIN));

        FoodItem sharedItem = new FoodItem("Shared Template Food", 100, creator.getId());
        sharedItem.setStatus(FoodItem.FoodStatus.ACTIVE);
        sharedItem.setVisibility(FoodItem.FoodVisibility.PUBLIC);
        sharedItem = foodItemRepository.save(sharedItem);

        FoodLog consumerLog = new FoodLog(consumer.getId(), sharedItem.getId(), LocalDateTime.now().minusHours(2), 1.0, "serving");
        consumerLog.setStatus(FoodLog.FoodLogStatus.ACTIVE);
        foodLogRepository.save(consumerLog);

        mockMvc.perform(delete("/users/admin/hard-delete-food-activity-data")
                        .param("email", creator.getEmail())
                        .with(authentication(auth(admin.getId(), true))))
                .andExpect(status().isOk());

        assertTrue(foodItemRepository.findById(sharedItem.getId()).isPresent());
        assertFalse(foodLogRepository.findByUserIdAndStatus(consumer.getId(), FoodLog.FoodLogStatus.ACTIVE).isEmpty());
    }

    @Test
    void adminHardDelete_preservesPublicActivityStillReferencedByAnotherUser() throws Exception {
        User creator = userRepository.save(buildUser("hd-creator-act", "hd-creator-act@example.com", User.UserRole.USER));
        User consumer = userRepository.save(buildUser("hd-consumer-act", "hd-consumer-act@example.com", User.UserRole.USER));
        User admin = userRepository.save(buildUser("hd-admin-act", "hd-admin-act@example.com", User.UserRole.ADMIN));

        Activity sharedActivity = new Activity(
                "Shared Template Activity",
                "cardio",
                new BigDecimal("8.0"),
                Activity.Visibility.PUBLIC,
                creator);
        sharedActivity.setStatus(Activity.Status.ACTIVE);
        sharedActivity = activityRepository.save(sharedActivity);

        ActivityLog consumerLog = new ActivityLog(consumer, sharedActivity, LocalDateTime.now().minusHours(1), 25, null);
        activityLogRepository.save(consumerLog);

        mockMvc.perform(delete("/users/admin/hard-delete-food-activity-data")
                        .param("email", creator.getEmail())
                        .with(authentication(auth(admin.getId(), true))))
                .andExpect(status().isOk());

        assertTrue(activityRepository.findById(sharedActivity.getId()).isPresent());
        assertFalse(activityLogRepository.findByUserAndStatus(consumer.getId(), ActivityLog.Status.ACTIVE).isEmpty());
    }

    private static User buildUser(String username, String email, User.UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("pw");
        u.setRole(role);
        return u;
    }

    private static Authentication auth(Long userId, boolean admin) {
        List<SimpleGrantedAuthority> roles = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new UsernamePasswordAuthenticationToken(userId, null, roles);
    }
}
