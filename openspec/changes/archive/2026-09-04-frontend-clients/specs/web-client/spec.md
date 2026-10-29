## Purpose

Provides a responsive Web application for users to manage personal and household finances, split receipts, reconcile bank statements, and monitor cash flow.

## ADDED Requirements

### Requirement: User Authentication and Adaptive Experience
The web application SHALL authenticate users via email/password and Google OAuth, dynamically adapting the interface based on whether the user is in Solitary mode (single member) or Household mode (shared roomies).

#### Scenario: Solitary user login
- **WHEN** a solitary user logs in
- **THEN** the application displays a streamlined personal dashboard hiding roomie balance ledgers

#### Scenario: Household member login
- **WHEN** a user who belongs to a multi-member household logs in
- **THEN** the application activates the shared household section, showing the 50/50 balance summary and settlement actions

### Requirement: Expense Management and Inverse Supermarket Splitting
The web application SHALL allow users to create and categorize expenses and perform inverse item selection on itemized receipts.

#### Scenario: Inverse supermarket receipt split
- **WHEN** a user selects 2 personal items from a 15-item supermarket invoice
- **THEN** the application marks the 2 items as PERSONAL and automatically classifies all 13 remaining items as HOUSEHOLD, recalculating the split amounts in real-time

### Requirement: Bank Reconciliation and Duplicate Resolution
The web application SHALL allow uploading CSV/Excel bank statements and reviewing suspected duplicate transactions.

#### Scenario: Review suspected duplicate
- **WHEN** the system identifies a bank statement transaction with >=80% confidence score matching an existing expense
- **THEN** the user can confirm the link or dismiss the suspicion with a single click

### Requirement: Net Cash Flow Visualization
The web application SHALL display the user's monthly income, fixed debts, personal expenses, and household share in a visual cash flow chart.

#### Scenario: View monthly cash flow
- **WHEN** user opens the Cash Flow view
- **THEN** the system visualizes Total Incomes, Total Expenses, and Net Disposable Cash Flow
