## Context

See `proposal.md` and capability spec in `specs/android-client/spec.md`. The Go backend runs in Docker on port 8080 and requires JWT authentication for protected endpoints. Android developers frequently test either on an emulator (`10.0.2.2:8080`), on a physical device over USB with `adb reverse`, or over local Wi-Fi LAN (`192.168.1.x:8080`).

## Goals / Non-Goals

**Goals:**
- Provide secure session storage and JWT injection in Retrofit via OkHttp interceptor.
- Support runtime configuration of API base URL without recompiling.
- Display 50/50 roomie settlement balance on the dashboard.
- Add local Room support for incomes.

**Non-Goals:**
- Rewriting the backend API.
- Third-party social login on Android (standard email/password auth first).

## Decisions

### 1. Session & Server Config via SharedPreferences (`SessionManager.kt`)
- Store `auth_token`, `user_id`, `user_name`, and `server_url`.
- Expose a dynamic OkHttp Interceptor that rewrites the request URL and adds the Bearer header.

### 2. Retrofit Dynamic Base URL
- Use an OkHttp Interceptor or a dynamic URL builder so changes in settings take effect immediately.

### 3. Room Database v2: Add `IncomeEntity` and `IncomeDao`
- Update `AppDatabase` schema version to 2 with fallbackToDestructiveMigration for local dev.
