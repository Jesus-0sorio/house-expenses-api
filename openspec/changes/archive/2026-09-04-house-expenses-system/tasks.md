# Tasks: House Expenses & Personal Finance Implementation

## Phase 1: Go Backend Foundation & Hexagonal Setup
- [x] 1.1 Initialize Go module (`go.mod`) with standard hexagonal directory layout (`cmd/api`, `internal/core`, `internal/adapters`).
- [x] 1.2 Implement database connection layer supporting dual drivers (PostgreSQL and SQLite via GORM / database/sql).
- [x] 1.3 Setup Docker Compose dev environment (Backend API with Air, DIAN microservice, PostgreSQL) and local `.env` configuration for native execution.
- [x] 1.4 Implement database migrations and schema definitions for all core tables (`users`, `households`, `expenses`, `invoices`, `incomes`).

## Phase 2: Identity, Households & Adaptive UX Mode
- [x] 2.1 Implement User and UserOAuthAccount domain entities, password hashing (bcrypt), and JWT authentication middleware.
- [x] 2.2 Implement Household domain entity, invite codes, and membership management (default 50/50 split).
- [x] 2.3 Implement adaptive mode resolution (Solitary mode 100% personal vs. Household mode).
- [x] 2.4 Implement extensible `OAuthProvider` interface and Registry (Strategy pattern), implementing `GoogleOAuthProvider` as the first active adapter, and exposing `/api/v1/auth/oauth/:provider/url` and `/api/v1/auth/oauth/:provider/callback`.
- [x] 2.5 Expose REST endpoints for standard `/api/v1/auth/*` and `/api/v1/households/*`.

## Phase 3: DIAN Microservice & Electronic Invoicing Ingestion
- [x] 3.1 Initialize dedicated `dian-service` microservice Go project (`services/dian-service`) with HTTP server, `/health` endpoint, and Dockerfile.
- [x] 3.2 Implement DIAN Web Catalog client in `dian-service` to scrape/parse invoice details from QR validation URL or CUFE (`POST /api/v1/dian/lookup`).
- [x] 3.3 Implement official DIAN graphic representation (PDF) downloader and streamer in `dian-service` (`GET /api/v1/dian/pdf/:cufe`).
- [x] 3.4 Implement `DianClientPort` interface and outbound HTTP client adapter in main backend (`house-expenses-api`) with resilient timeouts.
- [x] 3.5 Implement UBL 2.1 XML parser and background IMAP email poller in main backend to extract `.zip` attachments in memory.
- [x] 3.6 Expose REST endpoints in main backend for manual `.zip` upload, QR lookup (delegating to `dian-service`), and PDF download (streaming from `dian-service`).

## Phase 4: Expense Management, Inverse Splitting & Rules Engine
- [x] 4.1 Implement Expense and InvoiceItem domain logic, including the inverse selection algorithm for supermarket receipts.
- [x] 4.2 Implement Classification Rules engine (default to Household vs. prompt, and permanent vs. one-off overrides).
- [x] 4.3 Implement Roomie debt balance calculator (50/50 settlement ledger).
- [x] 4.4 Expose REST endpoints for expense CRUD, item inverse splitting, and roomie balance summary.

## Phase 5: Bank Statement Reconciliation & Anti-duplication
- [x] 5.1 Implement statement parser for credit cards and bank accounts (PDF/Excel extraction).
- [x] 5.2 Implement heuristic anti-duplication engine (date tolerance +/- 4 days, amount equality, merchant fuzzy matching).
- [x] 5.3 Implement manual link/unlink and duplicate dismissal workflows.
- [x] 5.4 Expose REST endpoints for statement upload, duplicates review, and manual override.

## Phase 6: Personal Finance, Incomes & Cash Flow
- [x] 6.1 Implement Income entity (regular salaries, extraordinary primas, freelance, bonuses).
- [x] 6.2 Implement Recurring Fixed Debts (vehicle loans, credit card installments).
- [x] 6.3 Implement Net Disposable Cash Flow calculator ($Net = Income - (Personal + Debts + HouseholdShare)$).
- [x] 6.4 Expose REST endpoints for personal budget, income tracking, and monthly cash flow metrics.

## Phase 7: Mobile Batch Sync & API Hardening
- [x] 7.1 Implement `/api/v1/sync` batch endpoint for offline Android client transactions.
- [x] 7.2 Add unit and integration tests across core domain use cases.
- [x] 7.3 Add API documentation (OpenAPI / Swagger spec and Postman collection).
