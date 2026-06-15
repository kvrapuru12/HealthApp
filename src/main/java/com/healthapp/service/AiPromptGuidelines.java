package com.healthapp.service;

/**
 * Shared fragments for OpenAI system prompts so inferred amounts and defaults stay consistent across APIs.
 */
public final class AiPromptGuidelines {

    private AiPromptGuidelines() {}

    /**
     * Use in any voice/parse flow that infers unstated numeric quantities or defaults.
     */
    public static final String SHARED_INFERENCE_PRINCIPLES = """
        Inferred quantities and defaults (when the user did not specify a number):
        - Prefer typical everyday amounts: common single portions, label-style servings, usual session lengths, or ordinary population defaults — not the largest plausible value unless the user clearly implied scale (e.g. "huge", "family-size", "marathon", "all day", "finished the bag").
        - Treat every guess as an editable default: put concrete inferred values in the structured output and/or notes so assumptions stay visible and correctable.
        - Avoid silently mapping vague phrases ("a serving", "a portion", "some") to oversized plates, full packages, or maximal restaurant sizes.
        """;

    public static final String FOOD_PORTION_ASSUMPTION_RULES = """
        Food-specific unstated portions (apply together with the general inference principles above):
        - Packaged snacks (potato crisps/chips, pretzels, crackers, tortilla chips): typical label band **~25–40 g** per assumed serving unless the user implied share/large/bucket.
        - French fries / chip-shop chips as a side: often **~100–150 g** when fries are clearly meant; do not use crisp-sized grams for those.
        - Sugary drinks / juice / soda: glass or typical single bottle **~250–355 ml** unless "can" / size words imply otherwise.
        - Wine / beer / spirits (when relevant): about **~150 ml wine**, **~330–355 ml beer**, **~45 ml spirits** per unstated single drink unless specified.
        - Sandwiches / burgers / plated curry or rice mains: **one prepared item** often **~150–250 g** edible total depending on type; avoid doubling implied weight for one meal.
        - Nuts / trail mix / candy: small portion **~15–30 g** or modest piece counts, not a full bag unless implied.
        - Oils, butter, nut butters: lean toward **1 tsp–1 tbsp** when the user did not describe deep-frying or "loaded".
        - Cheese boards / charcuterie: assume **~120g cheese + ~40g crackers** when amounts are unstated; keep wine as separate beverage items.
        - Buffet / all-you-can-eat: group similar items (e.g. "12 pc salmon avocado sushi ~360g") rather than listing every piece separately.
        """;

    public static final String ACTIVITY_DURATION_ASSUMPTION_RULES = """
        Activity duration when unstated (together with the general inference principles above):
        - Infer durationMinutes from activity type using ordinary sessions: e.g. neighborhood walk **~20–30 min**, gym weights **~30–45 min**, yoga **~45–60 min**, swim **~30 min**, casual bike **~25–40 min** — mid-range defaults, not elite endurance unless implied.
        - Very vague activity ("I stretched", "quick warmup"): prefer **~10–15 min** rather than a long workout.
        - **note** string (same visibility rules as food logs): always start with **`Voice:`** plus the user's exact utterance (quoted or verbatim). If they explicitly stated duration and/or when the activity happened, append **`Stated:`** with only those user-given facts. If you inferred **durationMinutes** and/or **loggedAt** because the user did not fully specify them, append **`Assumed:`** with concrete values (e.g. minutes chosen; ISO datetime and brief cue like "from this morning"). Omit the **`Assumed:`** clause entirely when nothing was inferred for those fields. Never write `Assumed: none`.
        """;

    public static final String CYCLE_DEFAULT_ASSUMPTION_RULES = """
        Cycle metrics when unstated (together with the general inference principles above):
        - Prefer cycleLength near **28** days unless the user cited a different typical range (common band roughly **21–35**); periodDuration near **5** unless they implied very short/long (typical band often **3–7**).
        - Prefer isCycleRegular **true** unless they clearly described irregularity.
        """;

