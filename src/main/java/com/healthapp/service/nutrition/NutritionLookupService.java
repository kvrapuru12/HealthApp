package com.healthapp.service.nutrition;

import com.healthapp.config.NutritionLookupProperties;
import com.healthapp.entity.FoodNutritionCache;
import com.healthapp.repository.FoodNutritionCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class NutritionLookupService {

    private static final Logger logger = LoggerFactory.getLogger(NutritionLookupService.class);
    static final String NEGATIVE_CACHE_SOURCE = "USDA_MISS";
    private static final int MAX_NORMALIZED_NAME_LENGTH = 120;

    private final NutritionLookupProperties properties;
    private final FoodNutritionCacheRepository cacheRepository;
    private final UsdaFoodDataClient usdaClient;
    private final FoodNutritionFallback foodNutritionFallback;

    @Autowired
    public NutritionLookupService(NutritionLookupProperties properties,
                                  FoodNutritionCacheRepository cacheRepository,
                                  UsdaFoodDataClient usdaClient,
                                  FoodNutritionFallback foodNutritionFallback) {
        this.properties = properties;
        this.cacheRepository = cacheRepository;
        this.usdaClient = usdaClient;
        this.foodNutritionFallback = foodNutritionFallback;
    }

    @PostConstruct
    void logLookupStatus() {
        if (!properties.isEnabled()) {
            logger.info("Nutrition lookup is disabled");
            return;
        }
        if (!"usda".equalsIgnoreCase(properties.getProvider())) {
            logger.warn("Nutrition provider '{}' is not supported; only 'usda' is implemented", properties.getProvider());
        }
        if (usdaClient.isAvailable()) {
            logger.info("USDA nutrition lookup enabled (confidence threshold={}, cache TTL={} days)",
                    properties.getConfidenceThreshold(), properties.getCacheTtlDays());
        } else {
            logger.warn("USDA nutrition lookup unavailable — set USDA_API_KEY in .env (local profile) or environment");
        }
    }

    private static final double INGREDIENT_BLEND_CONFIDENCE_THRESHOLD = 0.65;

    public Optional<NutritionProfile> lookup(String foodName) {
        return lookup(foodName, properties.getConfidenceThreshold());
    }

    public Optional<NutritionProfile> lookupIngredient(String foodName, String searchTerm) {
        return lookup(searchTerm != null && !searchTerm.isBlank() ? searchTerm : foodName,
                INGREDIENT_BLEND_CONFIDENCE_THRESHOLD);
    }

    private Optional<NutritionProfile> lookup(String foodName, double confidenceThreshold) {
        if (!properties.isEnabled() || foodName == null || foodName.isBlank()) {
            return Optional.empty();
        }
        if (!"usda".equalsIgnoreCase(properties.getProvider())) {
            return fallbackLookup(normalize(foodName), foodName);
        }
        String normalized = normalize(foodName);
        Optional<NutritionProfile> cached = loadFromCache(normalized, foodName);
        if (cached.isPresent()) {
            logger.debug("Nutrition cache hit for '{}'", normalized);
            return cached;
        }
        if (!usdaClient.isAvailable()) {
            return fallbackLookup(normalized, foodName);
        }
        String searchTerm = buildSearchTerm(foodName);
        UsdaFoodDataClient.UsdaSearchResponse searchResponse = usdaClient.searchFoods(searchTerm, 5);
        if (searchResponse.failedTransiently()) {
            logger.warn("USDA search transient failure for '{}'; skipping negative cache", foodName);
            return fallbackLookup(normalized, foodName);
        }
        if (searchResponse.results().isEmpty()) {
            saveNegativeCache(normalized);
            return fallbackLookup(normalized, foodName);
        }
        UsdaFoodDataClient.UsdaSearchResult best = pickBestResult(normalized, searchResponse.results());
        double confidence = scoreMatch(normalized, best.description());
        if (confidence < confidenceThreshold) {
            logger.info("USDA match below threshold for '{}': {} (confidence={})", foodName, best.description(), confidence);
            saveNegativeCache(normalized);
            return fallbackLookup(normalized, foodName);
        }
        UsdaFoodDataClient.UsdaDetailResponse detailResponse = usdaClient.getFoodDetails(best.fdcId(), confidence);
        if (detailResponse.failedTransiently()) {
            logger.warn("USDA detail transient failure for '{}'; skipping negative cache", foodName);
            return fallbackLookup(normalized, foodName);
        }
        NutritionProfile profile = detailResponse.profile();
        if (!detailResponse.found() || profile == null || !isPlausibleProfile(foodName, profile)) {
            saveNegativeCache(normalized);
            return fallbackLookup(normalized, foodName);
        }
        saveCache(normalized, profile);
        logger.info("USDA nutrition resolved for '{}': fdcId={} confidence={} cal/100g={}",
                foodName, profile.getFdcId(), confidence, profile.getCaloriesPer100g());
        return Optional.of(profile);
    }

    /**
     * Sums per-ingredient nutrition (USDA then fallback) into composite per-100g macros.
     */
    public Optional<NutritionProfile> blendIngredients(List<IngredientPortion> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return Optional.empty();
        }
        BlendAccumulator acc = new BlendAccumulator();
        for (IngredientPortion portion : ingredients) {
            if (portion.estimatedGrams() <= 0) {
                continue;
            }
            for (IngredientPortion expanded : expandSandwichIngredient(portion)) {
                if (!acc.add(expanded)) {
                    logger.info("Ingredient blend aborted — unresolved ingredient '{}'", expanded.name());
                    return Optional.empty();
                }
            }
        }
        if (acc.totalGrams <= 0) {
            return Optional.empty();
        }
        NutritionSource source = acc.usdaCount > 0
                ? NutritionSource.USDA
                : NutritionSource.FALLBACK_HARDCODED;
        double scale100 = 100.0 / acc.totalGrams;
        return Optional.of(new NutritionProfile(
                acc.totalCalories * scale100,
                acc.totalProtein * scale100,
                acc.totalCarbs * scale100,
                acc.totalFat * scale100,
                acc.totalFiber * scale100,
                source,
                acc.minConfidence,
                null
        ));
    }

    private final class BlendAccumulator {
        double totalGrams;
        double totalCalories;
        double totalProtein;
        double totalCarbs;
        double totalFat;
        double totalFiber;
        double minConfidence = 1.0;
        int usdaCount;

        /** @return false when the ingredient could not be resolved */
        boolean add(IngredientPortion portion) {
            String term = portion.fdcSearchTerm() != null && !portion.fdcSearchTerm().isBlank()
                    ? portion.fdcSearchTerm() : portion.name();
            NutritionProfile resolved = resolveIngredientProfile(portion.name(), term);
            if (resolved == null) {
                logger.warn("Ingredient '{}' unresolved in blend", portion.name());
                return false;
            }
            if (resolved.getSource() == NutritionSource.USDA) {
                usdaCount++;
            }
            double scale = portion.estimatedGrams() / 100.0;
            totalGrams += portion.estimatedGrams();
            totalCalories += resolved.getCaloriesPer100g() * scale;
            totalProtein += resolved.getProteinPer100g() * scale;
            totalCarbs += resolved.getCarbsPer100g() * scale;
            totalFat += resolved.getFatPer100g() * scale;
            totalFiber += resolved.getFiberPer100g() * scale;
            minConfidence = Math.min(minConfidence, resolved.getConfidence());
            return true;
        }
    }

    private List<IngredientPortion> expandSandwichIngredient(IngredientPortion portion) {
        String name = portion.name() != null ? portion.name().toLowerCase(Locale.ROOT) : "";
        if (!name.contains("sandwich") || !hasKnownSandwichProtein(name)) {
            return List.of(portion);
        }
        double total = portion.estimatedGrams();
        return List.of(
                new IngredientPortion("bread", total * 0.40, "bread wheat"),
                new IngredientPortion(sandwichProteinName(name), total * 0.60, sandwichProteinSearchTerm(name))
        );
    }

    private static boolean hasKnownSandwichProtein(String sandwichName) {
        return sandwichName.contains("turkey")
                || sandwichName.contains("chicken")
                || sandwichName.contains("ham");
    }

    private static String sandwichProteinName(String sandwichName) {
        if (sandwichName.contains("turkey")) {
            return "turkey breast";
        }
        if (sandwichName.contains("chicken")) {
            return "chicken breast";
        }
        if (sandwichName.contains("ham")) {
            return "ham";
        }
        return "deli meat";
    }

    private static String sandwichProteinSearchTerm(String sandwichName) {
        if (sandwichName.contains("turkey")) {
            return "turkey breast cooked";
        }
        if (sandwichName.contains("chicken")) {
            return "chicken breast cooked";
        }
        if (sandwichName.contains("ham")) {
            return "ham sliced";
        }
        return "lunchmeat";
    }

    private Optional<NutritionProfile> fallbackLookup(String normalized, String foodName) {
        Optional<NutritionProfile> fallback = foodNutritionFallback.resolveKnown(foodName);
        if (fallback.isEmpty()) {
            return Optional.empty();
        }
        NutritionProfile profile = fallback.get();
        if (!isPlausibleProfile(foodName, profile)) {
            return Optional.empty();
        }
        saveCache(normalized, profile);
        logger.info("Fallback nutrition for '{}': {} cal/100g", foodName, profile.getCaloriesPer100g());
        return Optional.of(profile);
    }

    private NutritionProfile resolveIngredientProfile(String foodName, String searchTerm) {
        Optional<NutritionProfile> usda = lookupIngredient(foodName, searchTerm);
        if (usda.isPresent() && isPlausibleProfile(foodName, usda.get())) {
            return usda.get();
        }
        Optional<NutritionProfile> fallback = foodNutritionFallback.resolveKnown(foodName);
        if (fallback.isPresent() && isPlausibleProfile(foodName, fallback.get())) {
            logger.debug("Ingredient fallback for '{}'", foodName);
            return fallback.get();
        }
        return null;
    }

    private boolean isPlausibleProfile(String foodName, NutritionProfile profile) {
        var validated = NutritionValidator.validateNutritionData(
                foodName, toNutritionData(profile), 100.0);
        return validated != null;
    }

    private static com.healthapp.service.AiFoodVoiceParsingService.NutritionData toNutritionData(NutritionProfile p) {
        var data = new com.healthapp.service.AiFoodVoiceParsingService.NutritionData();
        data.setCaloriesPer100g(p.getCaloriesPer100g());
        data.setProteinPer100g(p.getProteinPer100g());
        data.setCarbsPer100g(p.getCarbsPer100g());
        data.setFatPer100g(p.getFatPer100g());
        data.setFiberPer100g(p.getFiberPer100g());
        return data;
    }

    private Optional<NutritionProfile> loadFromCache(String normalized, String foodName) {
        Optional<FoodNutritionCache> cacheOpt = cacheRepository.findByNormalizedName(normalized);
        if (cacheOpt.isEmpty()) {
            return Optional.empty();
        }
        FoodNutritionCache cache = cacheOpt.get();
        if (!isCacheFresh(cache)) {
            evictCache(cache);
            return Optional.empty();
        }
        if (NEGATIVE_CACHE_SOURCE.equals(cache.getSource())) {
            return Optional.empty();
        }
        NutritionProfile profile = toProfile(cache);
        String label = foodName != null ? foodName : normalized;
        if (!isPlausibleProfile(label, profile)) {
            logger.info("Evicting implausible cached nutrition for '{}'", label);
            evictCache(cache);
            return Optional.empty();
        }
        return Optional.of(profile);
    }

    private boolean isCacheFresh(FoodNutritionCache cache) {
        LocalDateTime updatedAt = cache.getUpdatedAt() != null ? cache.getUpdatedAt() : cache.getCreatedAt();
        if (updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(LocalDateTime.now().minusDays(properties.getCacheTtlDays()));
    }

    private void evictCache(FoodNutritionCache cache) {
        cacheRepository.delete(cache);
    }

    private NutritionProfile toProfile(FoodNutritionCache cache) {
        NutritionSource source;
        try {
            source = NutritionSource.valueOf(cache.getSource());
        } catch (IllegalArgumentException e) {
            source = NutritionSource.LLM;
        }
        return new NutritionProfile(
                cache.getCaloriesPer100g(),
                safe(cache.getProteinPer100g()),
                safe(cache.getCarbsPer100g()),
                safe(cache.getFatPer100g()),
                safe(cache.getFiberPer100g()),
                source,
                safe(cache.getConfidence()),
                cache.getFdcId()
        );
    }

    private void saveNegativeCache(String normalized) {
        FoodNutritionCache cache = new FoodNutritionCache();
        cache.setNormalizedName(normalized);
        cache.setSource(NEGATIVE_CACHE_SOURCE);
        cache.setCaloriesPer100g(0.0);
        cache.setProteinPer100g(0.0);
        cache.setCarbsPer100g(0.0);
        cache.setFatPer100g(0.0);
        cache.setFiberPer100g(0.0);
        cache.setConfidence(0.0);
        persistCache(cache, normalized);
    }

    private void saveCache(String normalized, NutritionProfile profile) {
        FoodNutritionCache cache = cacheRepository.findByNormalizedName(normalized).orElse(new FoodNutritionCache());
        cache.setNormalizedName(normalized);
        cache.setFdcId(profile.getFdcId());
        cache.setSource(profile.getSource().name());
        cache.setCaloriesPer100g(profile.getCaloriesPer100g());
        cache.setProteinPer100g(profile.getProteinPer100g());
        cache.setCarbsPer100g(profile.getCarbsPer100g());
        cache.setFatPer100g(profile.getFatPer100g());
        cache.setFiberPer100g(profile.getFiberPer100g());
        cache.setConfidence(profile.getConfidence());
        persistCache(cache, normalized);
    }

    private void persistCache(FoodNutritionCache cache, String normalized) {
        try {
            cacheRepository.save(cache);
        } catch (DataIntegrityViolationException e) {
            logger.debug("Cache write race for '{}', retrying update", normalized);
            FoodNutritionCache existing = cacheRepository.findByNormalizedName(normalized)
                    .orElseThrow(() -> e);
            existing.setFdcId(cache.getFdcId());
            existing.setSource(cache.getSource());
            existing.setCaloriesPer100g(cache.getCaloriesPer100g());
            existing.setProteinPer100g(cache.getProteinPer100g());
            existing.setCarbsPer100g(cache.getCarbsPer100g());
            existing.setFatPer100g(cache.getFatPer100g());
            existing.setFiberPer100g(cache.getFiberPer100g());
            existing.setConfidence(cache.getConfidence());
            cacheRepository.save(existing);
        }
    }

    private UsdaFoodDataClient.UsdaSearchResult pickBestResult(String normalized, List<UsdaFoodDataClient.UsdaSearchResult> results) {
        UsdaFoodDataClient.UsdaSearchResult best = results.get(0);
        double bestScore = scoreMatch(normalized, best.description());
        for (UsdaFoodDataClient.UsdaSearchResult candidate : results) {
            double score = scoreMatch(normalized, candidate.description());
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    static double scoreMatch(String query, String description) {
        String q = normalize(query);
        String d = normalize(description);
        if (q.equals(d)) {
            return 1.0;
        }
        if (d.contains(q)) {
            return 0.85;
        }
        String[] qTokens = q.split("\\s+");
        int hits = 0;
        for (String token : qTokens) {
            if (token.length() > 2 && d.contains(token)) {
                hits++;
            }
        }
        if (qTokens.length == 0) {
            return 0;
        }
        return (double) hits / qTokens.length;
    }

    static String buildSearchTerm(String foodName) {
        String n = foodName.toLowerCase(Locale.ROOT);
        if (n.contains("avocado")) {
            return "avocado raw";
        }
        if (n.contains("egg")) {
            return "egg boiled";
        }
        if (n.contains("banana")) {
            return "banana raw";
        }
        if (n.contains("cashew")) {
            return "cashew nuts raw";
        }
        if (n.contains("almond")) {
            return "almonds raw";
        }
        if (n.contains("chia")) {
            return "chia seeds dried";
        }
        if (n.contains("blueberr")) {
            return "blueberries raw";
        }
        if (n.contains("rolled oats") || (n.contains("oat") && !n.contains("milk") && !n.contains("meal"))) {
            return "oats rolled dry";
        }
        if (n.contains("whole milk") || (n.contains("milk") && !n.contains("shake") && !n.contains("almond"))) {
            return "milk whole";
        }
        if (n.contains("apple")) {
            return "apple raw";
        }
        if (n.contains("carbonara")) {
            return "pasta carbonara";
        }
        if (n.contains("chicken") && n.contains("breast")) {
            return "chicken breast cooked";
        }
        if (n.contains("salmon") && (n.contains("grill") || n.contains("grilled"))) {
            return "salmon cooked";
        }
        if (n.contains("quinoa") && n.contains("cook")) {
            return "quinoa cooked";
        }
        if (n.contains("quinoa")) {
            return "quinoa cooked";
        }
        if (n.contains("broccoli")) {
            return "broccoli cooked";
        }
        if (n.contains("whole wheat") && n.contains("toast")) {
            return "whole wheat bread";
        }
        if (n.contains("toast") || n.contains("bread")) {
            return "bread wheat";
        }
        if (n.contains("peanut butter")) {
            return "peanut butter";
        }
        if (n.contains("butter") && !n.contains("peanut")) {
            return "butter salted";
        }
        if (n.contains("lassi") || n.contains("mango lassi")) {
            return "mango lassi";
        }
        if (n.contains("olive oil")) {
            return "olive oil";
        }
        if (n.contains("black coffee") || (n.contains("coffee") && !n.contains("latte"))) {
            return "coffee brewed";
        }
        if (n.contains("coke") || n.contains("cola") || n.contains("soda")) {
            return "cola soft drink";
        }
        if (n.contains("wine")) {
            return "wine table red";
        }
        if (n.contains("cappuccino") || n.contains("latte")) {
            return "coffee latte";
        }
        if (n.contains("cookie")) {
            return "cookie";
        }
        if (n.contains("muffin")) {
            return "muffin blueberry";
        }
        if (n.contains("burger")) {
            return "hamburger";
        }
        if (n.contains("fries") || n.contains("french fry")) {
            return "french fries";
        }
        if (n.contains("steak") || n.contains("sirloin")) {
            return "beef steak grilled";
        }
        if (n.contains("mashed potato")) {
            return "mashed potatoes";
        }
        if (n.contains("green bean")) {
            return "green beans cooked";
        }
        if (n.contains("cake")) {
            return "chocolate cake";
        }
        if (n.contains("ice cream")) {
            return "ice cream vanilla";
        }
        if (n.contains("pizza")) {
            return "pizza cheese";
        }
        if (n.contains("cheese") && !n.contains("cake")) {
            return "cheese cheddar";
        }
        if (n.contains("whey") || n.contains("protein powder")) {
            return "whey protein powder";
        }
        return foodName;
    }

    static String normalize(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (normalized.length() > MAX_NORMALIZED_NAME_LENGTH) {
            return normalized.substring(0, MAX_NORMALIZED_NAME_LENGTH);
        }
        return normalized;
    }

    private static double safe(Double value) {
        return value != null ? value : 0.0;
    }

    public record IngredientPortion(String name, double estimatedGrams, String fdcSearchTerm) {}
}
