# Social Login API – Frontend Integration

Base path for all auth endpoints: **`/api/auth`**  
Example base URL: `https://your-api-host.com/api`

All requests must use **`Content-Type: application/json`**.  
No `Authorization` header is required for these sign-in endpoints.

---

## 1. Sign in with Google

**`POST /api/auth/google`**

### Request

| Field     | Type   | Required | Description                                      |
|----------|--------|----------|--------------------------------------------------|
| `idToken` | string | Yes      | Google ID token from the client (JWT)            |
| `platform` | string | Yes      | `"ios"` or `"android"`                          |

**Example**

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...",
  "platform": "ios"
}
```

### Success response (200 OK)

Same shape as Apple sign-in (see section 2 below).

### Error responses

| Status | When | Body shape |
|--------|------|------------|
| 400 | Invalid token payload (e.g. missing email/sub) | `{ "error": string, "message": string }` |
| 401 | Invalid or expired Google token | `{ "error": "Token verification failed", "message": "Invalid or expired Google ID token" }` |
| 400 | Account inactive | `{ "error": "Account inactive", "message": "Account is not active" }` |
| 500 | Server error | `{ "error": string, "message": string }` |

---

## 2. Sign in with Apple

**`POST /api/auth/apple`**

### Request

| Field       | Type   | Required | Description |
|------------|--------|----------|-------------|
| `idToken`  | string | Yes      | Apple identity token (JWT) from the client. From iOS: `ASAuthorizationAppleIDCredential.identityToken` (e.g. base64 string or decoded to UTF-8 string). |
| `platform` | string | No       | `"ios"`, `"android"`, or `"web"`. Recommended to always send (e.g. `"ios"`). |
| `email`    | string | No       | User email. **Send on first login** when Apple provides it (may be private relay). |
| `firstName`| string | No       | First name. **Only provided by Apple on first login**; send when available. |
| `lastName` | string | No       | Last name. **Only provided by Apple on first login**; send when available. |

**Example – first login (with name and email from Apple)**

```json
{
  "idToken": "eyJraWQiOiJlWGF1Q...",
  "platform": "ios",
  "email": "user@privaterelay.appleid.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Example – subsequent logins (only token required)**

```json
{
  "idToken": "eyJraWQiOiJlWGF1Q...",
  "platform": "ios"
}
```

**Notes**

- Apple sends name and email **only on the first authorization**. Store them in your app and send in the first `POST /api/auth/apple` call so the backend can persist them.
- Email may be a private relay address (`@privaterelay.appleid.com`) or missing if the user hides it.
- Backend uses `sub` from the verified token as the stable Apple user ID; you do not need to send it in the body.

### Success response (200 OK)

Same shape for **password login** (`POST /api/auth/login`), **Google**, and **Apple** — including refresh fields:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "opaque-url-safe-string-store-securely",
  "expiresIn": 3600,
  "userId": 123,
  "username": "john_doe",
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "role": "USER",
  "gender": "MALE",
  "profileComplete": true,
  "message": "Login successful"
}
```

| Field             | Type    | Description |
|-------------------|---------|-------------|
| `token`           | string  | Short-lived **access JWT**. Send as `Authorization: Bearer <token>` (see section 3). |
| `refreshToken`    | string  | Opaque refresh token; use with `POST /api/auth/refresh` when the access token expires. |
| `expiresIn`       | number  | Access token lifetime in **seconds** (matches JWT `exp`). |
| `userId`          | number  | Backend user ID. |
| `username`        | string  | Unique username (may be derived from email or Apple ID). |
| `firstName`       | string  | First name. |
| `lastName`        | string  | Last name. |
| `email`           | string  | Email (may be private relay for Apple). |
| `role`            | string  | e.g. `"USER"`. |
| `gender`          | string? | e.g. `"MALE"`, `"FEMALE"`, or `null` if not set. |
| `profileComplete` | boolean | Whether required profile fields (e.g. DOB, gender) are set. |
| `message`         | string  | e.g. `"Login successful"`. |

### Error responses

| Status | When | Example body |
|--------|------|--------------|
| 400 | Invalid identity token format / missing `sub` | `{ "error": "Invalid token payload", "message": "Token does not contain subject (Apple user ID)" }` |
| 401 | Invalid or expired Apple token | `{ "error": "Token verification failed", "message": "Invalid or expired Apple identity token" }` |
| 400 | Account inactive | `{ "error": "Account inactive", "message": "Account is not active" }` |
| 500 | Server error | `{ "error": "Apple sign-in failed", "message": "<details>" }` |

---

## 3. Using the access token

After login or sign-in, send the access JWT on each request:

- **Header:** `Authorization: Bearer <token>`  
  The `token` field in JSON is the JWT **only** (no `"Bearer "` prefix inside the string).

When the access token expires (`expiresIn`), call **`POST /api/auth/refresh`** with the stored `refreshToken`. The server **rotates** the refresh token: each successful refresh returns a new `refreshToken`; replace the stored value.

### `POST /api/auth/refresh`

**Request**

```json
{
  "refreshToken": "<stored refresh token>"
}
```

**Success (200)** — same JSON shape as login (new `token`, new `refreshToken`, `expiresIn`, and user profile fields).

**401** — invalid, expired, or revoked refresh token (client should sign the user out).

---

## 3b. Legacy access tokens (backward compatibility)

Older clients may still send the legacy format `userId_ROLE_timestamp` (sometimes with a `"Bearer "` prefix embedded in the stored value). The API still accepts these for authentication until those clients re-login and receive JWT access tokens.

---

## 4. Backend: Apple Sign In environment variables

For **Sign in with Apple** to work, the backend must have at least one Apple client ID set so it can validate the `aud` claim in the Apple identity token.

| Variable | Description | Where to get it |
|----------|-------------|-----------------|
| `APPLE_CLIENT_ID_IOS` | iOS App ID / Bundle ID (e.g. `com.yourcompany.healthapp`) | [developer.apple.com](https://developer.apple.com) → Certificates, Identifiers & Profiles → Identifiers → your App ID |
| `APPLE_CLIENT_ID_ANDROID` | Services ID for Android (if using Sign in with Apple on Android) | Same → create a Services ID and use its identifier |
| `APPLE_CLIENT_ID_WEB` | Services ID for Web (if using Sign in with Apple on web) | Same as above |
| `APPLE_CLIENT_ID` | Optional fallback when platform-specific ID is not set | Same as above; can be your primary App ID or Services ID |
| `APPLE_ALLOW_EXPO_GO_AUDIENCE` | Dev-only toggle to accept Expo Go Apple token audience | Set to `true` only for local development; keep `false` in production |
| `APPLE_EXPO_GO_AUDIENCE` | Expected Expo Go audience value | Usually `host.exp.Exponent` |
| `APPLE_ALLOWED_AUDIENCES` | Optional extra audience allowlist | Comma-separated list, dev use only |

**Setup steps**

1. Copy `.env.example` to `.env` in the project root.
2. Set at least `APPLE_CLIENT_ID_IOS` to your iOS app’s Bundle ID (e.g. `com.yourcompany.healthapp`).
3. Before running the app, load the variables, for example:
   - **Unix/macOS:** `export $(grep -v '^#' .env | xargs)` then run the app, or add these to your IDE run configuration / shell profile.
   - **IDE:** In Run → Edit Configurations, add the variables to “Environment variables”.

If none of these are set, Apple token verification will fail (invalid or missing audience).

### Expo Go note (development)

When testing Apple Sign In in **Expo Go**, Apple token `aud` is typically `host.exp.Exponent`, not your iOS bundle ID.

- Recommended: test Apple Sign In with a custom dev client or production-like build so `aud` matches your app ID.
- If you must use Expo Go locally, set:
  - `APPLE_ALLOW_EXPO_GO_AUDIENCE=true`
  - optionally keep `APPLE_EXPO_GO_AUDIENCE=host.exp.Exponent`
- Keep `APPLE_ALLOW_EXPO_GO_AUDIENCE=false` in production.

---

## 5. Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/login`   | POST | Email/username + password; returns JWT + refresh + `expiresIn`. |
| `/api/auth/refresh` | POST | Body `{ "refreshToken" }`; returns new JWT + rotated refresh + profile. |
| `/api/auth/google`  | POST | Sign in with Google ID token. |
| `/api/auth/apple`   | POST | Sign in with Apple identity token; send email/name on first login when available. |

Success responses for **login, Google, Apple, and refresh** share the same JSON shape. Store `token`, `refreshToken`, and `expiresIn`; refresh before or after access expiry using `/api/auth/refresh`.

**Password change** (`POST /api/auth/change-password`) revokes all refresh tokens for that user; the client must log in again to obtain new tokens.
