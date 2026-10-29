# Delta Spec: Sincronización y Visibilidad de Gastos del Hogar

## ADDED Requirements

### Requirement: Cross-Member Household Expense Synchronization
The system SHALL return all expenses belonging to a user's active household (`scope = 'HOUSEHOLD' AND household_id = activeHouseholdID`), in addition to the user's own personal expenses (`user_id = userID`), when querying `GET /api/v1/expenses`.

#### Scenario: User B views expense created by User A in the same household
- **GIVEN** User A and User B belong to the same active household
- **WHEN** User A creates an expense with `scope = 'HOUSEHOLD'` (e.g. "Supermercado Éxito $150,000 COP")
- **AND** User B lists expenses or syncs their mobile app via `GET /api/v1/expenses`
- **THEN** User A's expense is included in the response, allowing User B to view it in their transaction list and incorporate it into their local store.

---

### Requirement: Complete Household Balance Model
The system SHALL expose the total household expenditure, the exact amount paid by each individual member, each member's expected contribution based on their split percentage, their net balance (debt or credit), and a human-readable settlement summary in `GET /api/v1/households/balance`.

#### Scenario: Multi-member expense differential settlement
- **GIVEN** a 2-person household with 50/50 split
- **WHEN** Member 1 pays \$230,000 COP for utilities and Member 2 pays \$555,000 COP for groceries (Total: \$785,000 COP, Expected each: \$392,500 COP)
- **THEN** the balance response indicates:
  - `total_household`: 785000.0
  - Member 1: Paid \$230,000, Expected \$392,500, Net Balance: -\$162,500 COP (owes)
  - Member 2: Paid \$555,000, Expected \$392,500, Net Balance: +\$162,500 COP (receives)
  - `settlement`: "Member 1 le debe \$162,500 COP a Member 2."
