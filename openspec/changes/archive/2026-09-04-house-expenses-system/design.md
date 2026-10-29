# Design: House Expenses & Personal Finance Architecture

## Context
This design establishes the technical blueprint for the **House Expenses & Personal Finance** ecosystem, focusing on a clean, scalable Go backend following **Hexagonal Architecture** (Ports and Adapters), paired with an Android Kotlin client (Offline-First) and a responsive Web frontend.

---

### 1. System Architecture: Distributed Hexagonal Backend & DIAN Microservice

```
+-----------------------------------------------------------------------------------------+
|                                  HOUSE-EXPENSES-API                                     |
|                                                                                         |
|  +-----------------------------------------------------------------------------------+  |
|  |                                INBOUND ADAPTERS                                   |  |
|  |  - REST API Handlers (Gin / standard net/http)                                    |  |
|  |  - Background IMAP Poller (Goroutine worker)                                      |  |
|  +-----------------------------------------+-----------------------------------------+  |
|                                            |                                            |
|                                            v                                            |
|  +-----------------------------------------------------------------------------------+  |
|  |                               APPLICATION / PORTS                                 |  |
|  |  Inbound Ports (Use Cases):                                                       |  |
|  |    * IngestEmailInvoiceUseCase                                                    |  |
|  |    * LookupDianQrUseCase                                                          |  |
|  |    * SplitInvoiceItemsUseCase (Inverse logic)                                     |  |
|  |    * ReconcileBankStatementUseCase                                                |  |
|  |    * CalculateHouseholdBalancesUseCase                                            |  |
|  |    * BatchSyncTransactionsUseCase                                                 |  |
|  |  Outbound Ports (Repository & Client Interfaces):                                 |  |
|  |    * UserRepository, HouseholdRepository, ExpenseRepository                       |  |
|  |    * InvoiceRepository, StatementRepository                                       |  |
|  |    * DianClientPort (HTTP Client contract), EmailReaderPort                       |  |
|  +--------------------+------------------------------------+-------------------------+  |
|                       |                                    |                            |
|                       v                                    v                            |
|  +-----------------------------------------+  +--------------------------------------+  |
|  |              DOMAIN (CORE)              |  |          OUTBOUND ADAPTERS           |  |
|  |  Entities: User, Household, Expense,    |  |  - Relational: PostgreSQL / SQLite   |  |
|  |            Invoice, Item, Income        |  |  - UBL 2.1 Parser: encoding/xml      |  |
|  |  Value Objects: MoneyCOP, CUFE          |  |  - Bank Statement Extractor          |  |
|  |  Domain Events: ExpenseCreated, ...     |  |  - DianHttpClientAdapter ------------+--+--+
|  +-----------------------------------------+  +--------------------------------------+  |  |
+-----------------------------------------------------------------------------------------+  |
                                                                                             |
                                                    HTTP REST / JSON (Port 8081)             |
                                                                                             v
+-----------------------------------------------------------------------------------------+
|                            DIAN MICROSERVICE (dian-service)                             |
|                                                                                         |
|  - Inbound Handler:                                                                     |
|      * POST /api/v1/dian/lookup (Resolves QR URL / CUFE)                                |
|      * GET  /api/v1/dian/pdf/:cufe (Downloads & streams official DIAN PDF)              |
|  - DIAN Web Catalog Scraper & Resolver:                                                 |
|      * Queries https://catalogo-vpfe.dian.gov.co                                        |
|      * Extracts Issuer, NIT, Issue Date, Item Lines, Taxes (IVA/IC), Total Payable       |
|  - PDF Downloader & Cache:                                                              |
|      * Fetches official graphic representation from DIAN binary endpoints               |
+-----------------------------------------------------------------------------------------+
```

