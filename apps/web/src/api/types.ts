export interface User {
  id: string;
  email: string;
  name: string;
  avatar_url?: string;
  tax_id?: string;
  primary_auth_provider: string;
  active_household_id?: string;
  created_at: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Household {
  id: string;
  name: string;
  invite_code: string;
  created_at: string;
}

export interface HouseholdBalance {
  household_id: string;
  user_paid: number;
  roomie_paid: number;
  net_difference: number;
  debt_amount: number;
  debtor_name: string;
  creditor_name: string;
  settlement_summary: string;
}

export type ExpenseScope = 'PERSONAL' | 'HOUSEHOLD';

export interface Expense {
  id: string;
  user_id: string;
  household_id?: string;
  invoice_id?: string;
  amount: number;
  date: string;
  category: string;
  description: string;
  scope: ExpenseScope;
  payment_status: string;
  created_at: string;
}

export interface InvoiceItem {
  id: string;
  invoice_id: string;
  description: string;
  quantity: number;
  unit_price: number;
  total_price: number;
  classification: ExpenseScope;
}

export interface Invoice {
  id: string;
  cufe?: string;
  issuer_name: string;
  issuer_nit: string;
  invoice_number: string;
  issue_date: string;
  total_amount: number;
  tax_amount: number;
  items: InvoiceItem[];
}

export interface SplitResult {
  items: InvoiceItem[];
  personal_total: number;
  household_total: number;
}

export interface BankTransaction {
  id: string;
  statement_id: string;
  transaction_date: string;
  description: string;
  amount: number;
  matched_expense_id?: string;
  duplicate_status: 'NONE' | 'SUSPECTED' | 'CONFIRMED' | 'DISMISSED';
  confidence_score?: number;
}

export interface CashFlowSummary {
  month: string;
  total_incomes: number;
  personal_expenses: number;
  fixed_debts: number;
  household_share: number;
  net_disposable_cash: number;
}

export interface Income {
  id: string;
  user_id: string;
  amount: number;
  type: 'SALARY_REGULAR' | 'PRIMA' | 'BONUS' | 'FREELANCE';
  date: string;
  description: string;
}
