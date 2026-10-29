# Delta Spec: Expenses & Recurring Debt Management

## Requirements

### Requirement: Recurring Debt Lifecycle Management (Update and Delete)
The backend system SHALL support modification and deletion of recurring debts and subscriptions (`RecurringDebt`) via authenticated REST endpoints (`PUT /api/v1/finance/debts/:id` and `DELETE /api/v1/finance/debts/:id`). A user SHALL ONLY be permitted to update or delete debts that belong to their own account (`user_id == caller_id`).

#### Scenario: User updates an existing recurring debt
- **GIVEN** an authenticated user who owns an active recurring debt record
- **WHEN** the user submits a `PUT /api/v1/finance/debts/:id` request with updated `name`, `monthly_amount`, `due_day`, and `scope`
- **THEN** the system validates that `due_day` is between 1 and 31, `monthly_amount` is greater than 0, updates the record in the database, and returns `HTTP 200 OK` with the updated `RecurringDebt` object.

#### Scenario: User deletes a recurring debt
- **GIVEN** an authenticated user who owns a recurring debt record
- **WHEN** the user submits a `DELETE /api/v1/finance/debts/:id` request
- **THEN** the system deletes the record from the database and returns `HTTP 200 OK` with a confirmation message.

#### Scenario: Unauthorized attempt to modify or delete another user's recurring debt
- **GIVEN** an authenticated user `User A` and a recurring debt created by `User B`
- **WHEN** `User A` attempts to send `PUT /api/v1/finance/debts/:id` or `DELETE /api/v1/finance/debts/:id` for `User B`'s debt
- **THEN** the system rejects the request with `HTTP 403 Forbidden` or `HTTP 404 Not Found`, without modifying the debt record.
