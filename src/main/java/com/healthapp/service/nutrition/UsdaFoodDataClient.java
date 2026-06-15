package com.healthapp.service.nutrition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.config.NutritionLookupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UsdaFoodDataClient {

    private static final Logger logger = LoggerFactory.getLogger(UsdaFoodDataClient.class);
    private static final String SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    private static final String DETAIL_URL = "https://api.nal.usda.gov/fdc/v1/food/{fdcId}";

    private final NutritionLookupProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public UsdaFoodDataClient(NutritionLookupProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public boolean isAvailable() {
        return properties.isEnabled() && properties.getUsda().hasApiKey();
    }

    public UsdaSearchResponse searchFoods(String query, int pageSize) {
        if (!isAvailable()) {
            return UsdaSearchResponse.empty();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("api_key", properties.getUsda().getApiKey())
                    .queryParam("query", query)
                    .queryParam("pageSize", pageSize)
                    .queryParam("dataType", "Foundation,SR Legacy")
                    .toUriString();
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode foods = root.get("foods");
            if (foods == null || !foods.isArray()) {
                return UsdaSearchResponse.empty();
            }
            List<UsdaSearchResult> results = new ArrayList<>();
            for (JsonNode food : foods) {
                String description = food.path("description").asText("");
                int fdcId = food.path("fdcId").asInt();
                if (!description.isBlank() && fdcId > 0) {
                    results.add(new UsdaSearchResult(fdcId, description));
                }
            }
            return new UsdaSearchResponse(results, false);
        } catch (Exception e) {
            logger.warn("USDA search failed for query '{}': {}", query, e.getMessage());
            return UsdaSearchResponse.transientError();
        }
    }

    public UsdaDetailResponse getFoodDetails(int fdcId, double confidence) {
        if (!isAvailable()) {
            return UsdaDetailResponse.unavailable();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(DETAIL_URL)
                    .queryParam("api_key", properties.getUsda().getApiKey())
                    .buildAndExpand(fdcId)
                    .toUriString();
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            NutritionProfile profile = extractMacros(root, fdcId, confidence);
            if (profile == null) {
                return UsdaDetailResponse.notFound();
            }
            return UsdaDetailResponse.success(profile);
        } catch (Exception e) {
            logger.warn("USDA detail failed for fdcId {}: {}", fdcId, e.getMessage());
            return UsdaDetailResponse.transientError();
        }
    }

    static NutritionProfile extractMacros(JsonNode root, int fdcId, double confidence) {
        JsonNode nutrients = root.get("foodNutrients");
        if (nutrients == null || !nutrients.isArray()) {
            return null;
        }
        Double calories = null;
        Double protein = 0.0;
        Double carbs = 0.0;
        Double fat = 0.0;
        Double fiber = 0.0;
        for (JsonNode n : nutrients) {
            String name = nutrientName(n);
            double amount = nutrientAmount(n);
            if (name.contains("energy")) {
                if (name.contains("kcal") || name.equals("energy")) {
                    calories = amount;
                } else if (name.contains("kj") && calories == null) {
                    calories = amount / 4.184;
                }
            } else if (name.equals("protein")) {
                protein = amount;
            } else if (name.contains("carbohydrate")) {
                carbs = amount;
            } else if (name.contains("total lipid") || name.equals("fat")) {
                fat = amount;
            } else if (name.contains("fiber")) {
                fiber = amount;
            }
        }
        if (calories == null) {
            return null;
        }
        return new NutritionProfile(calories, protein, carbs, fat, fiber, NutritionSource.USDA, confidence, fdcId);
    }

    private static String nutrientName(JsonNode n) {
        JsonNode nutrient = n.get("nutrient");
        if (nutrient != null && nutrient.has("name")) {
            return nutrient.get("name").asText("").toLowerCase(Locale.ROOT);
        }
        return n.path("nutrientName").asText("").toLowerCase(Locale.ROOT);
    }

    private static double nutrientAmount(JsonNode n) {
        if (n.has("amount")) {
            return n.get("amount").asDouble();
        }
        return n.path("value").asDouble(0);
    }

    public record UsdaSearchResult(int fdcId, String description) {}

    public record UsdaSearchResponse(List<UsdaSearchResult> results, boolean failedTransiently) {
        public static UsdaSearchResponse empty() {
            return new UsdaSearchResponse(List.of(), false);
        }

        public static UsdaSearchResponse transientError() {
            return new UsdaSearchResponse(List.of(), true);
        }
    }

    public record UsdaDetailResponse(NutritionProfile profile, boolean failedTransiently, boolean found) {
        public static UsdaDetailResponse success(NutritionProfile profile) {
            return new UsdaDetailResponse(profile, false, true);
        }

        public static UsdaDetailResponse notFound() {
            return new UsdaDetailResponse(null, false, false);
        }

        public static UsdaDetailResponse transientError() {
            return new UsdaDetailResponse(null, true, false);
        }

        public static UsdaDetailResponse unavailable() {
            return new UsdaDetailResponse(null, false, false);
        }
    }
}
