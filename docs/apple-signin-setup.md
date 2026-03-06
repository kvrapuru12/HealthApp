# Apple Sign In – Backend configuration

The backend uses these properties to validate the **audience** (`aud`) claim in the Apple identity token. You need at least one client ID set (typically the iOS App ID).

## Properties (application.properties)

| Property | Env variable | Where to get the value |
|---------|----------------|------------------------|
| `apple.client.id` | `APPLE_CLIENT_ID` | Fallback if no platform-specific ID is set. Use your main App ID or Services ID. |
| `apple.client.id.ios` | `APPLE_CLIENT_ID_IOS` | **iOS App ID (Bundle ID)** from Apple Developer. Example: `com.yourcompany.healthapp`. |
| `apple.client.id.android` | `APPLE_CLIENT_ID_ANDROID` | **Services ID** (if you use Sign in with Apple on Android). |
| `apple.client.id.web` | `APPLE_CLIENT_ID_WEB` | **Services ID** (if you use Sign in with Apple on the web). |

## How to get the values

### 1. iOS App ID (required for iOS app)

1. Go to [developer.apple.com/account](https://developer.apple.com/account) → **Certificates, Identifiers & Profiles** → **Identifiers**.
2. Open your **App ID** (or create one) – the one used by your iOS app (Bundle ID).
3. Ensure **Sign in with Apple** is enabled under Capabilities.
4. The **Identifier** (e.g. `com.yourcompany.healthapp`) is your **iOS client ID**.

**Set it:**

- **Local / dev:** In `application.properties`:
  ```properties
  apple.client.id.ios=com.yourcompany.healthapp
  ```
  Or leave the property as-is and set the env var:
  ```bash
  export APPLE_CLIENT_ID_IOS=com.yourcompany.healthapp
  ```
- **Production:** Set `APPLE_CLIENT_ID_IOS` in your server / container environment (e.g. AWS, K8s, `.env`).

### 2. (Optional) Single fallback client ID

If you only have one Apple client (e.g. only iOS), you can set just:

```properties
apple.client.id=com.yourcompany.healthapp
```

or

```bash
export APPLE_CLIENT_ID=com.yourcompany.healthapp
```

The verifier will use this when no platform-specific ID is configured or when the request has no platform.

### 3. (Optional) Android / Web – Services ID

If you add Sign in with Apple for **Android** or **web**:

1. In **Identifiers**, click **+** and create a **Services ID**.
2. Use that Services ID as `apple.client.id.android` or `apple.client.id.web`, and set the corresponding env vars or properties.

## Quick checklist

- [ ] Apple Developer account with an App ID that has **Sign in with Apple** enabled.
- [ ] Copy the App ID (Bundle ID) value.
- [ ] Set `APPLE_CLIENT_ID_IOS` (or `apple.client.id.ios` in `application.properties`) to that value.
- [ ] Restart the backend so it picks up the new config.
- [ ] (Optional) Set `APPLE_CLIENT_ID` or `apple.client.id` as a fallback.

## Expo Go (development only)

When using Expo Go, Apple token audience is commonly `host.exp.Exponent` instead of your app bundle ID.

- Recommended: use a custom dev client / TestFlight build for Apple Sign In testing.
- If you need Expo Go for local testing, set:
  - `APPLE_ALLOW_EXPO_GO_AUDIENCE=true`
  - `APPLE_EXPO_GO_AUDIENCE=host.exp.Exponent` (default)
- Keep `APPLE_ALLOW_EXPO_GO_AUDIENCE=false` in production.

If no Apple client ID is set, the backend still verifies the token (signature, issuer, expiry) but does not check `aud`. Setting at least the iOS App ID is recommended for production.
