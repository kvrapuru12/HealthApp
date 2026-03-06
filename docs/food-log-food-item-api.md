# Food Log & Food Item API Reference

Base path: **`/api`**

All endpoints require authentication unless otherwise noted.

---

## 1. Food Logs (`/api/food-logs`)

### 1.1 List food logs

**`GET /api/food-logs`**

**Query parameters**

| Parameter   | Type     | Required | Default   | Description                                                                 |
|------------|----------|----------|-----------|-----------------------------------------------------------------------------|
| `userId`   | Long     | No       | —         | Filter by user ID. Non-admins can only use their own ID or omit (own logs). |
| `from`     | DateTime | No       | —         | Start of time range (ISO 8601, e.g. `2025-03-01T00:00:00Z`).                |
| `to`       | DateTime | No       | —         | End of time range (ISO 8601).                                               |
| `mealType` | String   | No       | —         | One of: `breakfast`, `lunch`, `dinner`, `snack`.                            |
| `page`     | Integer  | No       | `1`       | Page number.                                                                |
| `limit`    | Integer  | No       | `20`      | Items per page.                                                             |
| `sortBy`   | String   | No       | `loggedAt`| Sort field: `loggedAt` or `createdAt`.                                      |
| `sortDir`  | String   | No       | `desc`    | Sort direction: `asc` or `desc`.                                            |

**Response: `200 OK`**

```json
{
  "foodLogs": [
    {
      "id": 1,
      "userId": 10,
      "foodItemId": 5,
      "foodItemName": "Grilled Chicken",
      "loggedAt": "2025-03-03T12:00:00Z",
      "mealType": "lunch",
      "quantity": 150.0,
      "unit": "grams",
      "calories": 248.0,
      "protein": 31.5,
      "carbs": 0.0,
      "fat": 14.0,
      "fiber": 0.0,
      "note": null,
      "createdAt": "2025-03-03T12:05:00Z",
      "updatedAt": "2025-03-03T12:05:00Z"
    }
  ],
  "currentPage": 1,
  "totalPages": 3,
  "totalItems": 45,
  "itemsPerPage": 20,
  "hasNext": true,
  "hasPrevious": false,
  "totalCalories": 1850.5,
  "totalProtein": 120.0,
  "totalCarbs": 180.0,
  "totalFat": 65.0,
  "totalFiber": 25.0
}
```

---

### 1.2 Get food log by ID

**`GET /api/food-logs/{id}`**

**Path parameters**

| Parameter | Type | Description      |
|-----------|------|------------------|
| `id`      | Long | Food log ID.     |

**Response: `200 OK`**

Same single-log shape as one element of `foodLogs` in the list response above:

```json
{
  "id": 1,
  "userId": 10,
  "foodItemId": 5,
  "foodItemName": "Grilled Chicken",
  "loggedAt": "2025-03-03T12:00:00Z",
  "mealType": "lunch",
  "quantity": 150.0,
  "unit": "grams",
  "calories": 248.0,
  "protein": 31.5,
  "carbs": 0.0,
  "fat": 14.0,
  "fiber": 0.0,
  "note": null,
  "createdAt": "2025-03-03T12:05:00Z",
  "updatedAt": "2025-03-03T12:05:00Z"
}
```

**Responses:** `404` Not Found, `401` Unauthorized, `403` Forbidden (not owner).

---

### 1.3 Create food log (POST)

**`POST /api/food-logs`**

**Request body (JSON)**

| Field         | Type     | Required | Constraints                    | Description                              |
|---------------|----------|----------|--------------------------------|------------------------------------------|
| `userId`      | Long     | Yes      | ≥ 1                            | User who is logging the food.            |
| `foodItemId`  | Long     | Yes      | ≥ 1                            | ID of the food item.                     |
| `loggedAt`    | DateTime | Yes      | —                              | When the food was consumed (e.g. `2025-03-03T12:00:00Z`). |
| `quantity`    | Double   | Yes      | 1.0–2000.0                     | Amount consumed.                         |
| `mealType`    | String   | No       | —                              | Optional: breakfast, lunch, dinner, snack. |
| `unit`        | String   | No       | max 20 chars                    | Unit of measurement.                     |
| `note`        | String   | No       | max 200 chars                   | Optional note.                           |

**Example request**

```json
{
  "userId": 10,
  "foodItemId": 5,
  "loggedAt": "2025-03-03T12:00:00Z",
  "quantity": 150.0,
  "mealType": "lunch",
  "unit": "grams",
  "note": "With salad"
}
```

**Response: `201 Created`**

```json
{
  "id": 1,
  "calories": 248.0,
  "protein": 31.5,
  "carbs": 0.0,
  "fat": 14.0,
  "fiber": 0.0,
  "createdAt": "2025-03-03T12:05:00Z"
}
```

