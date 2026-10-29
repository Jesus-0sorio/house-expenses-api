# Capability: Expenses & Financial Management

## ADDED Requirements

### Requirement: User & Household Management
The system MUST provide secure user authentication, isolated personal workspaces, and multi-tenant household spaces for shared expense tracking. Authentication MUST support standard credentials as well as an extensible OAuth2 provider architecture (Strategy pattern) allowing straightforward addition of future identity providers (e.g. Microsoft/Hotmail, GitHub), starting with Google/Gmail as the first supported provider.

#### Scenario: User registration and authentication via extensible OAuth (Gmail / Google)
- **GIVEN** an unauthenticated user with a valid account on a supported OAuth provider (currently `google`)
- **WHEN** the user initiates sign-up or login via the provider's OAuth2 flow
- **THEN** the system delegates verification to the corresponding provider strategy, extracts the verified email, provider unique ID, display name, and avatar, provisions a new user account if not already registered (without requiring a password), and returns a valid JWT authentication token.

#### Scenario: Attempting authentication with an unregistered or planned OAuth provider
- **GIVEN** an OAuth login request specifying an unsupported or future provider (e.g. `microsoft` or `github`)
- **WHEN** the request is received before that provider strategy is registered
- **THEN** the system returns `HTTP 400 Bad Request` indicating the provider is not yet configured, without affecting existing provider integrations.

#### Scenario: Operating in solitary mode
- **GIVEN** an authenticated user who is not a member of any active household
- **WHEN** an expense or electronic invoice is ingested or registered
- **THEN** the system MUST classify the expense as 100% Personal without presenting household split dialogues or prompts.

#### Scenario: Operating in shared household mode
- **GIVEN** two authenticated users who are members of the same household with a 50/50 split configuration
- **WHEN** a shared expense is created or approved
- **THEN** the system records the full expense under the household, attributes payment to the paying user, and credits 50% of the value as debt owed by the co-resident.

---

### Requirement: DIAN Electronic Invoicing & Ingestion
The system MUST support ingestion of Colombian DIAN electronic invoices via email (IMAP) and physical QR codes, delegating DIAN catalog querying, invoice data scraping, and official PDF downloading to a dedicated, autonomous DIAN microservice (`dian-service`).

#### Scenario: Ingesting UBL 2.1 zip attachment from email
- **GIVEN** an unread email in a configured IMAP inbox containing a `.zip` attachment with DIAN XML (UBL 2.1 AttachedDocument)
- **WHEN** the background email poller executes
- **THEN** the system extracts and parses the XML in memory, captures the Issuer Name, NIT, CUFE, Issue Date, Line Items, Taxes, and `PayableAmount`, stores the invoice record, and associates the accompanying PDF for visual inspection.

#### Scenario: Resolving physical receipt via DIAN microservice from QR code
- **GIVEN** a camera scan of a physical receipt QR containing a DIAN validation URL or document key
- **WHEN** the client sends the URL/key to the backend
- **THEN** the backend invokes the `dian-service` microservice endpoint (`POST /api/v1/dian/lookup`), which queries the official DIAN catalog, extracts structured invoice details (vendor, NIT, CUFE, line items, taxes, total) without requiring receipt image storage, and stores the invoice record in the database.

#### Scenario: Downloading official graphic representation (PDF) via DIAN microservice
- **GIVEN** an existing electronic invoice with a registered CUFE
- **WHEN** the user requests the invoice PDF via `/api/v1/invoices/:id/pdf`
- **THEN** the backend delegates the request to the `dian-service` microservice (`GET /api/v1/dian/pdf/:cufe`), which fetches or streams the official PDF from the DIAN repository back to the client.

#### Scenario: DIAN service timeout or unavailability handling
- **GIVEN** a QR lookup or PDF download request when the DIAN portal or the `dian-service` is unreachable or exceeds a 15-second timeout
- **WHEN** the backend attempts to query the microservice
- **THEN** the system responds with a clear `HTTP 504 Gateway Timeout` or `HTTP 502 Bad Gateway` error, prompting the user to retry or enter the expense manually, without crashing the core backend.

#### Scenario: Manual expense entry fallback
- **GIVEN** an expense paid in cash or without formal electronic invoicing
- **WHEN** the user submits description, amount, date, and category via web or mobile form
- **THEN** the system creates a standard expense record marked as `MANUAL`.

---

### Requirement: Supermarket Inverse Item Splitting
The system MUST provide an inverse selection mechanism to rapidly divide mixed receipts containing personal and household items.

#### Scenario: Inverse item classification
- **GIVEN** an imported supermarket invoice containing 10 line items
- **WHEN** the user activates item splitting and checks 2 items as `Personal`
- **THEN** the system automatically classifies the remaining 8 items as `Household`, calculating the exact sum for each category.

#### Scenario: Configurable default for mixed merchants
- **GIVEN** the user preference for a merchant (e.g., supermarket) is configured as "Always Household"
- **WHEN** a new invoice from that merchant is processed
- **THEN** the system assigns the entire invoice to the household automatically without blocking confirmation dialogues, while keeping an option to deselect or split items later.

#### Scenario: Bidirectional expense reclassification
- **GIVEN** an existing expense classified as Household
- **WHEN** the user reclassifies it to Personal
- **THEN** the system offers two options: "Only this time" (modifies only current transaction) or "Always" (updates the merchant classification rule for future imports).

---

### Requirement: Bank Statement Reconciliation & Anti-duplication
The system MUST detect potential duplicate transactions between bank statements and electronic invoices.

#### Scenario: Assisted heuristic duplicate detection
- **GIVEN** an invoice of $120,000 COP registered from a utility company on 2026-09-01
- **WHEN** a bank or credit card statement is parsed containing a transaction of $120,000 COP dated within +/- 4 days of the invoice
- **THEN** the system flags the statement line as a `POTENTIAL_DUPLICATE` with a confidence score and prompts the user to confirm pairing.

#### Scenario: Manual duplicate override and linking
- **GIVEN** an unlinked statement item or a misclassified duplicate
- **WHEN** the user manually links it to an existing invoice or marks it as "Dismiss / Not a duplicate"
- **THEN** the system updates the relation, prevents double counting in the monthly balance, and learns the alias mapping.

---

### Requirement: Personal Cash Flow & Fixed Obligations
The system MUST provide personal budget tracking including recurring fixed payments, income, and disposable cash flow.

#### Scenario: Calculating net disposable income
- **GIVEN** user regular income (e.g. bi-weekly salary), extraordinary income (e.g. Colombian prima in June/December, bonuses), fixed liabilities (car loans, credit cards), and the user's share of household expenses
- **WHEN** the cash flow summary for a given month is calculated
- **THEN** the system computes:
  $$\text{Net Disposable} = \text{Total Income} - (\text{Personal Expenses} + \text{Fixed Liabilities} + \text{Household Debt Share})$$

---

### Requirement: Android Offline-First Synchronization
The mobile application MUST operate seamlessly without network connectivity and synchronize reactively.

#### Scenario: Registering transactions offline
- **GIVEN** the mobile device has no active internet connection
- **WHEN** the user creates an expense or scans a receipt offline
- **THEN** the Android client saves the record in the local SQLite/Room database with `SYNC_STATUS = PENDING` and displays it immediately in the UI.

#### Scenario: Background reactive sync
- **GIVEN** pending offline records in the local database
- **WHEN** Android network monitor detects an active internet connection
- **THEN** `WorkManager` triggers a batch sync job against `/api/v1/sync`, sends pending records, and updates local records to `SYNC_STATUS = SYNCED` upon receiving `HTTP 201`.
