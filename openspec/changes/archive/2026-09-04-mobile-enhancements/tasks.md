## 1. Session, Auth & Dynamic Server Config

- [x] 1.1 Implement `SessionManager.kt` with SharedPreferences for JWT token, user credentials, and configurable server base URL.
- [x] 1.2 Implement `AuthInterceptor.kt` in OkHttp to inject Bearer token and route requests to the configured server base URL.
- [x] 1.3 Add auth DTOs (`LoginRequestDto`, `AuthResponseDto`, `RegisterRequestDto`) and endpoints to `HouseExpensesApi.kt`.

## 2. Income Local Persistence & Batch Sync

- [x] 2.1 Implement `IncomeEntity.kt` and `IncomeDao.kt` in Room local database.
- [x] 2.2 Update `AppDatabase.kt` to include `IncomeEntity` and bump database version.
- [x] 2.3 Update `ExpenseRepository.kt` and `SyncWorker.kt` to synchronize both expenses and incomes in `POST /api/v1/sync`.

## 3. UI Screens: Login, Server Settings & Household Balance

- [x] 3.1 Implement `LoginScreen.kt` with login/register tabs and a Server Connection Dialog (Emulator `10.0.2.2`, USB `localhost`, Wi-Fi LAN).
- [x] 3.2 Update `DashboardScreen.kt` to show the 50/50 household balance settlement card and user profile header.
- [x] 3.3 Update `MainActivity.kt` navigation: show `LoginScreen` if unauthenticated, otherwise navigate directly to `DashboardScreen`.
