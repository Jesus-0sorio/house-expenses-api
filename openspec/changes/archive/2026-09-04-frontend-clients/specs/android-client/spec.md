## Purpose

Provides a native Android mobile application with offline-first local persistence, camera receipt scanning, and automated background sync with the central Go API.

## ADDED Requirements

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
