package com.healthapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.config.JwtAuthenticationFilter;
import com.healthapp.dto.UserCreateRequest;
import com.healthapp.dto.UserPatchRequest;
import com.healthapp.entity.User;
import com.healthapp.service.UserService;
import com.healthapp.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ValidationService validationService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createUser_returnsValidationErrorsPayload() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("abc");
        request.setEmail("bad");

        when(validationService.validateUserCreation(org.mockito.ArgumentMatchers.any(UserCreateRequest.class)))
                .thenReturn(Map.of("email", "Email must be a valid email address"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"));
    }

    @Test
    void createUser_returnsUserResponseWhenValid() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setFirstName("Jane");
        request.setUsername("jane_1");
        request.setEmail("jane@example.com");
        request.setPassword("StrongPass1!");
        request.setDob(LocalDate.now().minusYears(22));
        request.setGender(User.Gender.FEMALE);
        request.setActivityLevel(User.ActivityLevel.MODERATE);
        request.setRole(User.UserRole.USER);

        User saved = request.toEntity();
        saved.setId(77L);
        saved.setAccountStatus(User.AccountStatus.ACTIVE);

        when(validationService.validateUserCreation(org.mockito.ArgumentMatchers.any(UserCreateRequest.class))).thenReturn(Map.of());
        when(userService.createUser(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.username").value("jane_1"));
    }

    @Test
    void patchUser_returnsBadRequestWhenValidationFails() throws Exception {
        UserPatchRequest request = new UserPatchRequest();
        request.setEmail("taken@example.com");

        when(validationService.validateUserPatch(org.mockito.ArgumentMatchers.any(UserPatchRequest.class), org.mockito.ArgumentMatchers.eq(5L)))
                .thenReturn(Map.of("email", "Email already exists"));

        mockMvc.perform(patch("/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email already exists"));
    }
}