**Responses:** `400` Invalid request, `401` Unauthorized.

---

### 1.4 Update food log (PATCH)

**`PATCH /api/food-logs/{id}`**

**Path parameters**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `id`      | Long | Food log ID. |

**Request body (JSON)** — all fields optional (partial update)

| Field      | Type     | Required | Constraints   | Description        |
|------------|----------|----------|---------------|--------------------|
| `loggedAt` | DateTime | No       | —             | New logged time.   |
| `mealType` | String   | No       | —             | breakfast, lunch, dinner, snack. |
| `quantity` | Double   | No       | 1.0–2000.0    | New quantity.      |
| `unit`     | String   | No       | max 20 chars  | New unit.          |
| `note`     | String   | No       | max 200 chars | New note.          |

**Example request**

```json
{
  "quantity": 200.0,
  "mealType": "dinner",
  "note": "Updated portion"
}
```

**Response: `200 OK`**

```json
{
  "message": "updated",
  "updatedAt": "2025-03-03T14:30:00Z"
}
```

**Responses:** `400` Invalid request, `401` Unauthorized, `403` Forbidden, `404` Not Found.

---

### 1.5 Delete food log

**`DELETE /api/food-logs/{id}`**

**Path parameters**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `id`      | Long | Food log ID. |

**Response: `200 OK`**

```json
{
  "message": "deleted"
}
```

**Responses:** `401` Unauthorized, `403` Forbidden, `404` Not Found.

---

## 2. Food Items (`/api/foods`)

### 2.1 List food items

**`GET /api/foods`**

**Query parameters**

| Parameter    | Type    | Required | Default   | Description                                      |
|-------------|---------|----------|-----------|--------------------------------------------------|
| `search`    | String  | No       | —         | Search by name.                                  |
| `visibility`| String  | No       | —         | Filter: `private` or `public`.                   |
| `page`      | Integer | No       | `1`       | Page number.                                     |
| `limit`     | Integer | No       | `20`      | Items per page.                                  |
| `sortBy`    | String  | No       | `createdAt` | Sort field: `name` or `createdAt`.             |
| `sortDir`   | String  | No       | `desc`    | Sort direction: `asc` or `desc`.                 |

**Response: `200 OK`**

