package com.healthapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.config.OpenAiModelProperties;
import com.healthapp.dto.CyclePhaseResponse;
import com.healthapp.dto.CycleSyncUnifiedResponse;
import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CycleSyncRecommendationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MenstrualCycleService menstrualCycleService;
    @Mock
    private OpenAiService openAiService;
    @Mock
    private OpenAiModelProperties modelProperties;

    @InjectMocks
    private CycleSyncRecommendationService cycleSyncRecommendationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "aiTimeoutSeconds", 8);
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "overviewCacheTtlHours", 24);
        ReflectionTestUtils.setField(
                cycleSyncRecommendationService,
                "overviewCache",
                new ConcurrentHashMap<>());
    }

    @Test
    void getUnifiedRecommendations_usesFallbackWhenAiUnavailable() {
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

    @Test
    void getUnifiedRecommendations_cacheHit_skipsSecondOpenAiCall() {
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "openAiService", openAiService);

        User user = new User();
        user.setId(3L);
        user.setDob(LocalDate.now().minusYears(28));
        user.setGender(User.Gender.FEMALE);

        CyclePhaseResponse phase = new CyclePhaseResponse(
                "follicular", LocalDate.now().minusDays(3), LocalDate.now(), 3, 8, LocalDate.now().plusDays(20));

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(menstrualCycleService.getCurrentPhase(3L)).thenReturn(phase);
        when(modelProperties.getCycleSyncModel()).thenReturn("gpt-4o-mini");
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult("""
                        {"menstrual":{"tip":"cached-menstrual-tip"}}
                        """));

        CycleSyncUnifiedResponse first = cycleSyncRecommendationService.getUnifiedRecommendations(3L);
        CycleSyncUnifiedResponse second = cycleSyncRecommendationService.getUnifiedRecommendations(3L);

        assertNotNull(first);
        assertSame(first, second);
        verify(openAiService, times(1)).createChatCompletion(any(ChatCompletionRequest.class));
        verify(userRepository, times(1)).findById(3L);
    }

    @Test
    void getUnifiedRecommendations_cacheMiss_whenPhaseChanges() {
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "openAiService", openAiService);

        User user = new User();
        user.setId(4L);
        user.setDob(LocalDate.now().minusYears(30));
        user.setGender(User.Gender.FEMALE);

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(modelProperties.getCycleSyncModel()).thenReturn("gpt-4o-mini");
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult("{}"));

        when(menstrualCycleService.getCurrentPhase(4L))
                .thenReturn(new CyclePhaseResponse("follicular", LocalDate.now().minusDays(5), LocalDate.now(), 5, 10, LocalDate.now().plusDays(18)))
                .thenReturn(new CyclePhaseResponse("luteal", LocalDate.now().minusDays(10), LocalDate.now(), 10, 22, LocalDate.now().plusDays(6)));

        cycleSyncRecommendationService.getUnifiedRecommendations(4L);
        cycleSyncRecommendationService.getUnifiedRecommendations(4L);

        verify(openAiService, times(2)).createChatCompletion(any(ChatCompletionRequest.class));
    }

    @Test
    void getUnifiedRecommendations_cacheMiss_afterTtlExpires() {
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "overviewCacheTtlHours", 0);
        ReflectionTestUtils.setField(cycleSyncRecommendationService, "openAiService", openAiService);

        User user = new User();
        user.setId(5L);
        user.setDob(LocalDate.now().minusYears(25));
        user.setGender(User.Gender.FEMALE);

        CyclePhaseResponse phase = new CyclePhaseResponse(
                "menstrual", LocalDate.now().minusDays(1), LocalDate.now(), 1, 2, LocalDate.now().plusDays(26));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(menstrualCycleService.getCurrentPhase(5L)).thenReturn(phase);
        when(modelProperties.getCycleSyncModel()).thenReturn("gpt-4o-mini");
        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenReturn(chatCompletionResult("{}"));

        cycleSyncRecommendationService.getUnifiedRecommendations(5L);
        cycleSyncRecommendationService.getUnifiedRecommendations(5L);

        verify(openAiService, times(2)).createChatCompletion(any(ChatCompletionRequest.class));
    }

    private static ChatCompletionResult chatCompletionResult(String assistantContent) {
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent(assistantContent);

        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setMessage(message);

        ChatCompletionResult result = new ChatCompletionResult();
        result.setChoices(List.of(choice));
        return result;
    }
}
