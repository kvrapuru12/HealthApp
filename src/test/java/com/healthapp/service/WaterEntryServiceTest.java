package com.healthapp.service;

import com.healthapp.dto.WaterCreateRequest;
import com.healthapp.dto.WaterUpdateRequest;
import com.healthapp.entity.User;
import com.healthapp.entity.WaterEntry;
import com.healthapp.repository.UserRepository;
import com.healthapp.repository.WaterEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WaterEntryServiceTest {

    @Autowired
    private WaterEntryService waterEntryService;

    @Autowired
    private WaterEntryRepository waterEntryRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);
        
        testTime = LocalDateTime.now();
    }

    @Test
    void createWaterEntry_Success() {
        // Given
        WaterCreateRequest request = new WaterCreateRequest();
        request.setUserId(testUser.getId());
        request.setLoggedAt(testTime);
        request.setAmount(350);
        request.setNote("Test water entry");

        // When
        var response = waterEntryService.createWaterEntry(request, testUser.getId(), false);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(350, response.getAmount());
        assertEquals("Test water entry", response.getNote());
        assertEquals("active", response.getStatus());
    }

    @Test
    void createWaterEntry_AdminCanCreateForOtherUser() {
        // Given
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser = userRepository.save(otherUser);

        WaterCreateRequest request = new WaterCreateRequest();
        request.setUserId(otherUser.getId());
        request.setLoggedAt(testTime);
        request.setAmount(500);
        request.setNote("Admin created entry");

        // When
        var response = waterEntryService.createWaterEntry(request, testUser.getId(), true);

        // Then
        assertNotNull(response);
        assertEquals(otherUser.getId(), response.getUserId());
        assertEquals(500, response.getAmount());
    }

    @Test
    void updateWaterEntry_Success() {
        // Given
        WaterEntry waterEntry = new WaterEntry(testUser, testTime, 300, "Original note");
        waterEntry = waterEntryRepository.save(waterEntry);

        WaterUpdateRequest request = new WaterUpdateRequest();
        request.setAmount(400);
        request.setNote("Updated note");

        // When
        var response = waterEntryService.updateWaterEntry(waterEntry.getId(), request, testUser.getId(), false);

        // Then
        assertNotNull(response);
        assertEquals(400, response.getAmount());
        assertEquals("Updated note", response.getNote());
    }

    @Test
    void softDeleteWaterEntry_Success() {
        // Given
        WaterEntry waterEntry = new WaterEntry(testUser, testTime, 250, "To be deleted");
        waterEntry = waterEntryRepository.save(waterEntry);

        // When
        waterEntryService.softDeleteWaterEntry(waterEntry.getId(), testUser.getId(), false);

        // Then
        var deletedEntry = waterEntryRepository.findByIdAndStatus(waterEntry.getId(), WaterEntry.Status.ACTIVE);
        assertTrue(deletedEntry.isEmpty());
    }

    @Test
    void getWaterEntryById_Success() {
        // Given
        WaterEntry waterEntry = new WaterEntry(testUser, testTime, 400, "Test entry");
        waterEntry = waterEntryRepository.save(waterEntry);

        // When
        var response = waterEntryService.getWaterEntryById(waterEntry.getId());

        // Then
        assertTrue(response.isPresent());
        assertEquals(400, response.get().getAmount());
        assertEquals("Test entry", response.get().getNote());
    }
}
