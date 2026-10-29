## 1. Web Client Foundation & Infrastructure

- [x] 1.1 Scaffold React + Vite + TypeScript application in `apps/web` and configure Tailwind CSS and Lucide icons. Verify with `npm run build`.
- [x] 1.2 Create typed API client with Axios (`apps/web/src/api/client.ts`), request interceptor for JWT token injection, and TypeScript interfaces matching backend models.
- [x] 1.3 Setup React Router navigation and Auth Context provider (`apps/web/src/context/AuthContext.tsx`).

## 2. Web Authentication & Adaptive UX Mode

- [x] 2.1 Implement Login and Register screens with email/password and Google OAuth redirect trigger.
- [x] 2.2 Implement Adaptive Header and Navigation switching between Solitary personal mode and Household mode based on user's active household.
- [x] 2.3 Implement Household Management screen (create household, generate invite code, join with code, and view 50/50 balance settlement summary).

## 3. Web Expense Management & Inverse Receipt Splitting

- [x] 3.1 Implement Expense List and Creation modal with category selection and scope toggle (Personal vs Household).
- [x] 3.2 Implement Invoice Ingestion and Interactive Inverse Splitting table: user checks personal items, system auto-classifies remaining as household and calculates split totals.
- [x] 3.3 Implement DIAN QR code manual input and preview modal.

## 4. Web Bank Reconciliation & Cash Flow Analytics

- [x] 4.1 Implement Bank Statement Upload view supporting CSV and Excel (.xlsx) files with instant transaction preview.
- [x] 4.2 Implement Duplicate Transactions Review modal: highlights transactions with >=80% confidence score with One-Click Confirm Link or Dismiss actions.
- [x] 4.3 Implement Cash Flow Dashboard view with monthly summary metrics (Income, Expenses, Fixed Debts, Household Share, and Net Disposable Cash).

## 5. Android Native Foundation & Offline Room Storage

- [x] 5.1 Scaffold standard Android Kotlin Gradle project in `apps/mobile-android` with Jetpack Compose, Material 3, and Kotlin Coroutines.
- [x] 5.2 Implement Room SQLite database entities (`ExpenseEntity`, `IncomeEntity`, `SyncQueueEntity`) and DAOs for local-first CRUD.
- [x] 5.3 Implement Repository layer mediating between local Room database and remote API.

## 6. Android Networking & Background Sync Worker

- [x] 6.1 Implement Retrofit 2 API client with OkHttp logging and Bearer token auth interceptor targeting Go backend.
- [x] 6.2 Implement Jetpack WorkManager `SyncWorker` to automate two-way batch sync via `POST /api/v1/sync` when network connectivity is available.
- [x] 6.3 Implement offline sync queue logic ensuring local changes are pushed and marked as SYNCED without data loss.

## 7. Android Jetpack Compose Screens & Receipt Scanning

- [x] 7.1 Implement Compose UI screens: Dashboard (Adaptive Solitary/Household), Expense List, and Quick Add Expense screen.
- [x] 7.2 Implement CameraX / ML Kit Barcode Scanner screen to scan DIAN QR codes from physical supermarket receipts and pre-fill expense details.
- [x] 7.3 Verify build scripts, Gradle dependencies, and documentation.
