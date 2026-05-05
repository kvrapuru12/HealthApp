package com.healthapp.service;

import com.healthapp.dto.UserCreateRequest;
import com.healthapp.dto.UserPatchRequest;
import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ValidationService validationService;

    @Test
    void validateUserCreation_returnsExpectedErrorsForInvalidPayload() {
        UserCreateRequest request = new UserCreateRequest();
        request.setFirstName("A");
        request.setEmail("bad-email");
        request.setUsername("ab");
        request.setPassword("weak");
        request.setDob(LocalDate.now().plusDays(1));
        request.setDailyCalorieIntakeTarget(700);
        request.setWeight(500.0);

        Map<String, String> errors = validationService.validateUserCreation(request);

        assertEquals("First name must be between 2 and 50 characters", errors.get("firstName"));
        assertEquals("Email must be a valid email address", errors.get("email"));
        assertEquals("Username must be between 4 and 20 characters", errors.get("username"));
        assertEquals("Password must be between 8 and 64 characters", errors.get("password"));
        assertEquals("Date of birth must be in the past", errors.get("dob"));
        assertEquals("Gender is required", errors.get("gender"));
        assertEquals("Activity level is required", errors.get("activityLevel"));
        assertEquals("Role is required", errors.get("role"));
        assertEquals("Daily calorie intake target must be between 800 and 6000 cal", errors.get("dailyCalorieIntakeTarget"));
        assertEquals("Weight must be between 30 and 300 kg", errors.get("weight"));
    }

    @Test
    void validateUserCreation_flagsDuplicateEmailAndUsername() {
        UserCreateRequest request = new UserCreateRequest();
        request.setFirstName("Jane");
        request.setEmail("jane@example.com");
        request.setUsername("jane_user");
        request.setPassword("StrongPass1!");
        request.setDob(LocalDate.now().minusYears(20));
        request.setGender(User.Gender.FEMALE);
        request.setActivityLevel(User.ActivityLevel.MODERATE);
        request.setRole(User.UserRole.USER);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);
        when(userRepository.existsByUsername("jane_user")).thenReturn(true);

        Map<String, String> errors = validationService.validateUserCreation(request);

        assertEquals("Email already exists", errors.get("email"));
        assertEquals("Username already exists", errors.get("username"));
    }

    @Test
    void validateUserPatch_rejectsDeletedStatusAndDuplicateEmail() {
        UserPatchRequest patchRequest = new UserPatchRequest();
        patchRequest.setEmail("existing@example.com");
        patchRequest.setAccountStatus(User.AccountStatus.DELETED);

        when(userRepository.existsByEmailAndIdNot("existing@example.com", 10L)).thenReturn(true);

        Map<String, String> errors = validationService.validateUserPatch(patchRequest, 10L);

        assertEquals("Email already exists", errors.get("email"));
        assertEquals("Cannot set account status to DELETED via PATCH", errors.get("accountStatus"));
    }

    @Test
    void validateUserPatch_allowsValidMinimalPatch() {
        UserPatchRequest patchRequest = new UserPatchRequest();
        patchRequest.setFirstName("Alice");
        patchRequest.setPhoneNumber("+14155552671");

        Map<String, String> errors = validationService.validateUserPatch(patchRequest, 1L);

        assertTrue(errors.isEmpty());
    }
}
