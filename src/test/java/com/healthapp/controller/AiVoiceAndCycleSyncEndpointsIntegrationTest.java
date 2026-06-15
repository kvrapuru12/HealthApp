package com.healthapp.controller;

import com.healthapp.entity.MenstrualCycle;
import com.healthapp.entity.User;
import com.healthapp.repository.MenstrualCycleRepository;
import com.healthapp.repository.UserRepository;
import com.healthapp.service.OpenAiChatClient;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises HTTP endpoints backed by recent AI voice parsing and cycle-sync recommendation code paths,
 * with {@link OpenAiService} and {@link OpenAiChatClient} mocked so tests do not call the external OpenAI API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiVoiceAndCycleSyncEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenstrualCycleRepository menstrualCycleRepository;

    @MockBean
    private OpenAiService openAiService;

    @MockBean
    private OpenAiChatClient openAiChatClient;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("ai-endpoint-user");
        user.setEmail("ai-endpoint-user@example.com");
        user.setPassword("password");
        user = userRepository.save(user);

        MenstrualCycle cycle = new MenstrualCycle(
                user.getId(),
                LocalDate.now().minusDays(3),
                28,
                5,
                true);
        cycle.setStatus(MenstrualCycle.Status.ACTIVE);
        menstrualCycleRepository.save(cycle);

        when(openAiChatClient.isAvailable()).thenReturn(true);
        when(openAiChatClient.createStructuredCompletion(any(), any(), any(), any(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    String system = invocation.getArgument(1);
                    String userMsg = invocation.getArgument(2);
                    if (system.contains("menstrual cycle events")) {
                        return """
                                {"periodStartDate":"%s","cycleLength":28,"periodDuration":5,"isCycleRegular":true}
                                """.formatted(LocalDate.now().minusDays(1));
                    }
                    if (system.contains("physical activities")) {
                        String lower = userMsg.toLowerCase();
                        if (lower.contains("running") && lower.contains("push")) {
                            return """
                                    {"activities":[
                                      {"activityName":"running","durationMinutes":5,"loggedAt":"2026-05-04T11:00:00","note":"Voice: running segment"},
                                      {"activityName":"push ups","durationMinutes":5,"loggedAt":"2026-05-04T11:05:00","note":"Voice: push ups segment"},
                                      {"activityName":"cross trainer","durationMinutes":5,"loggedAt":"2026-05-04T11:10:00","note":"Voice: cross trainer segment"},
                                      {"activityName":"walk","durationMinutes":5,"loggedAt":"2026-05-04T11:15:00","note":"Voice: walk segment"},
                                      {"activityName":"swim","durationMinutes":5,"loggedAt":"2026-05-04T11:20:00","note":"Voice: swim segment"}
                                    ]}
                                    """;
                        }
                        if (lower.contains("swim") || lower.contains("swam")) {
                            return """
                                    {"activities":[{"activityName":"swimming","durationMinutes":20,"loggedAt":"2026-05-04T07:30:00","note":"Voice: pool session Assumed: duration 20 min."}]}
                                    """;
                        }
                        return """
                                {"activities":[{"activityName":"Walking","durationMinutes":30,"loggedAt":"2026-05-04T10:00:00","note":"Voice: morning walk Stated: 30 minutes."}]}
                                """;
                    }
                    if (userMsg.contains("__EMPTY_FOOD__")) {
                        return "{\"compositeMeals\":[],\"foodItems\":[]}";
                    }
                    if (userMsg.toLowerCase().contains("avocado")) {
                        return """
                                {"compositeMeals":[],"foodItems":[{"foodName":"avocado","quantity":1,"unit":"medium","estimatedGrams":150,"mealType":"snack","loggedAt":"2026-05-04T12:00:00","note":"Stated: 1 medium avocado.","nutrition":{"caloriesPer100g":160,"proteinPer100g":2,"carbsPer100g":8.5,"fatPer100g":14.7,"fiberPer100g":6.7}}]}
                                """;
                    }
                    return """
                            {"compositeMeals":[],"foodItems":[{"foodName":"Integration Test Apple","quantity":1,"unit":"piece","estimatedGrams":150,"mealType":"snack","loggedAt":"2026-05-04T12:00:00","note":"Assumed: 1 apple ~150 g.","nutrition":{"caloriesPer100g":52,"proteinPer100g":0.3,"carbsPer100g":14,"fatPer100g":0.2,"fiberPer100g":2.4}}]}
                            """;
                });

        when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
                .thenAnswer(invocation -> chatCompletionResult("{}"));
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

    private static org.springframework.security.core.Authentication auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void getUnifiedCycleSyncRecommendations_ok() throws Exception {
        mockMvc.perform(get("/ai/suggestions/cycle-sync")
                        .with(authentication(auth(user.getId())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menstrual").exists())
                .andExpect(jsonPath("$.follicular").exists())
                .andExpect(jsonPath("$.ovulation").exists())
                .andExpect(jsonPath("$.luteal").exists());
    }

    @Test
    void getUnifiedCycleSyncRecommendations_withMatchingUserIdParam_ok() throws Exception {
        mockMvc.perform(get("/ai/suggestions/cycle-sync")
                        .param("userId", String.valueOf(user.getId()))
                        .with(authentication(auth(user.getId())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menstrual").exists());
    }

    @Test
    void getUnifiedCycleSyncRecommendations_whenUserIdParamDoesNotMatchAuth_returnsBadRequest() throws Exception {
        long otherId = user.getId() + 999;
        mockMvc.perform(get("/ai/suggestions/cycle-sync")
                        .param("userId", String.valueOf(otherId))
                        .with(authentication(auth(user.getId())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business logic error"));
    }

    @Test
    void getCycleSyncFoodRecommendations_ok() throws Exception {
        mockMvc.perform(get("/ai/suggestions/cycle-sync/food")
                        .with(authentication(auth(user.getId())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").exists());
    }

    @Test
    void getCycleSyncActivityRecommendations_ok() throws Exception {
        mockMvc.perform(get("/ai/suggestions/cycle-sync/activity")
                        .with(authentication(auth(user.getId())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").exists());
    }

    @Test
    void postFoodLogFromVoice_avocado_realisticCalories() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"I ate 1 medium size avocado"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/food-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logs[0].food").value("avocado"))
                .andExpect(jsonPath("$.logs[0].estimatedGrams").value(150.0))
                .andExpect(jsonPath("$.logs[0].calories").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.greaterThan(200.0),
                        org.hamcrest.Matchers.lessThan(320.0))));
    }

    @Test
    void postFoodLogFromVoice_created() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"ate an apple for a snack"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/food-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.logs[0].food").value("Integration Test Apple"))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    void postFoodLogFromVoice_whenAiReturnsNoItems_returnsUnprocessableWithErrorCode() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"__EMPTY_FOOD__"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/food-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("NO_FOOD_PARSED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.logs").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"userId\":%d}",
            "{\"userId\":%d,\"voiceText\":\"\"}",
            "{\"voiceText\":\"only text\"}"
    })
    void postFoodLogFromVoice_invalidBody_returnsValidationError(String bodyTemplate) throws Exception {
        String body = bodyTemplate.contains("%d")
                ? bodyTemplate.formatted(user.getId())
                : bodyTemplate;

        mockMvc.perform(post("/ai/food-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"userId\":%d}",
            "{\"userId\":%d,\"voiceText\":\"\"}",
            "{\"voiceText\":\"my period started\"}"
    })
    void postCycleLogFromVoice_invalidBody_returnsValidationError(String bodyTemplate) throws Exception {
        String body = bodyTemplate.contains("%d")
                ? bodyTemplate.formatted(user.getId())
                : bodyTemplate;

        mockMvc.perform(post("/ai/suggestions/cycle-sync/cycle-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"userId\":%d}",
            "{\"userId\":%d,\"voiceText\":\"run\"}",
            "{\"userId\":%d,\"voiceText\":\"\"}",
            "{\"voiceText\":\"I went for a thirty minute walk this morning\"}"
    })
    void postActivityLogFromVoice_invalidBody_returnsValidationError(String bodyTemplate) throws Exception {
        String body = bodyTemplate.contains("%d")
                ? bodyTemplate.formatted(user.getId())
                : bodyTemplate;

        mockMvc.perform(post("/ai/activity-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void postCycleLogFromVoice_created() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"my period started yesterday"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/suggestions/cycle-sync/cycle-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void postActivityLogFromVoice_created() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"I went for a thirty minute walk this morning"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/activity-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Activity logged successfully"))
                .andExpect(jsonPath("$.activityLog.activity").value("Walking"))
                .andExpect(jsonPath("$.activityLog.durationMinutes").value(30))
                .andExpect(jsonPath("$.activityLog.note").value("Voice: morning walk Stated: 30 minutes."))
                .andExpect(jsonPath("$.activityLogs").isArray())
                .andExpect(jsonPath("$.activityLogs.length()").value(1))
                .andExpect(jsonPath("$.activityLogs[0].activity").value("Walking"));
    }

    @Test
    void postActivityLogFromVoice_multiSegment_createsMultipleActivityLogs() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"I did 5 min running, 5 min push ups, 5 min cross trainer, 5 min walk, and 5 min swim"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/activity-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Logged 5 activities from voice input"))
                .andExpect(jsonPath("$.activityLogs.length()").value(5))
                .andExpect(jsonPath("$.activityLog.activity").value("running"))
                .andExpect(jsonPath("$.activityLog.durationMinutes").value(5))
                .andExpect(jsonPath("$.activityLogs[4].activity").value("swim"));
    }

    @Test
    void postActivityLogFromVoice_swimmingExample() throws Exception {
        String body = """
                {"userId":%d,"voiceText":"I swam laps at the pool for twenty minutes after work"}
                """.formatted(user.getId());

        mockMvc.perform(post("/ai/activity-log/from-voice")
                        .with(authentication(auth(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityLog.activity").value("swimming"))
                .andExpect(jsonPath("$.activityLog.durationMinutes").value(20));
    }
}
