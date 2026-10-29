# Delta Spec: Android Client UX, Filtros Avanzados y Control de Hogar

## ADDED Requirements

### Requirement: 8-Character Household Code Input
The Android client SHALL update input limits and placeholders to support 8-character invitation codes with automatic capitalization.

#### Scenario: User types 8-character code
- **WHEN** the user inputs an 8-character invitation code (e.g. `7FB908CB`)
- **THEN** the input permits all 8 characters and enables the "Unirse al Hogar" action.

### Requirement: Household Members Display and Variable Split Configuration
The Android client SHALL display the list of all members belonging to the active household, showing their names, emails, roles (Admin/Member), and split percentages, and allow admins to edit the percentage distribution.

#### Scenario: Admin adjusts member percentages
- **WHEN** an admin modifies member split sliders or text inputs to sum 100% and taps Save
- **THEN** the client dispatches the update to the backend and updates the displayed percentages and balances.

### Requirement: Delete and Leave Household Actions
The Android client SHALL provide a destructive "Eliminar Hogar" option for the admin and an "Abandonar Hogar" option for regular members, with confirmation dialogs.

#### Scenario: Admin confirms household deletion
- **WHEN** the admin clicks "Eliminar Hogar" and confirms the alert dialog
- **THEN** the household is removed, the user is transitioned back to Solitary mode, and the session state is updated.

### Requirement: Current Month Metrics on Dashboard
The Android client Dashboard SHALL calculate and display the total expenses registered in the current calendar month (`YYYY-MM`), rather than the all-time total.

#### Scenario: View current month expenses
- **WHEN** the user opens the Dashboard
- **THEN** the main metric card displays "Gastos del Mes (Septiembre)" summing only transactions dated in the current month.

### Requirement: Advanced Expense History Filtering
The Android client `ExpensesListScreen` SHALL provide comprehensive search and filtering capabilities:
1. Search bar for matching merchant names and descriptions.
2. Month filter selector (e.g. Current Month, Previous Month, or YYYY-MM).
3. Category filter chip selector.
4. Date range filter (From Date to To Date).
5. Amount filter (Exact amount or Min/Max amount range).
6. Reset/Clear all active filters button.

#### Scenario: Filter expenses by search query and category
- **WHEN** the user searches "D1" and selects category "Supermercado"
- **THEN** the list immediately shows only matching expenses within the filtered criteria.

#### Scenario: Filter expenses by value range
- **WHEN** the user enters minimum $50.000 and maximum $200.000
- **THEN** only expenses with amounts within this range are shown.

### Requirement: Integrated Centered '+' Navigation Item
The Android client bottom navigation bar SHALL integrate the quick-action `+` button as a centered item in the bar layout (5 tabs: Inicio, Gastos, [+], Flujo, Hogar), without floating above or blocking other elements.

#### Scenario: Tap central '+' button
- **WHEN** the user taps the center `+` icon in the navigation bar
- **THEN** the quick action sheet opens (QR Scanner / Manual Expense) smoothly.

### Requirement: Billing Cycle Configuration & Settlement History
The Android client SHALL provide an interface to configure the household's billing cutoff day, view the current cycle balance, and consult previous cycles with their respective "Pendiente" or "Saldado" status badges.

#### Scenario: View cycle status
- **WHEN** the user reviews cycle history
- **THEN** each past cycle shows start and end dates, total spent, member shares, and a badge indicating whether it is `PENDING` or `SETTLED`.
