# Delta Spec: Gestión de Miembros, Auto-Rebalanceo y Rollover de Ciclos

## ADDED Requirements

### Requirement: Admin Member Removal
The system SHALL allow the administrator of a household to remove any non-admin member from the household.

#### Scenario: Admin removes an existing member
- **WHEN** the household admin requests `DELETE /api/v1/households/members/:user_id` for a valid member
- **THEN** the system removes the member record, clears the target user's `active_household_id`, and triggers automatic split rebalancing for the remaining members.

#### Scenario: Non-admin attempts to remove a member
- **WHEN** a regular member attempts to call `DELETE /api/v1/households/members/:user_id`
- **THEN** the system rejects the request with HTTP 403 Forbidden.

#### Scenario: Admin attempts to remove themselves
- **WHEN** the admin attempts to delete their own member ID via the member removal endpoint
- **THEN** the system rejects the request instructing the admin to either transfer ownership or delete the household.

---

### Requirement: Automatic Equitable Split Rebalancing
The system SHALL automatically recalculate and persist equitable split percentages across all active members whenever a member joins, is removed, or leaves the household, guaranteeing that the sum of split percentages is always exactly 100.0%.

#### Scenario: Second member joins
- **WHEN** a user joins a 1-person household (which had 100.0%)
- **THEN** both members are updated to 50.00% and 50.00%, totaling 100.0%.

#### Scenario: Third member joins
- **WHEN** a third user joins a 2-person household
- **THEN** the system updates all 3 members to equitable shares (33.34%, 33.33%, 33.33%), totaling exactly 100.0%.

#### Scenario: Member leaves or is removed
- **WHEN** a member leaves a 3-person household
- **THEN** the remaining 2 members are automatically updated to 50.00% each, totaling 100.0%.

---

### Requirement: Cycle History Detail Breakdown
The system SHALL provide a detailed view of any past billing cycle, including its total expense, the list of household expenses recorded in that cycle window, the individual contributions per member, and settlement status.

#### Scenario: Fetch cycle detail
- **WHEN** a household member requests `GET /api/v1/households/cycles/:cycle_index`
- **THEN** the system returns the date range, total sum, itemized expense list with payers, individual member contributions vs expected shares, and net balance settlement.

---

### Requirement: Rollover of Unsettled Cycle Debts
The system SHALL carry over unpaid debt balances from the immediately preceding billing cycle into the active billing cycle's net balance calculation if the previous cycle concluded with unpaid obligations.

#### Scenario: Debtor does not settle past cycle
- **WHEN** Member A owed \$50,000 COP at the end of Cycle N-1 and no settlement payment was recorded
- **THEN** in Cycle N's balance calculation, Member A's net balance starts with an initial deficit of -\$50,000 COP, and the creditor starts with +\$50,000 COP.

---

### Requirement: Month Clamping for End-of-Month Cutoff Days
The system SHALL clamp billing cycle cutoffs higher than the last day of the target month to that month's actual maximum day (e.g. Day 31 clamps to Day 28 or Day 29 in February, and Day 30 in April, June, September, November).

#### Scenario: Day 31 cutoff in February
- **WHEN** the household billing cycle day is set to 31 and the period reaches February
- **THEN** the cycle period starts on the last day of February (28 or 29) without crashing or overflowing into March.
