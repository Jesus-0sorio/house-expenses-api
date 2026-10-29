# Delta Spec: Rediseño de UX/UI y Experiencia Móvil

## ADDED Requirements

### Requirement: Online-First Outbox Pattern
The Android app SHALL prioritize direct backend calls and persist records into Room SQLite ONLY when a network or server failure occurs (acting as an outbox buffer), without displaying offline-mode jargon or sync badge counts to the user.

### Requirement: Bottom Navigation Architecture
The Android app SHALL provide a 4-tab bottom navigation bar (Inicio, Gastos, Flujo, Hogar) with a central floating action button for QR scanning and quick expense logging.

### Requirement: Formatted Colombian Currency Input
The Android app SHALL format expense amounts dynamically with Colombian peso thousand separators ($ 1.000.000) during typing via a custom Compose visual transformation.

### Requirement: Categorized Visual Chips
The Android app SHALL provide an interactive chip selector with thematic icons for expense categories instead of a plain dropdown.

### Requirement: Cash Flow and Daily Expense Tracking
The Android app SHALL allow configuring recurring (bi-weekly, monthly) and extraordinary (primas, bonuses, freelance) incomes, displaying current-cycle available balance and actual daily expenditures.

### Requirement: Household Debt Settlement Action
The Android app SHALL provide a dedicated action to record settlement payments (Nequi, Daviplata, Bank Transfer, Cash) between household roommates.
