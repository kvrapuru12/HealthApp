# Apple Health MVP API

Base URL uses app context path from config:

- Local default: `http://localhost:8080/api`

All endpoints require JWT auth:

- Header: `Authorization: Bearer <access_token>`

---

## 1) Ingest Apple Health samples (steps and sleep)

### Endpoint

- **Method:** `POST`
- **Path:** `/integrations/apple-health/ingest`
- **Full URL (local):** `http://localhost:8080/api/integrations/apple-health/ingest`

### Request body (mixed batch allowed)

```json
{
  "clientIngestSchemaVersion": 2,
  "anchorTimeZone": "America/Los_Angeles",
  "samples": [
    {
      "metric": "STEPS",
      "externalSampleId": "HKQuantityTypeIdentifierStepCount:ABC-UUID",
      "localDate": "2026-04-16",
      "start": "2026-04-15T07:00:00Z",
      "end": "2026-04-16T07:00:00Z",
      "value": 8432
    },
    {
      "metric": "SLEEP",
      "externalSampleId": "HKCategoryTypeIdentifierSleepAnalysis:DEF-UUID",
      "localDate": "2026-04-16",
      "start": "2026-04-16T04:00:00Z",
      "end": "2026-04-16T04:30:00Z",
      "sleepStage": "CORE"
    }
  ]
}
```

### Request field rules

- `clientIngestSchemaVersion`: must be `2`.
- `anchorTimeZone`: must be valid IANA timezone (example: `America/Los_Angeles`, `UTC`).
- `samples`: required, non-empty array.
- `samples[].metric`: `STEPS` or `SLEEP`.
- `samples[].externalSampleId`: stable per HealthKit sample for idempotency.
- `samples[].start` and `samples[].end`: ISO-8601 datetime with timezone/offset; `end` must not be before `start`.
- **STEPS**
  - `samples[].localDate`: required; must match the calendar date of `start` in `anchorTimeZone`.
  - `samples[].value`: required; integer between `0` and `1000000`.
- **SLEEP**
  - `samples[].localDate`: required; must match the calendar date of **`end`** in `anchorTimeZone` (wake-day attribution for overnight segments).
  - `samples[].sleepStage`: required; case-insensitive and normalized to canonical values:
    - canonical: `AWAKE`, `IN_BED`, `ASLEEP`, `ASLEEP_UNSPECIFIED`, `CORE`, `DEEP`, `REM`
    - accepted aliases: `ASLEEP_REM -> REM`, `ASLEEP_CORE -> CORE`, `ASLEEP_DEEP -> DEEP`
  - `samples[].value`: not used for `SLEEP` (duration is always derived from `end - start`, in seconds, then converted to hours for dashboard read models).

### Success response (`200`)

```json
{
  "accepted": 2,
  "unchanged": 0,
  "rejected": 0,
  "affectedLocalDates": [
    "2026-04-16"
  ],
  "results": [
    {
      "externalSampleId": "HKQuantityTypeIdentifierStepCount:ABC-UUID",
      "status": "UPSERTED"
    },
    {
      "externalSampleId": "HKCategoryTypeIdentifierSleepAnalysis:DEF-UUID",
      "status": "UPSERTED"
    }
  ],
  "serverIngestSchemaVersion": 2
}
```

### Result status meanings

- `UPSERTED`: inserted or updated.
- `UNCHANGED`: duplicate resend with same content.
- `REJECTED`: invalid sample in batch.

### Error responses

- `400`: invalid payload, unsupported schema version, invalid timezone, or batch-level validation failure.
- Invalid sample-level fields are returned as per-item `REJECTED` rows in a `200` response.
- `401`: missing/invalid access token.

### Dashboard “asleep” total (read path)

For `GET /dashboard/daily`, Apple sleep hours are derived only from segments whose `sleepStage` counts as asleep: `ASLEEP`, `ASLEEP_UNSPECIFIED`, `CORE`, `DEEP`, `REM`. Durations for `AWAKE` and `IN_BED` are stored but excluded from that sum.

