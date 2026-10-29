## ADDED Requirements

### Requirement: Mobile User Authentication and Token Management
The Android application SHALL allow users to log in with their email and password, securely persist the returned JWT authentication token, and inject the Bearer token in all network requests.

#### Scenario: User logs in from mobile
- **WHEN** user provides valid credentials on the login screen
- **THEN** application stores the token and navigates directly to the dashboard

### Requirement: Dynamic Server Host Configuration
The Android application SHALL allow configuring the backend API host URL to support Android Emulator (10.0.2.2), USB debugging (localhost with adb reverse), and Wi-Fi LAN (local machine IP).

#### Scenario: User configures server IP
- **WHEN** user changes the server IP in the connection settings dialog
- **THEN** the Retrofit client immediately updates its base URL for all subsequent network calls

### Requirement: Offline Income Persistence
The Android application SHALL persist user incomes locally in Room SQLite and sync them with the central backend during batch sync.

#### Scenario: Record offline income
- **WHEN** user records a salary or legal prima without network connectivity
- **THEN** the income is saved locally with PENDING_SYNC status and queued for synchronization
