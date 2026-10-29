## Why

The backend REST API and DIAN microservice are fully operational, tested, and ready. Now, users need dedicated user interfaces: a modern, responsive Web application for comprehensive personal/household finance management, receipt splitting, and bank statement reconciliations, plus a native Android mobile application optimized for quick expense logging, offline-first operation, and mobile QR scanning.

## What Changes

- **Web Frontend (`apps/web`)**:
  - React 18 / 19 + TypeScript + Tailwind CSS powered by Vite.
  - Authentication (JWT login/register & Google OAuth redirect).
  - Adaptive Mode UX: Clean personal mode for solitary users; roomie ledger, invite codes, and 50/50 balance settlement for households.
  - Expense Management: Creation, category filtering, and receipt item inverse-splitting table.
  - Bank Reconciliation View: Upload CSV/Excel statements, interactive review of suspected duplicates (>=80% confidence), link and dismiss actions.
  - Cash Flow & Incomes Dashboard: Visual charts for disposable cash flow ($Net = Income - (Personal + Debts + HouseholdShare)$).
- **Native Android Client (`apps/mobile-android`)**:
  - Native Kotlin application with Jetpack Compose UI and Material 3 design.
  - Offline-First Architecture: Local SQLite persistence with Room, queuing created expenses and incomes when offline.
  - Background Sync Worker: Automates two-way idempotent sync via `POST /api/v1/sync`.
  - Camera QR Scanner: Instant scanning of Colombian DIAN receipts to extract financial data on-device or via API.

## Capabilities

### New Capabilities
- `web-client`: Responsive web application for end-to-end expense tracking, roomie balances, bank reconciliation, and financial analytics.
- `android-client`: Native Android mobile application featuring offline-first data caching with Room, background sync with the Go backend, and camera receipt scanning.

### Modified Capabilities
<!-- None, backend API contract remains stable and unchanged -->

## Impact

- Creates two new client directories: `apps/web` (React/Vite) and `apps/mobile-android` (Android Studio / Gradle project).
- Leverages existing Go backend at `http://localhost:8080/api/v1`.
- Enables CORS for web frontend (already supported in backend Gin router).
