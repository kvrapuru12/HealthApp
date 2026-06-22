package com.healthapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.dto.CyclePhaseResponse;
import com.healthapp.dto.CycleSyncUnifiedResponse;
import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CycleSyncRecommendationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MenstrualCycleService menstrualCycleService;

    @InjectMocks
    private CycleSyncRecommendationService cycleSyncRecommendationService;

    @Test
    void getUnifiedRecommendations_usesFallbackWhenAiUnavailable() {
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "openAiService", null);

        User user = new User();
        user.setId(1L);
        user.setDob(LocalDate.now().minusYears(26));
        user.setGender(User.Gender.FEMALE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(menstrualCycleService.getCurrentPhase(1L))
                .thenReturn(new CyclePhaseResponse("ovulatory", LocalDate.now().minusDays(1), LocalDate.now(), 1, 14, LocalDate.now().plusDays(14)));

        CycleSyncUnifiedResponse response = cycleSyncRecommendationService.getUnifiedRecommendations(1L);

        assertNotNull(response.getMenstrual());
        assertNotNull(response.getFollicular());
        assertNotNull(response.getOvulation());
        assertNotNull(response.getLuteal());
        assertEquals("Peak energy - Perform and recover", response.getOvulation().getSubtitle());
    }

    @Test
    void getUnifiedRecommendations_defaultsUnknownPhaseToFollicularFallback() {
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "openAiService", null);

        User user = new User();
        user.setId(2L);
        user.setDob(LocalDate.now().minusYears(20));

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(menstrualCycleService.getCurrentPhase(2L))
                .thenReturn(new CyclePhaseResponse("unknown_phase", LocalDate.now(), LocalDate.now(), 1, 1, LocalDate.now().plusDays(28)));

        CycleSyncUnifiedResponse response = cycleSyncRecommendationService.getUnifiedRecommendations(2L);

        assertEquals("Rising energy - Build momentum", response.getFollicular().getSubtitle());
    }
}
