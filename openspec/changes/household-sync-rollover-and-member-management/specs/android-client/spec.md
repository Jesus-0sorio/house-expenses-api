# Delta Spec: Interfaz Android de Balances, Detalle de Ciclos y Transacciones Agrupadas

## ADDED Requirements

### Requirement: Household Balance UI with Member Breakdown
The Android application SHALL parse the full `SettlementSummary` response and display a detailed shared balance card containing:
1. Total expenditure of the household in the active billing cycle.
2. Itemized list of each member with: Total amount paid, Expected share based on split %, and Net balance (positive in green = "a favor", negative in red/orange = "debe pagar").
3. Human-readable settlement instruction string explaining exactly who owes whom.

#### Scenario: Display multi-member balance
- **WHEN** the user opens the Household screen with active expenses
- **THEN** the balance card renders the total household sum and individual breakdown for every roomie.

---

### Requirement: Cycle History Detail Modal
The Android application SHALL display an interactive modal or bottom sheet when the user taps any card in the cycle history list, showing:
1. The cycle date range and final total amount.
2. The settlement status badge (Saldado / Pendiente).
3. The list of household expenses recorded in that cycle (description, payer, category, amount).
4. The final net balance per member and whether debts were settled or rolled over.

#### Scenario: Tap cycle history card
- **WHEN** a user taps a past cycle in the history
- **THEN** a `ModalBottomSheet` or dialog opens displaying the itemized expenses and settlement summary of that cycle.

---

### Requirement: Admin Member Removal UI
The Android application SHALL provide a visible remove button next to each non-admin member in the household members list for the admin, with a destructive confirmation dialog.

#### Scenario: Admin removes a member
- **WHEN** the admin clicks the remove icon next to a member and confirms
- **THEN** the app triggers the removal API, refreshes members and splits, and displays a success toast.

---

### Requirement: Hierarchical Transactions Grouping & Infinite Scroll
The Android application SHALL organize recent transactions hierarchically into Month sections (e.g. "Septiembre 2026"), sub-divided by Day sections (e.g. "Hoy - 5 de Septiembre", "Ayer - 4 de Septiembre", "28 de Agosto"), and support progressive infinite scrolling.

#### Scenario: Scroll through multiple transactions
- **WHEN** the user scrolls down the expenses list
- **THEN** sticky or grouped headers distinctly separate transactions by month and day, and additional transactions are loaded seamlessly as the user approaches the bottom of the list.