```json
{
  "foodItems": [
    {
      "id": 5,
      "name": "Grilled Chicken",
      "category": "Protein",
      "defaultUnit": "grams",
      "quantityPerUnit": 100.0,
      "caloriesPerUnit": 165,
      "proteinPerUnit": 31.0,
      "carbsPerUnit": 0.0,
      "fatPerUnit": 3.6,
      "fiberPerUnit": 0.0,
      "visibility": "private",
      "createdAt": "2025-02-01T10:00:00Z",
      "updatedAt": "2025-02-01T10:00:00Z"
    }
  ],
  "currentPage": 1,
  "totalPages": 2,
  "totalItems": 25,
  "itemsPerPage": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

---

### 2.2 Get food item by ID

**`GET /api/foods/{id}`**

**Path parameters**

| Parameter | Type | Description   |
|-----------|------|---------------|
| `id`      | Long | Food item ID. |

**Response: `200 OK`**

Same single-item shape as one element of `foodItems` in the list response:

```json
{
  "id": 5,
  "name": "Grilled Chicken",
  "category": "Protein",
  "defaultUnit": "grams",
  "quantityPerUnit": 100.0,
  "caloriesPerUnit": 165,
  "proteinPerUnit": 31.0,
  "carbsPerUnit": 0.0,
  "fatPerUnit": 3.6,
  "fiberPerUnit": 0.0,
  "visibility": "private",
  "createdAt": "2025-02-01T10:00:00Z",
  "updatedAt": "2025-02-01T10:00:00Z"
}
```

**Responses:** `404` Not Found, `401` Unauthorized, `403` Forbidden (if not owner and not public).

---

### 2.3 Create food item (POST)

**`POST /api/foods`**

**Request body (JSON)**

| Field             | Type    | Required | Constraints       | Default  | Description                    |
|-------------------|---------|----------|-------------------|----------|--------------------------------|
| `name`            | String  | Yes      | 3–100 chars        | —        | Food name.                     |
| `category`        | String  | No       | max 50 chars       | —        | Category (e.g. Protein).       |
| `defaultUnit`     | String  | No       | max 20 chars       | `"grams"`| Default unit.                  |
| `quantityPerUnit` | Double  | No       | 1.0–1000.0         | `100.0`  | Quantity this unit represents. |
| `weightPerUnit`   | Double  | No       | 0.1–10000.0        | `100.0`  | Weight per unit (e.g. grams).  |
| `caloriesPerUnit` | Integer | Yes      | 1–2000             | —        | Calories per unit.             |
| `proteinPerUnit`  | Double  | No       | 0.0–100.0          | —        | Protein (g) per unit.          |
| `carbsPerUnit`    | Double  | No       | 0.0–100.0          | —        | Carbs (g) per unit.           |
| `fatPerUnit`      | Double  | No       | 0.0–100.0          | —        | Fat (g) per unit.             |
| `fiberPerUnit`    | Double  | No       | 0.0–50.0           | —        | Fiber (g) per unit.           |
| `visibility`      | String  | No       | `"private"` or `"public"` | `"private"` | Visibility.        |

**Example request**

```json
{
  "name": "Grilled Chicken Breast",
  "category": "Protein",
  "defaultUnit": "grams",
  "quantityPerUnit": 100.0,
  "weightPerUnit": 100.0,
  "caloriesPerUnit": 165,
  "proteinPerUnit": 31.0,
  "carbsPerUnit": 0.0,
  "fatPerUnit": 3.6,
  "fiberPerUnit": 0.0,
  "visibility": "private"
}
```

**Response: `201 Created`**

```json
{
  "id": 5,
  "createdAt": "2025-03-03T10:00:00Z"
}
```

**Responses:** `400` Invalid request, `401` Unauthorized.

---

### 2.4 Update food item (PATCH)

**`PATCH /api/foods/{id}`**

**Path parameters**

| Parameter | Type | Description   |
|-----------|------|---------------|
| `id`      | Long | Food item ID. |

**Request body (JSON)** — all fields optional (partial update)

| Field             | Type    | Required | Constraints       | Description        |
|-------------------|---------|----------|-------------------|--------------------|
| `name`            | String  | No       | 3–100 chars        | New name.          |
| `category`        | String  | No       | max 50 chars       | New category.      |
| `defaultUnit`     | String  | No       | max 20 chars       | New default unit.  |
| `quantityPerUnit` | Double  | No       | 1.0–1000.0         | New quantity/unit. |
| `caloriesPerUnit` | Integer | No       | 1–2000             | New calories/unit. |
| `proteinPerUnit`  | Double  | No       | 0.0–100.0          | New protein/unit.  |
| `carbsPerUnit`    | Double  | No       | 0.0–100.0          | New carbs/unit.    |
| `fatPerUnit`      | Double  | No       | 0.0–100.0          | New fat/unit.      |
| `fiberPerUnit`    | Double  | No       | 0.0–50.0           | New fiber/unit.    |
| `visibility`      | String  | No       | `private` / `public` | New visibility.  |

**Example request**

```json
{
  "name": "Grilled Chicken Breast (skinless)",
  "caloriesPerUnit": 160,
  "proteinPerUnit": 32.0
}
```

**Response: `200 OK`**

```json
{
  "message": "updated",
  "updatedAt": "2025-03-03T14:00:00Z"
}
```

**Responses:** `400` Invalid request, `401` Unauthorized, `403` Forbidden, `404` Not Found.

---

### 2.5 Delete food item

**`DELETE /api/foods/{id}`**

**Path parameters**

| Parameter | Type | Description   |
|-----------|------|---------------|
| `id`      | Long | Food item ID. |

**Response: `200 OK`**

```json
{
  "message": "deleted"
}
```

**Responses:** `401` Unauthorized, `403` Forbidden, `404` Not Found.

---

## Summary

| Resource   | Method | Endpoint              | Request body              | Response body                    |
|-----------|--------|------------------------|---------------------------|----------------------------------|
| Food Log  | GET    | `/api/food-logs`       | —                         | `FoodLogPaginatedResponse`       |
| Food Log  | GET    | `/api/food-logs/{id}`  | —                         | `FoodLogResponse`                 |
| Food Log  | POST   | `/api/food-logs`       | `FoodLogCreateRequest`     | `FoodLogCreateResponse` (201)    |
| Food Log  | PATCH  | `/api/food-logs/{id}`  | `FoodLogUpdateRequest`     | `{ message, updatedAt }`         |
| Food Log  | DELETE | `/api/food-logs/{id}`  | —                         | `{ message: "deleted" }`         |
| Food Item | GET    | `/api/foods`           | —                         | `FoodItemPaginatedResponse`      |
| Food Item | GET    | `/api/foods/{id}`      | —                         | `FoodItemResponse`               |
| Food Item | POST   | `/api/foods`           | `FoodItemCreateRequest`   | `FoodItemCreateResponse` (201)   |
| Food Item | PATCH  | `/api/foods/{id}`      | `FoodItemUpdateRequest`   | `{ message, updatedAt }`         |
| Food Item | DELETE | `/api/foods/{id}`      | —                         | `{ message: "deleted" }`         |

**Date/time format:** ISO 8601, e.g. `yyyy-MM-dd'T'HH:mm:ss'Z'` (e.g. `2025-03-03T12:00:00Z`).