### Directory Layout
```
house-expenses/
├── cmd/
│   └── api/                        # Backend principal: Entrypoint & bootstrap
│       └── main.go
├── internal/
│   ├── core/
│   │   ├── domain/                 # Pure entities & value objects
│   │   │   ├── user.go
│   │   │   ├── household.go
│   │   │   ├── expense.go
│   │   │   ├── invoice.go
│   │   │   └── income.go
│   │   ├── ports/                  # Inbound and outbound interfaces
│   │   │   ├── repositories.go
│   │   │   ├── oauth_provider.go   # Pluggable OAuthProvider interface & Registry
│   │   │   ├── dian_client.go      # DianClientPort interface (LookupInvoice, DownloadPDF)
│   │   │   └── services.go
│   │   └── usecase/                # Business logic & application workflows
│   │       ├── auth_usecase.go
│   │       ├── expense_usecase.go
│   │       ├── invoice_usecase.go
│   │       ├── dian_usecase.go     # Calls DianClientPort, stores invoice & items
│   │       ├── reconciliation_usecase.go
│   │       └── cashflow_usecase.go
│   └── adapters/
│       ├── handlers/               # REST API HTTP handlers
│       │   ├── auth_handler.go
│       │   ├── expense_handler.go
│       │   ├── invoice_handler.go
│       │   └── sync_handler.go
│       ├── oauth/                  # Pluggable OAuth Providers (Strategy Pattern)
│       ├── repository/             # Database persistence (GORM / sqlx)
│       │   ├── postgres/
│       │   └── sqlite/
│       ├── email/                  # IMAP poller & email attachments downloader
│       ├── dian_client/            # Outbound HTTP Client adapter targeting dian-service
│       │   └── http_client.go
│       └── extract/                # PDF & Excel bank statement extractor
├── services/
│   └── dian-service/               # MICROSERVICIO AUTÓNOMO DIAN
│       ├── cmd/
│       │   └── main.go             # Entrypoint microservicio (Puerto 8081)
│       ├── internal/
│       │   ├── handler/            # Handlers HTTP del microservicio
│       │   │   ├── lookup_handler.go
│       │   │   └── pdf_handler.go
│       │   ├── service/            # Scraper/Catalog resolver DIAN & PDF downloader
│       │   │   ├── catalog_client.go
│       │   │   └── pdf_client.go
│       │   └── model/              # DTOs de respuesta (DianInvoiceDTO, DianItemDTO)
│       ├── Dockerfile
│       └── go.mod
├── docker-compose.yml
├── Dockerfile
├── Makefile
├── .env.example
└── README.md
```

---

## 2. Data Model (Relational Schema)

### Entities & Relationships
1. **`users`**: `id (UUID)`, `email`, `password_hash (nullable)`, `name`, `avatar_url (nullable)`, `primary_auth_provider (VARCHAR(50), default 'LOCAL')`, `active_household_id (nullable)`, `created_at`.
2. **`user_oauth_accounts`**: `id (UUID)`, `user_id (UUID, FK users)`, `provider (VARCHAR(50), e.g. 'GOOGLE', 'MICROSOFT', 'GITHUB')`, `provider_user_id (VARCHAR(255))`, `email (VARCHAR(255))`, `created_at` *(Constraint: UNIQUE(`provider`, `provider_user_id`))*.
3. **`households`**: `id (UUID)`, `name`, `invite_code`, `created_at`.
4. **`household_members`**: `id`, `household_id`, `user_id`, `split_percentage (default 50.00)`, `joined_at`.
4. **`invoices`**: `id (UUID)`, `cufe (nullable, unique)`, `issuer_name`, `issuer_nit`, `invoice_number`, `issue_date`, `total_amount`, `tax_amount`, `pdf_storage_path`, `source_type (EMAIL_ZIP, QR_DIAN, MANUAL)`, `created_at`.
5. **`invoice_items`**: `id (UUID)`, `invoice_id`, `description`, `quantity`, `unit_price`, `total_price`, `classification (PERSONAL, HOUSEHOLD)`.
6. **`expenses`**: `id (UUID)`, `user_id`, `household_id (nullable)`, `invoice_id (nullable)`, `amount`, `date`, `category`, `description`, `scope (PERSONAL, HOUSEHOLD)`, `payment_status`, `created_at`.
7. **`bank_statements`**: `id (UUID)`, `user_id`, `bank_name`, `account_last_four`, `period_start`, `period_end`, `created_at`.
8. **`bank_transactions`**: `id (UUID)`, `statement_id`, `transaction_date`, `description`, `amount`, `matched_expense_id (nullable)`, `duplicate_status (NONE, SUSPECTED, CONFIRMED, DISMISSED)`.
9. **`incomes`**: `id (UUID)`, `user_id`, `amount`, `type (SALARY_REGULAR, PRIMA, BONUS, FREELANCE)`, `date`, `description`.
10. **`recurring_debts`**: `id (UUID)`, `user_id`, `name`, `monthly_amount`, `due_day`, `remaining_installments`, `scope (PERSONAL, HOUSEHOLD)`.
11. **`classification_rules`**: `id (UUID)`, `user_id`, `household_id`, `merchant_pattern`, `default_scope (PERSONAL, HOUSEHOLD)`, `auto_split_policy (ALWAYS_HOUSEHOLD, PROMPT)`.

---

## 3. Core Technical Workflows

### A. DIAN Invoice Parsing (UBL 2.1)
* Colombian electronic invoices package an `AttachedDocument` XML inside a `.zip`.
* The parser extracts:
  * XPath `/AttachedDocument/cac:Attachment/cac:ExternalReference/cbc:Description`: XML payload.
  * In root `Invoice`: `/Invoice/cbc:UUID` (CUFE), `/Invoice/cac:AccountingSupplierParty` (Vendor NIT & Name), `/Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount` (Total COP).
  * Lines under `/Invoice/cac:InvoiceLine`: item descriptions, unit prices, and quantities.