    /**
     * Few-shot examples for food voice parsing (composite vs simple, portions, beverages).
     */
    public static final String FOOD_VOICE_FEW_SHOT_EXAMPLES = """
        FEW-SHOT EXAMPLES (follow this structure exactly):

        Input: "had a banana"
        Output: {"compositeMeals":[],"foodItems":[{"foodName":"banana","quantity":1,"unit":"medium","estimatedGrams":120,"mealType":"snack","loggedAt":"<CURRENT>","note":"Stated: had a banana. Assumed: 1 medium banana ~120g.","nutrition":{"caloriesPer100g":89,"proteinPer100g":1.1,"carbsPer100g":23,"fatPer100g":0.3,"fiberPer100g":2.6}}]}

        Input: "200g grilled salmon, 1 cup cooked quinoa, steamed broccoli with olive oil drizzle for dinner"
        Output: {"compositeMeals":[{"displayName":"grilled salmon with quinoa and broccoli","approximateTotalGrams":435,"mealType":"dinner","loggedAt":"<CURRENT>","note":"Stated: 200g salmon, 1 cup quinoa, broccoli with olive oil. Assumed: broccoli ~45g, olive oil ~5g.","nutrition":{"caloriesPer100g":165,"proteinPer100g":18,"carbsPer100g":12,"fatPer100g":7,"fiberPer100g":2},"ingredients":[{"name":"grilled salmon","estimatedGrams":200,"fdcSearchTerm":"salmon, cooked"},{"name":"cooked quinoa","estimatedGrams":185,"fdcSearchTerm":"quinoa, cooked"},{"name":"steamed broccoli","estimatedGrams":45,"fdcSearchTerm":"broccoli, cooked"},{"name":"olive oil","estimatedGrams":5,"fdcSearchTerm":"olive oil"}]}],"foodItems":[]}

        Input: "homemade chicken biryani with raita and a mango lassi for dinner"
        Output: {"compositeMeals":[{"displayName":"chicken biryani with raita","approximateTotalGrams":350,"mealType":"dinner","loggedAt":"<CURRENT>","note":"Stated: chicken biryani with raita for dinner. Assumed: biryani ~300g, raita ~50g.","nutrition":{"caloriesPer100g":180,"proteinPer100g":8,"carbsPer100g":22,"fatPer100g":7,"fiberPer100g":1.5},"ingredients":[{"name":"chicken biryani","estimatedGrams":300,"fdcSearchTerm":"chicken biryani"},{"name":"raita","estimatedGrams":50,"fdcSearchTerm":"yogurt raita"}]}],"foodItems":[{"foodName":"mango lassi","quantity":1,"unit":"glass","estimatedGrams":300,"mealType":"dinner","loggedAt":"<CURRENT>","note":"Stated: mango lassi. Assumed: 1 glass ~300g.","nutrition":{"caloriesPer100g":83,"proteinPer100g":3,"carbsPer100g":14,"fatPer100g":2,"fiberPer100g":0.5}}]}

        Input: "latte with oat milk and a blueberry muffin"
        Output: {"compositeMeals":[{"displayName":"blueberry muffin","approximateTotalGrams":110,"mealType":"snack","loggedAt":"<CURRENT>","note":"Stated: blueberry muffin. Assumed: 1 bakery muffin ~110g.","nutrition":{"caloriesPer100g":380,"proteinPer100g":5,"carbsPer100g":50,"fatPer100g":18,"fiberPer100g":2},"ingredients":[{"name":"blueberry muffin","estimatedGrams":110,"fdcSearchTerm":"blueberry muffin"}]}],"foodItems":[{"foodName":"latte with oat milk","quantity":1,"unit":"cup","estimatedGrams":300,"mealType":"snack","loggedAt":"<CURRENT>","note":"Stated: latte with oat milk. Assumed: 1 medium cup ~300g.","nutrition":{"caloriesPer100g":65,"proteinPer100g":2.5,"carbsPer100g":8,"fatPer100g":3.5,"fiberPer100g":0.5}}]}

        Input: "two glasses of red wine with cheese board"
        Output: {"compositeMeals":[{"displayName":"cheese board","approximateTotalGrams":160,"mealType":"snack","loggedAt":"<CURRENT>","note":"Stated: cheese board. Assumed: ~120g cheese + ~40g crackers.","nutrition":{"caloriesPer100g":350,"proteinPer100g":18,"carbsPer100g":12,"fatPer100g":28,"fiberPer100g":0.5},"ingredients":[{"name":"cheese","estimatedGrams":120,"fdcSearchTerm":"cheese cheddar"},{"name":"crackers","estimatedGrams":40,"fdcSearchTerm":"crackers"}]}],"foodItems":[{"foodName":"red wine","quantity":2,"unit":"glass","estimatedGrams":300,"mealType":"snack","loggedAt":"<CURRENT>","note":"Stated: two glasses red wine. Assumed: 2 glasses ~300ml total.","nutrition":{"caloriesPer100g":85,"proteinPer100g":0.1,"carbsPer100g":2.6,"fatPer100g":0,"fiberPer100g":0}}]}

        Input: "all you can eat sushi dinner about 12 pieces salmon avocado and 4 tuna rolls"
        Output: {"compositeMeals":[{"displayName":"sushi dinner","approximateTotalGrams":520,"mealType":"dinner","loggedAt":"<CURRENT>","note":"Stated: AYCE sushi ~12 salmon avocado pieces + 4 tuna rolls. Assumed: grouped portions ~520g total.","nutrition":{"caloriesPer100g":180,"proteinPer100g":9,"carbsPer100g":24,"fatPer100g":5,"fiberPer100g":1},"ingredients":[{"name":"salmon avocado sushi","estimatedGrams":360,"fdcSearchTerm":"sushi salmon avocado"},{"name":"tuna rolls","estimatedGrams":160,"fdcSearchTerm":"sushi tuna roll"}]}],"foodItems":[]}

        Input: "had some snacks in the afternoon"
        Output: {"compositeMeals":[],"foodItems":[{"foodName":"snacks","quantity":1,"unit":"serving","estimatedGrams":80,"mealType":"snack","loggedAt":"<CURRENT>","note":"Stated: some snacks in the afternoon. Assumed: modest snack portion ~80g (not a full bag).","nutrition":{"caloriesPer100g":450,"proteinPer100g":6,"carbsPer100g":50,"fatPer100g":25,"fiberPer100g":3}}]}
        """;

    /**
     * Cycle-sync recommendation JSON is qualitative; avoid inventing precise portions there.
     */
    public static final String RECOMMENDATION_QUALITATIVE_GUIDANCE = """
            Do not invent precise gram weights, per-item calories, or exact portion sizes in this JSON; keep eat/move guidance qualitative and pattern-based.
            """;
}
