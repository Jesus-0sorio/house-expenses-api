# Delta Spec: Android Client (Aislamiento de Gastos Propios y Gestión de Suscripciones)

## Requirements

### Requirement: User Isolation in Personal Views & Clean Transaction Cards
The Android application SHALL strictly display transactions belonging to the currently logged-in user in both the Dashboard *"Transacciones del Mes"* and the *"Historial de Gastos"* screens. The application SHALL NOT display roomie expenses in these screens, and SHALL NOT display redundant authorship badges (`"Por: Tú"` / `"Por: [Nombre]"`) where all items are intrinsically the user's own.

#### Scenario: Clean transaction cards in Dashboard "Transacciones del Mes"
- **GIVEN** an authenticated user viewing the *"Transacciones del Mes"* section on `DashboardScreen`
- **WHEN** the list renders personal and household expenses paid by the user
- **THEN** each card displays the description, the scope badge (`"Hogar"` or `"Personal"`), the category, clean date (`YYYY-MM-DD`), and amount, without rendering any `"Por: Tú"` or authorship chip.

#### Scenario: Strictly own expenses in ExpensesListScreen
- **GIVEN** an active household where both the user and their roomies have recorded household expenses
- **WHEN** the user navigates to the *"Historial de Gastos"* (`ExpensesListScreen`)
- **THEN** the screen filters the local SQLite records to show strictly `exp.userId == currentUserId`, excluding all roomie expenses from the list, from pagination, from hierarchical month/day grouping, and from total amount summaries.

#### Scenario: HouseholdScreen remains the sole multi-member expense view
- **GIVEN** an active household with shared expenses registered by multiple co-residents
- **WHEN** the user navigates to the *"Hogar"* (`HouseholdScreen`)
- **THEN** all household expenses from all members remain visible, maintaining the read-only restriction for expenses not belonging to the current user.

### Requirement: Interactive Recurring Debt Management in CashFlowScreen
The Android application SHALL allow users to tap any recurring debt card in `CashFlowScreen` to modify its properties (name, monthly amount, due day, scope) or delete it from the system.

#### Scenario: Modifying a recurring debt
- **GIVEN** an authenticated user on `CashFlowScreen` with an existing recurring debt
- **WHEN** the user taps the recurring debt card
- **THEN** the app opens `EditRecurringDebtDialog` pre-filled with the debt's current values, and upon saving, calls `viewModel.updateRecurringDebt(...)` and refreshes the list upon success.

#### Scenario: Deleting a recurring debt
- **GIVEN** an open `EditRecurringDebtDialog` for an existing recurring debt
- **WHEN** the user taps the delete action and confirms
- **THEN** the app calls `viewModel.deleteRecurringDebt(...)`, removes the item, dismisses the dialog, and updates the UI.
