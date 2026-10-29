## Why

The initial Android mobile scaffold provides offline expense storage, ML Kit QR scanning, and basic sync. However, to complete the development plan and allow comprehensive local testing against the Docker backend, the Android app needs real JWT authentication, configurable server host IP, 50/50 household balance display, and income tracking.

## What Changes

- **Session & Dynamic Server URL Management (`SessionManager.kt`)**:
  - Encrypted / persistent settings for JWT bearer token, user profile, and custom API base URL (toggling between Emulator `10.0.2.2:8080`, USB Cable `localhost:8080`, or Wi-Fi LAN `192.168.1.2:8080`).
- **Authentication Screens & Navigation**:
  - `LoginScreen.kt`: User login and registration with validation and Server IP configuration dialog.
  - Dynamic navigation flow: unauthenticated users start at `LoginScreen`; authenticated users start at `DashboardScreen`.
- **Household Balance 50/50 Card in Mobile**:
  - Displays roomie settlement status on the mobile dashboard.
- **Income & Cash Flow Support in Room**:
  - Adds `IncomeEntity.kt` and `IncomeDao.kt` to local Room database.
  - Synchronizes offline incomes during batch sync.

## Capabilities

### New Capabilities
<!-- None, builds upon existing android-client capability -->

### Modified Capabilities
- `android-client`: Enhances native Android client with JWT authentication, dynamic host IP configuration, roomie debt balance display, and local income tracking.

## Impact

- Updates `apps/mobile-android/app/src/main/java/com/houseexpenses/`.
- Connects seamlessly with Docker backend at `http://localhost:8080/api/v1` or `http://10.0.2.2:8080/api/v1`.
