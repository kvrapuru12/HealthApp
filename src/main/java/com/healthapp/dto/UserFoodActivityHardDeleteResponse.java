package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Counts of rows physically removed for the given user")
public record UserFoodActivityHardDeleteResponse(
        @Schema(description = "Email used for lookup") String email,
        @Schema(description = "Resolved user id") long userId,
        @Schema(description = "food_logs rows deleted") int foodLogsDeleted,
        @Schema(description = "food_items rows deleted (only when no logs reference them)") int foodItemsDeleted,
        @Schema(description = "activity_logs rows deleted") int activityLogsDeleted,
        @Schema(description = "activities rows deleted (only when no logs reference them)") int activitiesDeleted
) {}