---

## 2) Get Daily Dashboard (merged steps and sleep)

### Endpoint

- **Method:** `GET`
- **Path:** `/dashboard/daily`
- **Full URL (local):** `http://localhost:8080/api/dashboard/daily`

### Query params (required)

- `localDate`: format `YYYY-MM-DD` (example: `2026-04-16`)
- `timeZone`: IANA timezone (example: `America/Los_Angeles`)

### Example request

```bash
curl -X GET "http://localhost:8080/api/dashboard/daily?localDate=2026-04-16&timeZone=America/Los_Angeles" \
  -H "Authorization: Bearer <access_token>"
```

### Success response (`200`)

```json
{
  "localDate": "2026-04-16",
  "timeZone": "America/Los_Angeles",
  "schemaVersion": 2,
  "generatedAt": "2026-04-16T11:10:03.456Z",
  "steps": {
    "mergePolicy": "SUM_WHEN_BOTH_PRESENT",
    "displayedSteps": 8632,
    "resolvedSource": "BOTH",
    "bySource": [
      {
        "source": "APPLE_HEALTH",
        "steps": 8432
      },
      {
        "source": "MANUAL_APP",
        "steps": 200
      }
    ],
    "conflictFlags": {
      "manualVsAppleMismatch": true,
      "manualIgnoredForDisplay": false
    }
  },
  "sleep": {
    "mergePolicy": "SUM_WHEN_BOTH_PRESENT",
    "displayedSleepHours": 14.5,
    "resolvedSource": "BOTH",
    "bySource": [
      {
        "source": "APPLE_HEALTH",
        "hours": 7.5
      },
      {
        "source": "MANUAL_APP",
        "hours": 7.0
      }
    ],
    "conflictFlags": {
      "manualVsAppleMismatch": true,
      "manualIgnoredForDisplay": false
    }
  }
}
```

### Merge behavior (steps)

- If both sources exist for `localDate`, `displayedSteps = APPLE_HEALTH + MANUAL_APP` total and `resolvedSource = BOTH`.
- If only Apple step data exists, `displayedSteps = APPLE_HEALTH` and `resolvedSource = APPLE_HEALTH`.
- If only manual step data exists, `displayedSteps = MANUAL_APP` and `resolvedSource = MANUAL_APP`.
- If neither exists, `displayedSteps = 0` and `resolvedSource = NONE`.
- `conflictFlags.manualVsAppleMismatch` becomes `true` when both sources exist and totals differ.

### Merge behavior (sleep)

- If both sources exist for `localDate`, `displayedSleepHours = APPLE_HEALTH_ASLEEP_SUM + MANUAL_APP` and `resolvedSource = BOTH`.
- If only Apple sleep rows exist, `displayedSleepHours` uses Apple’s asleep-only sum and `resolvedSource = APPLE_HEALTH`.
- If only manual sleep exists, `displayedSleepHours` uses manual `sleep_logs.hours` sum and `resolvedSource = MANUAL_APP`.
- If neither exists, `displayedSleepHours = 0` and `resolvedSource = NONE`.
- `sleep.conflictFlags.manualVsAppleMismatch` is set when both sources exist and the two source totals differ.

### Error responses

- `400`: invalid `localDate` or `timeZone`.
- `401`: missing/invalid access token.

---

## Quick FE integration checklist

- Always send authenticated JWT.
- Prefer `clientIngestSchemaVersion: 2` and send `samples[].localDate` from the UI-selected day (steps: align with `start`; sleep: align with `end` / wake day).
- Treat ingest as idempotent; safe to retry same `externalSampleId`.
- Use `steps.displayedSteps` and `sleep.displayedSleepHours` for primary UI numbers.
- Use `*.bySource` and `*.conflictFlags` for transparency UI.
