# Delta Spec: Modo Oscuro/Claro, Edición y Gestión de Hogares

## ADDED Requirements

### Requirement: Dynamic Dark and Light Theme
The Android application SHALL support toggling between Dark and Light color schemes, persisting the user's choice in local preferences and dynamically applying semantic colors with high-contrast text across all screens.

#### Scenario: User toggles theme
- **WHEN** the user clicks the theme toggle button in the top bar
- **THEN** the application switches between Dark Mode (dark background, light text) and Light Mode (white/light-slate background, dark text) immediately without restarting.

### Requirement: Transaction Editing
The Android application SHALL allow users to edit existing expenses and incomes, updating their amount, category, description, scope, and date, reflecting the changes in Room SQLite and dispatching an update to the backend.

#### Scenario: Edit an expense
- **WHEN** the user selects an expense from the history or dashboard and modifies its amount or description
- **THEN** the application updates the record locally and queues/sends the updated entity to the backend.

#### Scenario: Edit an income
- **WHEN** the user clicks an income item in the Cash Flow screen and edits the amount or frequency
- **THEN** the application updates the local database and recalibrates the available cash flow balance immediately.

### Requirement: Household Creation and Code Joining
The Android application SHALL allow solitary users to create a new household by specifying a name, or join an existing household by entering an invitation code.

#### Scenario: Create household
- **WHEN** a solitary user enters a household name and confirms
- **THEN** the backend creates the household, generates an invitation code, assigns the user as creator, and updates the user's active household ID.

#### Scenario: Join household via code
- **WHEN** a user enters a valid 6-character invitation code
- **THEN** the application joins the household, switches to household mode, and activates the 50/50 balance summary.

### Requirement: Deep Link Household Joining
The Android application SHALL handle deep links (`houseexpenses://household/join?code={code}` and `https://houseexpenses.app/join/{code}`) to automatically open the join confirmation dialog with the invitation code pre-populated.

#### Scenario: Open deep link
- **WHEN** the user clicks a shared household invite link on their mobile device
- **THEN** the app opens, extracts the invitation code, and prompts the user to join the household in one tap.
