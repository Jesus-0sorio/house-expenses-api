# android-client Specification

## Purpose
TBD - created by archiving change frontend-clients. Update Purpose after archive.

## Requirements

### Requirement: Offline-First Local Data Storage
The Android application SHALL persist expenses, incomes, and categories locally in a Room SQLite database, allowing full read and write functionality when the device has no internet connectivity.

#### Scenario: Offline expense creation
- **WHEN** the user creates an expense without active internet
- **THEN** the application saves the transaction locally with a PENDING_SYNC status and immediately updates local balances

### Requirement: Two-Way Batch Synchronization
The Android application SHALL automatically synchronize pending local transactions with the Go backend via `/api/v1/sync` when network connectivity is restored.

#### Scenario: Network restoration triggers sync
- **WHEN** network connectivity becomes available
- **THEN** the background sync worker pushes local changes, fetches remote updates, and marks local records as SYNCED

### Requirement: Receipt QR Scanning
The Android application SHALL utilize the device camera to scan Colombian electronic invoice QR codes and extract financial data.

#### Scenario: Scan DIAN receipt QR
- **WHEN** user scans a DIAN invoice QR code
- **THEN** the application parses the standard DIAN payload (total, date, merchant, CUFE) and pre-fills the expense form for review

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
