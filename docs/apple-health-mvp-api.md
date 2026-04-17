# Apple Health MVP API

Base URL uses app context path from config:

- Local default: `http://localhost:8080/api`

All endpoints require JWT auth:

- Header: `Authorization: Bearer <access_token>`

---

## 1) Ingest Apple Health Steps

### Endpoint

- **Method:** `POST`
- **Path:** `/integrations/apple-health/ingest`
- **Full URL (local):** `http://localhost:8080/api/integrations/apple-health/ingest`

### Request body

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
    }
  ]
}
```

### Request field rules

- `clientIngestSchemaVersion`: must be `2`.
- `samples[].localDate`: required; format `YYYY-MM-DD`; must match the sample `end` date in `anchorTimeZone`.
- `anchorTimeZone`: must be valid IANA timezone (example: `America/Los_Angeles`, `UTC`).
- `samples`: required, non-empty array.
- `samples[].metric`: MVP supports only `STEPS`.
- `samples[].externalSampleId`: stable per HealthKit sample for idempotency.
- `samples[].start` and `samples[].end`: ISO-8601 datetime with timezone/offset.
- `samples[].value`: integer between `0` and `1000000`.

### Success response (`200`)

```json
{
  "accepted": 1,
  "unchanged": 0,
  "rejected": 0,
  "affectedLocalDates": [
    "2026-04-16"
  ],
  "results": [
    {
      "externalSampleId": "HKQuantityTypeIdentifierStepCount:ABC-UUID",
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

---

## 2) Get Daily Dashboard (Merged Steps)

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
  "schemaVersion": 1,
  "generatedAt": "2026-04-16T11:10:03.456Z",
  "steps": {
    "mergePolicy": "PREFER_APPLE_HEALTH_IF_PRESENT",
    "displayedSteps": 8432,
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
      "manualIgnoredForDisplay": true
    }
  }
}
```

### Merge behavior

- If Apple data exists for `localDate`, `displayedSteps = APPLE_HEALTH` total.
- If Apple data does not exist, `displayedSteps = MANUAL_APP` total.
- `conflictFlags` become `true` when Apple exists and manual steps are non-zero but totals differ.

### Error responses

- `400`: invalid `localDate` or `timeZone`.
- `401`: missing/invalid access token.

---

## Quick FE integration checklist

- Always send authenticated JWT.
- Prefer `clientIngestSchemaVersion: 2` and send `samples[].localDate` from the UI-selected day.
- Treat ingest as idempotent; safe to retry same `externalSampleId`.
- Use only `steps.displayedSteps` for primary UI number.
- Use `steps.bySource` and `steps.conflictFlags` for transparency UI.
