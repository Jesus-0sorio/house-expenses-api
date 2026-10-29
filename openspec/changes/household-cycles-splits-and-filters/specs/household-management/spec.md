# Delta Spec: Gestión Avanzada de Hogares, Ciclos y Reparto Variable

## ADDED Requirements

### Requirement: 8-Character Invitation Code Support
The system SHALL accept and validate invitation codes of 8 alphanumeric characters for joining a household.

#### Scenario: Join with 8-character code
- **WHEN** a user submits an 8-character invitation code (e.g. `7FB908CB`)
- **THEN** the system validates the format, finds the corresponding household, and links the user as a member.

### Requirement: Member Listing and Variable Split Percentages
The system SHALL allow the administrator/creator of a household to view all members and configure customized split percentages for each member, ensuring the sum of all percentages equals 100.0%.

#### Scenario: Update member splits
- **WHEN** the household admin submits new split percentages for members (e.g., 60.0% and 40.0%, or 33.34%, 33.33%, 33.33%)
- **THEN** the system verifies that the sum equals 100.0%, updates the `HouseholdMember` records, and recalibrates the settlement expected share accordingly.

#### Scenario: Reject invalid split sum
- **WHEN** the submitted split percentages do not sum to 100.0%
- **THEN** the system rejects the update with a 400 Bad Request error.

### Requirement: Household Deletion and Member Leaving
The system SHALL allow the household creator/admin to delete the household, unlinking all members and resetting their active household ID, and allow individual non-admin members to leave.

#### Scenario: Admin deletes household
- **WHEN** the admin confirms household deletion
- **THEN** all member associations are cleared, active household pointers are set to null, and the household record is deleted.

#### Scenario: Member leaves household
- **WHEN** a member leaves the household
- **THEN** the member's association is removed and their `active_household_id` is set to null.

### Requirement: Configurable Billing Cycle
The system SHALL allow configuring a monthly billing cycle start day (e.g. day 29), grouping household expenses by the active cycle (e.g. 29 Aug to 28 Sep) rather than calendar months.

#### Scenario: Configure billing cycle cutoff
- **WHEN** the admin sets the billing cycle start day to 29
- **THEN** household expenses from day 29 of the current month cycle are grouped into the active period, and prior periods are archived in cycle history.

### Requirement: Household Cycle History and Settlement Status
The system SHALL maintain a history of completed billing cycles with their respective total expense, member shares, and status (`PENDING` or `SETTLED`).

#### Scenario: Mark cycle as settled
- **WHEN** members settle debts for a past cycle and register the payment
- **THEN** the cycle status updates from `PENDING` to `SETTLED`.
