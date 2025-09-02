package com.healthapp.service;

import com.healthapp.dto.WeightCreateRequest;
import com.healthapp.dto.WeightCreateResponse;
import com.healthapp.dto.WeightUpdateRequest;
import com.healthapp.entity.WeightEntry;
import com.healthapp.entity.User;
import com.healthapp.repository.WeightEntryRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WeightEntryServiceTest {

    @Autowired
    private WeightEntryService weightEntryService;

    @Autowired
    private WeightEntryRepository weightEntryRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setDob(java.time.LocalDate.of(1990, 1, 1));
        testUser.setGender(com.healthapp.entity.User.Gender.MALE);
        testUser.setActivityLevel(com.healthapp.entity.User.ActivityLevel.MODERATE);
        testUser.setRole(com.healthapp.entity.User.UserRole.USER);
        testUser = userRepository.save(testUser);

        testTime = LocalDateTime.now().minusHours(1);
    }

    @Test
    void createWeightEntry_Success() {
        // Arrange
        WeightCreateRequest request = new WeightCreateRequest();
        request.setUserId(testUser.getId());
        request.setLoggedAt(testTime);
        request.setWeight(new BigDecimal("70.5"));
        request.setNote("Morning weight");

        // Act
        WeightCreateResponse response = weightEntryService.createWeightEntry(request, testUser.getId(), false);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getCreatedAt());

        // Verify the entry was saved
        WeightEntry savedEntry = weightEntryRepository.findById(response.getId()).orElse(null);
        assertNotNull(savedEntry);
        assertEquals(testUser.getId(), savedEntry.getUserId());
        assertEquals(new BigDecimal("70.5"), savedEntry.getWeight());
        assertEquals("Morning weight", savedEntry.getNote());
        assertEquals(WeightEntry.Status.ACTIVE, savedEntry.getStatus());

        // Verify user's weight was updated
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(70.5, updatedUser.getWeight());
    }

    @Test
    void createWeightEntry_NonAdminCannotCreateForOtherUser() {
        // Arrange
        User otherUser = new User();
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmail("other@example.com");
        otherUser.setUsername("otheruser");
        otherUser.setPassword("password");
        otherUser.setDob(java.time.LocalDate.of(1990, 1, 1));
        otherUser.setGender(com.healthapp.entity.User.Gender.FEMALE);
        otherUser.setActivityLevel(com.healthapp.entity.User.ActivityLevel.MODERATE);
        otherUser.setRole(com.healthapp.entity.User.UserRole.USER);
        otherUser = userRepository.save(otherUser);

        WeightCreateRequest request = new WeightCreateRequest();
        request.setUserId(otherUser.getId());
        request.setLoggedAt(testTime);
        request.setWeight(new BigDecimal("65.0"));
        request.setNote("Test weight");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            weightEntryService.createWeightEntry(request, testUser.getId(), false);
        });
    }

    @Test
    void updateWeightEntry_Success() {
        // Arrange - Create a weight entry first
        WeightEntry weightEntry = new WeightEntry(testUser, testTime, new BigDecimal("70.0"), "Initial weight");
        weightEntry = weightEntryRepository.save(weightEntry);

        WeightUpdateRequest request = new WeightUpdateRequest();
        request.setWeight(new BigDecimal("69.5"));
        request.setNote("Updated weight");

        // Act
        Map<String, Object> response = weightEntryService.updateWeightEntry(weightEntry.getId(), request, testUser.getId(), false);

        // Assert
        assertNotNull(response);
        assertEquals("updated", response.get("message"));
        assertNotNull(response.get("updatedAt"));

        // Verify the entry was updated
        WeightEntry updatedEntry = weightEntryRepository.findById(weightEntry.getId()).orElse(null);
        assertNotNull(updatedEntry);
        assertEquals(new BigDecimal("69.5"), updatedEntry.getWeight());
        assertEquals("Updated weight", updatedEntry.getNote());
    }

    @Test
    void deleteWeightEntry_Success() {
        // Arrange - Create a weight entry first
        WeightEntry weightEntry = new WeightEntry(testUser, testTime, new BigDecimal("70.0"), "Test weight");
        weightEntry = weightEntryRepository.save(weightEntry);

        // Act
        Map<String, String> response = weightEntryService.deleteWeightEntry(weightEntry.getId(), testUser.getId(), false);

        // Assert
        assertNotNull(response);
        assertEquals("deleted", response.get("message"));

        // Verify the entry was soft deleted
        WeightEntry deletedEntry = weightEntryRepository.findById(weightEntry.getId()).orElse(null);
        assertNotNull(deletedEntry);
        assertEquals(WeightEntry.Status.DELETED, deletedEntry.getStatus());
    }

    @Test
    void syncUserLatestWeight_UpdatesUserWeight() {
        // Arrange - Create multiple weight entries
        WeightEntry entry1 = new WeightEntry(testUser, testTime.minusDays(2), new BigDecimal("70.0"), "Old weight");
        WeightEntry entry2 = new WeightEntry(testUser, testTime.minusDays(1), new BigDecimal("69.5"), "Recent weight");
        WeightEntry entry3 = new WeightEntry(testUser, testTime, new BigDecimal("69.0"), "Latest weight");
        
        weightEntryRepository.save(entry1);
        weightEntryRepository.save(entry2);
        weightEntryRepository.save(entry3);

        // Act - Create a new entry to trigger sync
        WeightCreateRequest request = new WeightCreateRequest();
        request.setUserId(testUser.getId());
        request.setLoggedAt(testTime.plusHours(1));
        request.setWeight(new BigDecimal("68.5"));
        request.setNote("New latest weight");

        weightEntryService.createWeightEntry(request, testUser.getId(), false);

        // Assert - User's weight should be updated to the most recent entry
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(68.5, updatedUser.getWeight());
    }
}
