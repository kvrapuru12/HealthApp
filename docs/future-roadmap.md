# Future Roadmap

This document tracks planned features that are approved for future implementation.

## Planned Features

### Backend-driven food icon metadata

Status: Planned (not implemented yet)

Goal:
- Improve food icon consistency across all app screens by using backend-provided metadata instead of frontend keyword guessing.

Scope:
- Add `iconKey` and optional `imageKey` fields to food item data.
- Return these fields in food item APIs.
- Render food visuals in frontend directly from backend keys.
- Keep current keyword/category fallback only for legacy items that do not yet have metadata.

Why this matters:
- Prevents mismatched icons.
- Keeps visual logic consistent across Food Tracking, Add Food, and future food-related screens.
- Reduces duplicated mapping logic in frontend components.

Implementation notes (for later):
- Add DB migration for new columns in `food_items`.
- Extend DTOs (`FoodItemCreateRequest`, `FoodItemUpdateRequest`, `FoodItemResponse`).
- Update service mapping in `FoodItemService`.
- Backfill existing food items with default `iconKey` values.
- Add/update API documentation with new fields.

### Test coverage hardening for releases

Status: In progress

Current state:
- Unit and integration coverage improved, but not fully comprehensive yet.
- CI currently enforces a low baseline coverage gate (line `>= 15%`, branch `>= 5%`) to avoid blocking builds while test suites are expanded.

Release-confidence target:
- Raise overall line coverage to `>= 70%` (then toward `80%+`).
- Raise overall branch coverage to `>= 50%`.
- Achieve near-complete coverage for critical paths (authentication, core logging flows, cycle-sync recommendation logic).

Planned next steps:
- Add missing negative/edge case tests for auth and token lifecycle.
- Expand controller-level validation and failure-path tests.
- Deepen branch coverage in high-risk services (`CycleSyncRecommendationService`, `VoiceFoodLogService`, `UserService`, `ValidationService`).
- Add post-deploy smoke tests that call real deployed endpoints every release.
- Tighten JaCoCo thresholds incrementally as coverage rises.