### B. Inverse Item Splitting Logic
```go
func SplitItemsInversely(items []InvoiceItem, selectedItemIDs []uuid.UUID, selectedAs Classification) SplitResult {
    var personalTotal, householdTotal float64
    opposite := HOUSEHOLD
    if selectedAs == HOUSEHOLD {
        opposite = PERSONAL
    }

    selectedSet := makeSet(selectedItemIDs)
    for i := range items {
        if selectedSet.Contains(items[i].ID) {
            items[i].Classification = selectedAs
        } else {
            items[i].Classification = opposite
        }
        if items[i].Classification == PERSONAL {
            personalTotal += items[i].TotalPrice
        } else {
            householdTotal += items[i].TotalPrice
        }
    }
    return SplitResult{Items: items, PersonalTotal: personalTotal, HouseholdTotal: householdTotal}
}
```

### C. Bank Reconciliation Duplicate Detection Engine
* Input: Bank transaction $T_b(\text{date}, \text{amount}, \text{description})$ and registered expenses $E_r$.
* Heuristic matching criteria:
  1. Amount match: $|T_b.\text{amount} - E_r.\text{amount}| < 1.00\text{ COP}$.
  2. Date window: $|T_b.\text{date} - E_r.\text{date}| \le 4\text{ days}$.
  3. Description token similarity score using Levenshtein / Jaro-Winkler distance on merchant names.
* Confidence score $\ge 80\% \rightarrow$ Flagged as `SUSPECTED_DUPLICATE` for user confirmation.

---

## 4. REST API Specification

### A. Core Gateway Endpoints (`house-expenses-api` - Port 8080)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Standard user signup & JWT token generation |
| `POST` | `/api/v1/auth/login` | Standard user authentication |
| `GET` | `/api/v1/auth/oauth/:provider/url` | Get OAuth consent URL (active: `google`; extensible: `microsoft`, `github`) |
| `POST` | `/api/v1/auth/oauth/:provider/callback` | Exchange OAuth code/token for provider, provision/login user & return JWT |
| `POST` | `/api/v1/households` | Create a new household space |
| `POST` | `/api/v1/households/join` | Join household via invitation code |
| `GET` | `/api/v1/households/balance` | Get net debt balance between roomies |
| `POST` | `/api/v1/invoices/upload-zip` | Upload and process DIAN `.zip` manually |
| `POST` | `/api/v1/invoices/dian-qr` | Lookup receipt details (delegates to `dian-service`) and save invoice |
| `GET` | `/api/v1/invoices/:id/pdf` | Stream or download invoice graphic representation (delegates to `dian-service`) |
| `POST` | `/api/v1/invoices/:id/split-inverse` | Apply inverse item splitting on receipt lines |
| `POST` | `/api/v1/reconciliation/upload-statement` | Upload PDF/Excel credit card or bank statement |
| `GET` | `/api/v1/reconciliation/duplicates` | Retrieve suspected duplicates for review |
| `POST` | `/api/v1/reconciliation/link` | Manually link statement charge to an invoice |
| `POST` | `/api/v1/incomes` | Register salary or extraordinary income |
| `GET` | `/api/v1/finance/cash-flow` | Get monthly disposable income report |
| `POST` | `/api/v1/sync` | Batch synchronization endpoint for offline mobile clients |

### B. Internal Microservice Endpoints (`dian-service` - Port 8081)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Healthcheck and readiness probe |
| `POST` | `/api/v1/dian/lookup` | Consults DIAN web catalog by QR URL or CUFE, returns structured JSON |
| `GET` | `/api/v1/dian/pdf/:cufe` | Streams the official graphic representation (PDF) directly from DIAN storage |

---

## 5. Development & Deployment Environments (DevEx)

### A. Docker Compose (Zero Config Setup)
```yaml
version: '3.8'
services:
  api:
    build:
      context: .
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
    volumes:
      - .:/app
    environment:
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_USER=expenses
      - DB_PASSWORD=expenses_secret
      - DB_NAME=house_expenses
      - JWT_SECRET=super_secret_dev_key
      - DIAN_SERVICE_URL=http://dian-service:8081
    depends_on:
      - postgres
      - dian-service

  dian-service:
    build:
      context: ./services/dian-service
      dockerfile: Dockerfile.dev
    ports:
      - "8081:8081"
    volumes:
      - ./services/dian-service:/app
    environment:
      - PORT=8081
      - DIAN_CATALOG_BASE_URL=https://catalogo-vpfe.dian.gov.co

  postgres:
    image: postgres:16-alpine
    restart: always
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=expenses
      - POSTGRES_PASSWORD=expenses_secret
      - POSTGRES_DB=house_expenses
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### B. Native Local Execution (Lightweight Mode)
* Configurable via `.env` in root:
  ```env
  DB_DRIVER=sqlite
  DB_DSN=file:local_expenses.db?cache=shared&mode=rwc
  PORT=8080
  DIAN_SERVICE_URL=http://localhost:8081
  ```
* Run `dian-service`:
  ```bash
  cd services/dian-service && go run cmd/main.go
  ```
* Run `house-expenses-api`:
  ```bash
  go run cmd/api/main.go
  ```
  *(Zero background daemons required; SQLite + DIAN microservice).*
