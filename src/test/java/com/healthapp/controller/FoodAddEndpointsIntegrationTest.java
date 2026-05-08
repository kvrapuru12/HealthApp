package com.healthapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.entity.FoodItem;
import com.healthapp.entity.FoodLog;
import com.healthapp.entity.User;
import com.healthapp.repository.FoodItemRepository;
import com.healthapp.repository.FoodLogRepository;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FoodAddEndpointsIntegrationTest {

    private static final DateTimeFormatter UTC_Z =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private FoodLogRepository foodLogRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("food-add-user");
        user.setEmail("food-add-user@example.com");
        user.setPassword("password");
        user = userRepository.save(user);
    }

    @Test
    void foodsPost_createsFoodItem() throws Exception {
        String body = """
                {
                  "name": "Integration Test Oats",
                  "category": "breakfast",
                  "caloriesPerUnit": 150,
                  "proteinPerUnit": 5.0,
                  "carbsPerUnit": 27.0,
                  "fatPerUnit": 3.0,
                  "fiberPerUnit": 4.0,
                  "visibility": "private"
                }
                """;

        MvcResult result = mockMvc.perform(post("/foods")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long foodItemId = root.get("id").asLong();
        assertTrue(foodItemId > 0);

        FoodItem saved = foodItemRepository.findByIdAndStatus(foodItemId, FoodItem.FoodStatus.ACTIVE).orElseThrow();
        assertEquals("Integration Test Oats", saved.getName());
        assertEquals(user.getId(), saved.getCreatedBy());
    }

    @Test
    void foodLogsPost_createsFoodLogUsingFoodItem() throws Exception {
        FoodItem item = new FoodItem("Preseed Banana", 89, user.getId());
        item.setVisibility(FoodItem.FoodVisibility.PRIVATE);
        item.setStatus(FoodItem.FoodStatus.ACTIVE);
        item = foodItemRepository.save(item);

        OffsetDateTime loggedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2).withNano(0);
        String loggedAtStr = loggedAt.withOffsetSameInstant(ZoneOffset.UTC).format(UTC_Z);

        String body = """
                {
                  "userId": %d,
                  "foodItemId": %d,
                  "loggedAt": "%s",
                  "mealType": "SNACK",
                  "quantity": 1.0,
                  "unit": "grams",
                  "note": "integration test"
                }
                """.formatted(user.getId(), item.getId(), loggedAtStr);

        mockMvc.perform(post("/food-logs")
                        .with(authentication(auth(user.getId(), false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        List<FoodLog> logs = foodLogRepository.findByUserIdAndStatus(user.getId(), FoodLog.FoodLogStatus.ACTIVE);
        assertEquals(1, logs.size());
        assertEquals(item.getId(), logs.get(0).getFoodItemId());
        assertEquals(loggedAt.toLocalDateTime(), logs.get(0).getLoggedAt());
    }

    private static Authentication auth(Long userId, boolean admin) {
        List<SimpleGrantedAuthority> roles = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new UsernamePasswordAuthenticationToken(userId, null, roles);
    }
}
