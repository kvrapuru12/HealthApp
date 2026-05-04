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
     * Cycle-sync recommendation JSON is qualitative; avoid inventing precise portions there.
     */
    public static final String RECOMMENDATION_QUALITATIVE_GUIDANCE = """
            Do not invent precise gram weights, per-item calories, or exact portion sizes in this JSON; keep eat/move guidance qualitative and pattern-based.
            """;
}
