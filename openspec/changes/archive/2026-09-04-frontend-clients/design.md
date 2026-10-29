## Context

The Go hexagonal backend and DIAN microservice expose complete REST APIs for authentication, adaptive households, expense tracking, invoice ingestion, bank reconciliation, cash flow, and mobile batch sync (`/api/v1/sync`). See `proposal.md` and capability specs in `specs/`.

## Goals / Non-Goals

**Goals:**
- Deliver a modern, responsive Web application in `apps/web` using React, TypeScript, and Tailwind CSS (Vite).
- Deliver a robust native Android application in `apps/mobile-android` using Kotlin, Jetpack Compose, and Room SQLite for offline-first resilience.
- Provide seamless authentication, adaptive mode switching (solitary vs household), receipt splitting, bank statement reconciliation, and cash flow visualization.
- Enable offline mobile expense logging with automated background synchronization when network is available.

**Non-Goals:**
- Modifying or breaking existing backend Go REST API contracts.
- Building iOS native app (Android was explicitly selected).
- Native desktop applications (covered responsively via web).

## Decisions

### 1. Web Application: React + Vite + TypeScript + Tailwind CSS
- **Decision:** Build a single-page application (SPA) in `apps/web` with Vite, TypeScript, and Tailwind CSS.
- **Rationale:** Lightning-fast builds, zero-friction local development, type safety matching backend DTOs, and utility-first styling for quick responsive layouts.
- **Alternatives considered:** Next.js (adds Node SSR server overhead which is unnecessary for a client consuming a Go REST API).

### 2. Web State Management & API Communication: TanStack Query + Axios
- **Decision:** Use TanStack Query (React Query) for server state caching and optimistic UI updates, paired with Axios interceptors for JWT token injection and 401 refresh handling.
- **Rationale:** Handles loading states, query invalidation (e.g., refreshing household balance immediately after creating an expense), and automatic retries.

### 3. Native Android: Kotlin + Jetpack Compose + Clean MVVM
- **Decision:** Structure `apps/mobile-android` using single-activity Jetpack Compose navigation, StateFlow, and ViewModels.
- **Rationale:** Google's modern standard for declarative Android UI, eliminating XML boilerplate and simplifying reactive UI updates.

### 4. Android Offline-First: Room SQLite + WorkManager
- **Decision:** All write actions (creating expenses/incomes) save to local Room SQLite with a `PENDING` sync status flag. A `SyncWorker` powered by Jetpack WorkManager triggers when network connectivity is detected to execute `POST /api/v1/sync`.
- **Rationale:** The user can log expenses on the spot inside a store with zero cell reception.

### 5. Receipt Scanning: CameraX + Google ML Kit Barcode Scanning
- **Decision:** Integrate ML Kit Barcode Scanning on Android to parse DIAN QR codes directly on-device.
- **Rationale:** Real-time camera viewfinder detection without uploading images to cloud APIs.

## Risks / Trade-offs

- [Cross-Origin Resource Sharing (CORS) during Web dev] → Backend Gin router already configures CORS with credentials, headers, and wildcard origins for local dev ports (5173, 3000).
- [Data conflict during offline sync] → Backend uses UUID primary keys generated on the client, ensuring idempotency and conflict-free UPSERT operations during batch sync.
- [Mobile build environment without local Android SDK installed in shell] → We structure the complete standard Gradle project with `gradlew` wrapper, source code, Composables, ViewModels, and Room entities so it can be opened directly in Android Studio or built via CI.
