# Weight Tracking Implementation

## Overview
This document describes the complete implementation of weight tracking functionality for the HealthApp, following the established patterns and architecture of the existing codebase.

## Features Implemented

### ✅ 1. POST /api/weights — Create Weight Entry
**Endpoint:** `POST /api/weights`

**Request Body:**
```json
{
  "userId": 12,
  "loggedAt": "2025-08-14T07:00:00Z",
  "weight": 62.3,
  "note": "Morning, post-bathroom"
}
```

**Validations:**
- `userId`: Required integer; must match authenticated user unless admin
- `loggedAt`: Required ISO-8601 timestamp; ≤ now + 10 minutes
- `weight`: Required number in kg; Min: 30, Max: 300
- `note`: Optional string; ≤ 200 characters

**Response (201 Created):**
```json
{
  "id": 431,
  "createdAt": "2025-08-14T07:01:00Z"
}
```

**Behavior:** Auto-updates User.weight if this is the most recent loggedAt entry.

### ✅ 2. GET /api/weights/{id} — Get Weight Entry by ID
**Endpoint:** `GET /api/weights/{id}`

**Validations:**
- `id`: Must be positive BIGINT
- Must belong to authenticated user unless admin

**Response:**
```json
{
  "id": 431,
  "userId": 12,
  "loggedAt": "2025-08-14T07:00:00Z",
  "weight": 62.3,
  "note": "Morning, post-bathroom",
  "status": "active",
  "createdAt": "2025-08-14T07:01:00Z",
  "updatedAt": "2025-08-14T07:01:00Z"
}
```

### ✅ 3. GET /api/weights — List Weight Entries
**Endpoint:** `GET /api/weights?userId&from&to&page&limit&sortBy&sortDir`

**Query Parameters:**
- `userId`: Optional (admin only). Ignored for normal users
- `from`, `to`: Optional ISO-8601; if both, ensure from <= to
- `page`: ≥ 1
- `limit`: 1–100
- `sortBy`: loggedAt, createdAt
- `sortDir`: asc, desc

**Response:**
```json
{
  "items": [
    {
      "id": 431,
      "userId": 12,
      "loggedAt": "2025-08-14T07:00:00Z",
      "weight": 62.3,
      "note": "Morning, post-bathroom",
      "status": "active",
      "createdAt": "2025-08-14T07:01:00Z",
      "updatedAt": "2025-08-14T07:01:00Z"
    }
  ],
  "page": 1,
  "limit": 20,
  "total": 5
}
```

### ✅ 4. PATCH /api/weights/{id} — Update Weight Entry
**Endpoint:** `PATCH /api/weights/{id}`

**Request Body:**
```json
{
  "weight": 62.0,
  "note": "Corrected after recheck"
}
```

**Validations:**
- `id`: Must be valid and belong to auth user (unless admin)
- `loggedAt`: If present, ≤ now + 10 min
- `weight`: If present, number 30–300 (kg)
- `note`: If present, ≤ 200 characters
- Immutable fields: id, userId, status, createdAt

**Response:**
```json
{
  "message": "updated",
  "updatedAt": "2025-08-14T07:25:00Z"
}
```

**Behavior:** If loggedAt is the latest, updates User.weight.

### ✅ 5. DELETE /api/weights/{id} — Soft Delete Weight Entry
**Endpoint:** `DELETE /api/weights/{id}`

**Behavior:**
- Sets status = deleted
- If deleted entry was the latest loggedAt, re-calculates most recent weight and updates User.weight

**Response:**
```json
{
  "message": "deleted"
}
```

## Database Schema

### Table: `weight_logs`
```sql
CREATE TABLE weight_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    logged_at DATETIME(6) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    note VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    -- Constraints
    CONSTRAINT fk_weight_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_weight_range CHECK (weight >= 30.0 AND weight <= 300.0),
    CONSTRAINT chk_weight_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

-- Indexes for performance optimization
CREATE INDEX idx_weight_user_logged_at ON weight_logs(user_id, logged_at);
CREATE INDEX idx_weight_logged_at ON weight_logs(logged_at);
CREATE INDEX idx_weight_status ON weight_logs(status);
CREATE INDEX idx_weight_created_at ON weight_logs(created_at);
```

## Files Created

### 1. Database Migration
- `src/main/resources/db/migration/V10__create_weight_logs_table.sql`

### 2. Entity
- `src/main/java/com/healthapp/entity/WeightEntry.java`

### 3. DTOs
- `src/main/java/com/healthapp/dto/WeightCreateRequest.java`
- `src/main/java/com/healthapp/dto/WeightCreateResponse.java`
- `src/main/java/com/healthapp/dto/WeightResponse.java`
- `src/main/java/com/healthapp/dto/WeightPaginatedResponse.java`
- `src/main/java/com/healthapp/dto/WeightUpdateRequest.java`

### 4. Repository
- `src/main/java/com/healthapp/repository/WeightEntryRepository.java`

### 5. Service
- `src/main/java/com/healthapp/service/WeightEntryService.java`

### 6. Controller
- `src/main/java/com/healthapp/controller/WeightController.java`

### 7. Tests
- `src/test/java/com/healthapp/service/WeightEntryServiceTest.java`

## Key Features

### 🔄 Weight Synchronization Logic
The service automatically syncs the user's latest weight to the `users.weight_kg` field:
- After every insert/update/delete operation
- Finds the most recent active weight entry for the user
- Updates the user's weight field accordingly
- Handles edge cases (no entries, deleted entries)

### 🛡️ Security & Access Control
- Users can only access their own weight entries unless they are admin
- Proper validation of user permissions in all operations
- Rate limiting applied to create/update/delete operations

### 📊 Performance Optimizations
- Database indexes on frequently queried columns
- Efficient pagination and filtering
- Optimized queries for date range searches

### ✅ Validation & Error Handling
- Comprehensive input validation
- Future timestamp validation (max 10 minutes ahead)
- Duplicate entry prevention (within 5-minute window)
- Weight range validation (30-300 kg)
- Proper error responses and logging

## Testing

The implementation includes comprehensive unit tests covering:
- ✅ Weight entry creation
- ✅ Access control validation
- ✅ Weight entry updates
- ✅ Soft deletion
- ✅ Weight synchronization logic

All tests pass successfully.

## API Documentation

The endpoints are fully documented with OpenAPI/Swagger annotations, providing:
- Detailed request/response schemas
- Validation rules
- Error codes and descriptions
- Example requests and responses

## Future Enhancements

The implementation is designed to support future features:
- BMI trend charts (weight + height)
- Notifications for target weight milestones
- Smart scale sync (via Bluetooth or Google Fit APIs)
- Weight trend analysis and insights

## Usage Examples

### Create a weight entry
```bash
curl -X POST http://localhost:8080/api/weights \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "userId": 12,
    "loggedAt": "2025-08-14T07:00:00Z",
    "weight": 62.3,
    "note": "Morning, post-bathroom"
  }'
```

### Get weight entries with filtering
```bash
curl "http://localhost:8080/api/weights?from=2025-08-01T00:00:00Z&to=2025-08-31T23:59:59Z&page=1&limit=20&sortBy=loggedAt&sortDir=desc" \
  -H "Authorization: Bearer <jwt-token>"
```

### Update a weight entry
```bash
curl -X PATCH http://localhost:8080/api/weights/431 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "weight": 62.0,
    "note": "Corrected after recheck"
  }'
```

## Conclusion

The weight tracking implementation is complete and follows all the specified requirements. It integrates seamlessly with the existing codebase architecture and provides a robust, secure, and performant solution for tracking user weight measurements.
