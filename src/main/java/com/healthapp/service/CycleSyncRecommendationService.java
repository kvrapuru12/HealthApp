package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.dto.CyclePhaseResponse;
import com.healthapp.dto.CycleSyncUnifiedResponse;
import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CycleSyncRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(CycleSyncRecommendationService.class);

    private static final String SYSTEM_PROMPT = """
            You are a health and wellness recommendation assistant.
            Return ONLY valid JSON. No markdown, no prose, no explanation.
            Strictly follow this schema:
            {
              "menstrual": phaseObject,
              "follicular": phaseObject,
              "ovulation": phaseObject,
              "luteal": phaseObject
            }
            phaseObject must contain:
            - phaseName: string
            - days: string
            - subtitle: string
            - energyLevel: integer from 1 to 5
            - move: {
                title: string, intensity: string, sessionHint: string,
                main: string, mainDetail: string, extra: string, extraDetail: string,
                strengthFocus: boolean, note: string
              }
            - eatToday: {
                categories: { carbs: string[], protein: string[], fats: string[], greens: string[] },
                digestiveSupport: string[],
                prebioticFoods: string[],
                probioticFoods: string[],
                seedCycling: { main: string[], optionalAddons: string[] }
              }
            - feel: string[]
            - avoidDetailed: [{ item: string, reason: string }]
            - tip: string
            - digestionNote: string
            - theme: { accent: string(hex), background: string(hex) }
            Never return scalar strings for move/eatToday/feel/avoidDetailed/theme.
            Keep recommendations practical and concise. Avoid diagnosis and medical certainty.
            """
            + AiPromptGuidelines.RECOMMENDATION_QUALITATIVE_GUIDANCE;

    @Autowired(required = false)
    private OpenAiService openAiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenstrualCycleService menstrualCycleService;

    @Value("${cycle.sync.ai.timeout.seconds:8}")
    private int aiTimeoutSeconds;

    public CycleSyncUnifiedResponse getUnifiedRecommendations(Long authenticatedUserId) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId is required");
        CyclePhaseResponse currentPhase = menstrualCycleService.getCurrentPhase(authenticatedUserId);
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (openAiService == null) {
            logger.warn("OpenAI service unavailable; using fallback recommendations for user {}", authenticatedUserId);
            return buildFallbackRecommendations(normalizePhase(currentPhase.getPhase()));
        }

        try {
            String userPrompt = buildUserPrompt(user, currentPhase);
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(List.of(
                            new ChatMessage("system", SYSTEM_PROMPT),
                            new ChatMessage("user", userPrompt)
                    ))
                    .maxTokens(1600)
                    .temperature(0.15)
                    .build();

            String responseText = CompletableFuture.supplyAsync(() -> openAiService.createChatCompletion(request)
                            .getChoices()
                            .get(0)
                            .getMessage()
                            .getContent())
                    .orTimeout(aiTimeoutSeconds, TimeUnit.SECONDS)
                    .join();

            CycleSyncUnifiedResponse parsed = parseAiResponse(responseText, normalizePhase(currentPhase.getPhase()));
            return ensureComplete(parsed, normalizePhase(currentPhase.getPhase()));
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                logger.warn("OpenAI request timed out after {}s; using fallback recommendations for user {}",
                        aiTimeoutSeconds, authenticatedUserId);
                return buildFallbackRecommendations(normalizePhase(currentPhase.getPhase()));
            }
            logger.error("Failed to generate unified cycle-sync recommendations: {}", e.getMessage(), e);
            return buildFallbackRecommendations(normalizePhase(currentPhase.getPhase()));
        } catch (Exception e) {
            logger.error("Failed to generate unified cycle-sync recommendations: {}", e.getMessage(), e);
            return buildFallbackRecommendations(normalizePhase(currentPhase.getPhase()));
        }
    }

    private String buildUserPrompt(User user, CyclePhaseResponse currentPhase) {
        Integer age = user.getDob() == null ? null : Period.between(user.getDob(), LocalDate.now()).getYears();
        return """
                Build personalized cycle recommendations in the required JSON shape.

                User context:
                - age: %s
                - gender: %s
                - activityLevel: %s
                - weightKg: %s
                - heightCm: %s
                - dailyCalorieIntakeTarget: %s
                - dailyCalorieBurnTarget: %s
                - macroTargets: fat=%s, protein=%s, carbs=%s
                - targetSteps: %s
                - targetSleepHours: %s
                - targetWaterLitres: %s

                Cycle context:
                - currentPhase: %s
                - cycleDay: %s
                - daysInPhase: %s

                Output rules:
                1) Return only JSON object with keys menstrual, follicular, ovulation, luteal.
                2) energyLevel must be integer from 1 to 5.
                3) Keep arrays non-empty.
                4) Keep theme as hex colors.
                """.formatted(
                age,
                user.getGender(),
                user.getActivityLevel(),
                user.getWeight(),
                user.getHeight(),
                user.getDailyCalorieIntakeTarget(),
                user.getDailyCalorieBurnTarget(),
                user.getTargetFat(),
                user.getTargetProtein(),
                user.getTargetCarbs(),
                user.getTargetSteps(),
                user.getTargetSleepHours(),
                user.getTargetWaterLitres(),
                normalizePhase(currentPhase.getPhase()),
                currentPhase.getCycleDay(),
                currentPhase.getDaysInPhase()
        );
    }

    private CycleSyncUnifiedResponse parseAiResponse(String responseText, String currentPhase) throws Exception {
        String normalized = responseText == null ? "" : responseText.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        JsonNode root = objectMapper.readTree(normalized);
        return coerceResponse(root, currentPhase);
    }

    private String normalizePhase(String phase) {
        if (phase == null) {
            return "follicular";
        }
        return switch (phase.toLowerCase()) {
            case "ovulatory" -> "ovulation";
            case "menstrual", "follicular", "ovulation", "luteal" -> phase.toLowerCase();
            default -> "follicular";
        };
    }

    private CycleSyncUnifiedResponse ensureComplete(CycleSyncUnifiedResponse response, String currentPhase) {
        if (response == null) {
            return buildFallbackRecommendations(currentPhase);
        }

        CycleSyncUnifiedResponse fallback = buildFallbackRecommendations(currentPhase);
        if (response.getMenstrual() == null) {
            response.setMenstrual(fallback.getMenstrual());
        }
        if (response.getFollicular() == null) {
            response.setFollicular(fallback.getFollicular());
        }
        if (response.getOvulation() == null) {
            response.setOvulation(fallback.getOvulation());
        }
        if (response.getLuteal() == null) {
            response.setLuteal(fallback.getLuteal());
        }
        return response;
    }

    private CycleSyncUnifiedResponse coerceResponse(JsonNode root, String currentPhase) {
        CycleSyncUnifiedResponse base = buildFallbackRecommendations(currentPhase);
        if (root == null || !root.isObject()) {
            return base;
        }

        base.setMenstrual(coercePhase(root.get("menstrual"), base.getMenstrual()));
        base.setFollicular(coercePhase(root.get("follicular"), base.getFollicular()));
        base.setOvulation(coercePhase(root.get("ovulation"), base.getOvulation()));
        base.setLuteal(coercePhase(root.get("luteal"), base.getLuteal()));
        return base;
    }

    private CycleSyncUnifiedResponse.CyclePhaseRecommendation coercePhase(
            JsonNode node,
            CycleSyncUnifiedResponse.CyclePhaseRecommendation fallback) {
        if (node == null || !node.isObject()) {
            return fallback;
        }

        if (isText(node.get("phaseName"))) fallback.setPhaseName(node.get("phaseName").asText());
        if (isText(node.get("days"))) fallback.setDays(node.get("days").asText());
        if (isText(node.get("subtitle"))) fallback.setSubtitle(node.get("subtitle").asText());
        fallback.setEnergyLevel(coerceEnergyLevel(node.get("energyLevel"), fallback.getEnergyLevel()));
        if (isText(node.get("tip"))) fallback.setTip(node.get("tip").asText());
        if (isText(node.get("digestionNote"))) fallback.setDigestionNote(node.get("digestionNote").asText());

        fallback.setMove(coerceMove(node.get("move"), fallback.getMove()));
        fallback.setEatToday(coerceEatToday(node.get("eatToday"), fallback.getEatToday()));
        fallback.setFeel(coerceStringList(node.get("feel"), fallback.getFeel()));
        fallback.setAvoidDetailed(coerceAvoidDetailed(node.get("avoidDetailed"), fallback.getAvoidDetailed()));
        fallback.setTheme(coerceTheme(node.get("theme"), fallback.getTheme()));
        return fallback;
    }

    private CycleSyncUnifiedResponse.Move coerceMove(JsonNode node, CycleSyncUnifiedResponse.Move fallback) {
        if (node == null) return fallback;
        if (node.isTextual()) {
            fallback.setTitle(node.asText());
            return fallback;
        }
        if (!node.isObject()) return fallback;
        if (isText(node.get("title"))) fallback.setTitle(node.get("title").asText());
        if (isText(node.get("intensity"))) fallback.setIntensity(node.get("intensity").asText());
        if (isText(node.get("sessionHint"))) fallback.setSessionHint(node.get("sessionHint").asText());
        if (isText(node.get("main"))) fallback.setMain(node.get("main").asText());
        if (isText(node.get("mainDetail"))) fallback.setMainDetail(node.get("mainDetail").asText());
        if (isText(node.get("extra"))) fallback.setExtra(node.get("extra").asText());
        if (isText(node.get("extraDetail"))) fallback.setExtraDetail(node.get("extraDetail").asText());
        if (node.has("strengthFocus") && node.get("strengthFocus").isBoolean()) {
            fallback.setStrengthFocus(node.get("strengthFocus").asBoolean());
        }
        if (isText(node.get("note"))) fallback.setNote(node.get("note").asText());
        return fallback;
    }

    private CycleSyncUnifiedResponse.EatToday coerceEatToday(JsonNode node, CycleSyncUnifiedResponse.EatToday fallback) {
        if (node == null) return fallback;
        if (node.isTextual()) {
            fallback.setDigestiveSupport(List.of(node.asText()));
            return fallback;
        }
        if (!node.isObject()) return fallback;

        JsonNode categories = node.get("categories");
        if (categories != null && categories.isObject()) {
            CycleSyncUnifiedResponse.Categories cat = fallback.getCategories();
            if (cat == null) cat = new CycleSyncUnifiedResponse.Categories();
            cat.setCarbs(coerceStringList(categories.get("carbs"), cat.getCarbs()));
            cat.setProtein(coerceStringList(categories.get("protein"), cat.getProtein()));
            cat.setFats(coerceStringList(categories.get("fats"), cat.getFats()));
            cat.setGreens(coerceStringList(categories.get("greens"), cat.getGreens()));
            fallback.setCategories(cat);
        }

        fallback.setDigestiveSupport(coerceStringList(node.get("digestiveSupport"), fallback.getDigestiveSupport()));
        fallback.setPrebioticFoods(coerceStringList(node.get("prebioticFoods"), fallback.getPrebioticFoods()));
        fallback.setProbioticFoods(coerceStringList(node.get("probioticFoods"), fallback.getProbioticFoods()));

        JsonNode seed = node.get("seedCycling");
        if (seed != null && seed.isObject()) {
            CycleSyncUnifiedResponse.SeedCycling seedCycling = fallback.getSeedCycling();
            if (seedCycling == null) seedCycling = new CycleSyncUnifiedResponse.SeedCycling();
            seedCycling.setMain(coerceStringList(seed.get("main"), seedCycling.getMain()));
            seedCycling.setOptionalAddons(coerceStringList(seed.get("optionalAddons"), seedCycling.getOptionalAddons()));
            fallback.setSeedCycling(seedCycling);
        }
        return fallback;
    }

    private List<CycleSyncUnifiedResponse.AvoidDetailed> coerceAvoidDetailed(
            JsonNode node,
            List<CycleSyncUnifiedResponse.AvoidDetailed> fallback) {
        if (node == null) return fallback;
        List<CycleSyncUnifiedResponse.AvoidDetailed> out = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode entry : node) {
                if (entry != null && entry.isObject()) {
                    String item = isText(entry.get("item")) ? entry.get("item").asText() : null;
                    String reason = isText(entry.get("reason")) ? entry.get("reason").asText() : null;
                    if (item != null && reason != null) {
                        out.add(avoidItem(item, reason));
                    }
                } else if (entry != null && entry.isTextual()) {
                    out.add(avoidItem(entry.asText(), "May reduce comfort or recovery"));
                }
            }
        } else if (node.isTextual()) {
            out.add(avoidItem(node.asText(), "May reduce comfort or recovery"));
        }
        return out.isEmpty() ? fallback : out;
    }

    private CycleSyncUnifiedResponse.Theme coerceTheme(JsonNode node, CycleSyncUnifiedResponse.Theme fallback) {
        if (node == null) return fallback;
        if (node.isTextual()) return fallback;
        if (!node.isObject()) return fallback;
        if (isText(node.get("accent")) && node.get("accent").asText().startsWith("#")) {
            fallback.setAccent(node.get("accent").asText());
        }
        if (isText(node.get("background")) && node.get("background").asText().startsWith("#")) {
            fallback.setBackground(node.get("background").asText());
        }
        return fallback;
    }

    private List<String> coerceStringList(JsonNode node, List<String> fallback) {
        if (node == null) return fallback;
        List<String> out = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) {
                if (n != null && n.isTextual() && !n.asText().isBlank()) {
                    out.add(n.asText());
                }
            }
        } else if (node.isTextual() && !node.asText().isBlank()) {
            out.add(node.asText());
        }
        return out.isEmpty() ? fallback : out;
    }

    private Integer coerceEnergyLevel(JsonNode node, Integer fallback) {
        if (node == null) return fallback;
        if (node.isInt()) {
            int val = node.asInt();
            return Math.max(1, Math.min(5, val));
        }
        if (node.isTextual()) {
            String s = node.asText().toLowerCase();
            if (s.contains("very high") || s.contains("peak")) return 5;
            if (s.contains("high")) return 4;
            if (s.contains("moderate")) return 3;
            if (s.contains("very low")) return 1;
            if (s.contains("low")) return 2;
        }
        return fallback;
    }

    private boolean isText(JsonNode node) {
        return node != null && node.isTextual() && !node.asText().isBlank();
    }

    private CycleSyncUnifiedResponse buildFallbackRecommendations(String currentPhase) {
        CycleSyncUnifiedResponse response = new CycleSyncUnifiedResponse();
        response.setMenstrual(buildMenstrualFallback());
        response.setFollicular(buildFollicularFallback());
        response.setOvulation(buildOvulationFallback());
        response.setLuteal(buildLutealFallback());

        if ("menstrual".equals(currentPhase)) {
            response.getMenstrual().setSubtitle("Low energy - Focus on recovery");
        } else if ("follicular".equals(currentPhase)) {
            response.getFollicular().setSubtitle("Rising energy - Build momentum");
        } else if ("ovulation".equals(currentPhase)) {
            response.getOvulation().setSubtitle("Peak energy - Perform and recover");
        } else if ("luteal".equals(currentPhase)) {
            response.getLuteal().setSubtitle("Steady effort - Prioritize balance");
        }
        return response;
    }

    private CycleSyncUnifiedResponse.CyclePhaseRecommendation buildMenstrualFallback() {
        CycleSyncUnifiedResponse.CyclePhaseRecommendation p = basePhase("Menstrual Phase", "Day 1-5", 2, "#E97A7A", "#FFF4F4");
        p.setSubtitle("Low energy - Focus on recovery");
        p.setFeel(List.of("Low energy", "Need rest", "More inward"));
        p.setTip("Choose warm, iron-rich foods and lighter movement today.");
        p.setDigestionNote("Warm, gentle foods may feel easier on the stomach during this phase.");
        p.setMove(move("Light movement or rest", "low", "~15-25 min total. One focus or a light mix.",
                "Walking", "10-15 min easy", "Yoga / mobility", "10-15 min gentle", false,
                "Easy breathing; rest days count."));
        p.setEatToday(eat(
                List.of("Oats", "Sweet potato", "Rice"),
                List.of("Eggs", "Lentils", "Chicken"),
                List.of("Avocado", "Walnuts", "Pumpkin seeds"),
                List.of("Spinach", "Beetroot", "Berries"),
                List.of("Ginger tea", "Peppermint tea", "Warm meals"),
                List.of("Banana", "Oats", "Garlic"),
                List.of("Yogurt", "Kefir"),
                List.of("Flax", "Pumpkin"),
                List.of("Fennel seeds", "Fenugreek seeds")
        ));
        p.setAvoidDetailed(avoid(
                "Excess caffeine", "May worsen cramps or jitters",
                "Salty foods", "May increase water retention",
                "Alcohol", "May worsen fatigue and dehydration"
        ));
        return p;
    }

    private CycleSyncUnifiedResponse.CyclePhaseRecommendation buildFollicularFallback() {
        CycleSyncUnifiedResponse.CyclePhaseRecommendation p = basePhase("Follicular Phase", "Day 6-13", 4, "#7AC7E9", "#F2FAFF");
        p.setSubtitle("Rising energy - Build momentum");
        p.setFeel(List.of("Motivated", "Curious", "Lighter mood"));
        p.setTip("This is a great window for progressive training and fresh meal variety.");
        p.setDigestionNote("Fiber-rich meals often feel good in this phase.");
        p.setMove(move("Build strength and cardio base", "moderate", "~30-45 min. One main session.",
                "Strength training", "25-35 min", "Light cardio", "10-15 min", true,
                "Increase gradually and keep form sharp."));
        p.setEatToday(eat(
                List.of("Brown rice", "Quinoa", "Oats"),
                List.of("Fish", "Paneer", "Tofu"),
                List.of("Olive oil", "Almonds", "Chia"),
                List.of("Broccoli", "Kale", "Berries"),
                List.of("Mint tea", "Warm water", "Cooked vegetables"),
                List.of("Oats", "Apple", "Onion"),
                List.of("Yogurt", "Kimchi"),
                List.of("Flax", "Pumpkin"),
                List.of("Sesame", "Sunflower")
        ));
        p.setAvoidDetailed(avoid(
                "Skipping meals", "Can reduce workout quality and recovery",
                "Highly processed snacks", "May spike and crash energy",
                "Very late caffeine", "May affect sleep quality"
        ));
        return p;
    }

    private CycleSyncUnifiedResponse.CyclePhaseRecommendation buildOvulationFallback() {
        CycleSyncUnifiedResponse.CyclePhaseRecommendation p = basePhase("Ovulation Phase", "Day 14-16", 5, "#7AE9A8", "#F3FFF7");
        p.setSubtitle("Peak energy - Perform and recover");
        p.setFeel(List.of("Confident", "Social", "High output"));
        p.setTip("Use peak energy for performance days, then recover with hydration and protein.");
        p.setDigestionNote("Balanced meals with lean protein can support steady energy.");
        p.setMove(move("Performance-focused training", "moderate-high", "~35-50 min with warm-up and cool-down.",
                "Interval cardio", "15-20 min", "Strength compound lifts", "20-25 min", true,
                "Keep hydration high and avoid overreaching."));
        p.setEatToday(eat(
                List.of("Whole grain pasta", "Millets", "Rice"),
                List.of("Eggs", "Greek yogurt", "Chicken"),
                List.of("Nuts", "Seeds", "Avocado"),
                List.of("Leafy greens", "Bell pepper", "Berries"),
                List.of("Electrolyte water", "Ginger tea", "Warm soups"),
                List.of("Banana", "Oats", "Garlic"),
                List.of("Yogurt", "Kefir"),
                List.of("Sesame", "Sunflower"),
                List.of("Flax", "Pumpkin")
        ));
        p.setAvoidDetailed(avoid(
                "Under-hydration", "Can reduce performance and recovery",
                "High alcohol intake", "May impair sleep and muscle recovery",
                "Skipping warm-up", "Can increase injury risk"
        ));
        return p;
    }

    private CycleSyncUnifiedResponse.CyclePhaseRecommendation buildLutealFallback() {
        CycleSyncUnifiedResponse.CyclePhaseRecommendation p = basePhase("Luteal Phase", "Day 17-28", 3, "#D9A0E9", "#FCF5FF");
        p.setSubtitle("Steady effort - Prioritize balance");
        p.setFeel(List.of("Mixed energy", "More cravings", "Need structure"));
        p.setTip("Choose steady meals and moderate movement to reduce PMS discomfort.");
        p.setDigestionNote("Lower-salt, magnesium-rich foods can help with bloating.");
        p.setMove(move("Moderate training and stress relief", "moderate", "~25-40 min total.",
                "Walking / incline walk", "20-30 min", "Yoga / Pilates", "10-15 min", false,
                "Keep intensity flexible based on sleep and symptoms."));
        p.setEatToday(eat(
                List.of("Sweet potato", "Brown rice", "Oats"),
                List.of("Lentils", "Fish", "Tofu"),
                List.of("Pumpkin seeds", "Almonds", "Olive oil"),
                List.of("Spinach", "Cucumber", "Berries"),
                List.of("Peppermint tea", "Warm water", "Soups"),
                List.of("Oats", "Banana", "Garlic"),
                List.of("Yogurt", "Kefir"),
                List.of("Sesame", "Sunflower"),
                List.of("Fenugreek seeds", "Fennel seeds")
        ));
        p.setAvoidDetailed(avoid(
                "Excess salt", "May increase bloating",
                "Refined sugars", "May worsen cravings and energy crashes",
                "Very intense late workouts", "May disrupt sleep and recovery"
        ));
        return p;
    }

    private CycleSyncUnifiedResponse.CyclePhaseRecommendation basePhase(String phaseName, String days, int energyLevel, String accent, String background) {
        CycleSyncUnifiedResponse.CyclePhaseRecommendation phase = new CycleSyncUnifiedResponse.CyclePhaseRecommendation();
        phase.setPhaseName(phaseName);
        phase.setDays(days);
        phase.setEnergyLevel(energyLevel);
        CycleSyncUnifiedResponse.Theme theme = new CycleSyncUnifiedResponse.Theme();
        theme.setAccent(accent);
        theme.setBackground(background);
        phase.setTheme(theme);
        return phase;
    }

    private CycleSyncUnifiedResponse.Move move(String title, String intensity, String sessionHint, String main,
                                               String mainDetail, String extra, String extraDetail, boolean strengthFocus, String note) {
        CycleSyncUnifiedResponse.Move move = new CycleSyncUnifiedResponse.Move();
        move.setTitle(title);
        move.setIntensity(intensity);
        move.setSessionHint(sessionHint);
        move.setMain(main);
        move.setMainDetail(mainDetail);
        move.setExtra(extra);
        move.setExtraDetail(extraDetail);
        move.setStrengthFocus(strengthFocus);
        move.setNote(note);
        return move;
    }

    private CycleSyncUnifiedResponse.EatToday eat(List<String> carbs, List<String> protein, List<String> fats,
                                                  List<String> greens, List<String> digestiveSupport,
                                                  List<String> prebioticFoods, List<String> probioticFoods,
                                                  List<String> seedMain, List<String> seedOptional) {
        CycleSyncUnifiedResponse.EatToday eatToday = new CycleSyncUnifiedResponse.EatToday();
        CycleSyncUnifiedResponse.Categories categories = new CycleSyncUnifiedResponse.Categories();
        categories.setCarbs(carbs);
        categories.setProtein(protein);
        categories.setFats(fats);
        categories.setGreens(greens);
        eatToday.setCategories(categories);
        eatToday.setDigestiveSupport(digestiveSupport);
        eatToday.setPrebioticFoods(prebioticFoods);
        eatToday.setProbioticFoods(probioticFoods);
        CycleSyncUnifiedResponse.SeedCycling seedCycling = new CycleSyncUnifiedResponse.SeedCycling();
        seedCycling.setMain(seedMain);
        seedCycling.setOptionalAddons(seedOptional);
        eatToday.setSeedCycling(seedCycling);
        return eatToday;
    }

    private List<CycleSyncUnifiedResponse.AvoidDetailed> avoid(String item1, String reason1,
                                                               String item2, String reason2,
                                                               String item3, String reason3) {
        return List.of(
                avoidItem(item1, reason1),
                avoidItem(item2, reason2),
                avoidItem(item3, reason3)
        );
    }

    private CycleSyncUnifiedResponse.AvoidDetailed avoidItem(String item, String reason) {
        CycleSyncUnifiedResponse.AvoidDetailed avoidDetailed = new CycleSyncUnifiedResponse.AvoidDetailed();
        avoidDetailed.setItem(item);
        avoidDetailed.setReason(reason);
        return avoidDetailed;
    }
}
