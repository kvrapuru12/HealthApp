# Cycle-Sync Future Phases (Deferred)

This document captures deferred work after Phase 1 MVP for the unified cycle-sync endpoint.

## Phase 2: Contract Hardening and Safety Guardrails
- Add JSON schema validation for AI response before returning to FE.
- Enforce strict defaults and bounds:
  - `energyLevel` in allowed range
  - required strings and arrays always populated
  - fallback values for missing nested keys
- Add safety guardrails for health guidance text:
  - avoid diagnosis/treatment claims
  - avoid prescriptive medical language
  - include conservative caution notes where needed
- Expand fallback templates to ensure all 4 phase blocks are complete.

## Phase 3: Personalization Quality Improvements
- Enrich prompt context beyond `User` table:
  - recent steps/sleep/activity/food trends
  - adherence signals and recent symptom patterns
- Add preference model if missing:
  - dietary restrictions and dislikes
  - workout constraints/equipment access
- Add caching by `userId + cyclePhase + date` to reduce latency/cost.
- Add observability:
  - model latency
  - parse/validation failure rate
  - fallback rate

## Phase 4: Rollout and Validation
- Keep legacy endpoints during FE migration period:
  - `GET /ai/suggestions/cycle-sync/food`
  - `GET /ai/suggestions/cycle-sync/activity`
- Cut FE to unified endpoint after contract validation.
- Run controlled comparison and monitor quality metrics.
- Deprecate old endpoints after stable adoption window.

## Revisit Triggers
- Fallback rate consistently above threshold.
- FE requests additional fields in recommendation contract.
- Cost or latency exceeds SLO targets.
