import axios from 'axios';
import type {
  AuthResponse,
  Household,
  HouseholdBalance,
  Expense,
  Invoice,
  SplitResult,
  BankTransaction,
  CashFlowSummary,
  Income,
} from './types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor for attaching Bearer token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor for 401 Unauthorized handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Authentication Endpoints
export const authApi = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/login', credentials);
    return res.data;
  },
  register: async (data: { email: string; password: string; name: string; tax_id?: string }): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/register', data);
    return res.data;
  },
  getOAuthUrl: async (provider: string): Promise<{ url: string }> => {
    const res = await apiClient.get<{ url: string }>(`/auth/oauth/${provider}/url`);
    return res.data;
  },
};

// Household Endpoints
export const householdApi = {
  create: async (name: string): Promise<Household> => {
    const res = await apiClient.post<Household>('/households', { name });
    return res.data;
  },
  join: async (invite_code: string): Promise<Household> => {
    const res = await apiClient.post<Household>('/households/join', { invite_code });
    return res.data;
  },
  getBalance: async (): Promise<HouseholdBalance> => {
    const res = await apiClient.get<HouseholdBalance>('/households/balance');
    return res.data;
  },
};

// Expense Endpoints
export const expenseApi = {
  list: async (params?: { category?: string; scope?: string }): Promise<Expense[]> => {
    const res = await apiClient.get<Expense[]>('/expenses', { params });
    return res.data;
  },
  create: async (data: {
    amount: number;
    date: string;
    category: string;
    description: string;
    scope: 'PERSONAL' | 'HOUSEHOLD';
    household_id?: string;
  }): Promise<Expense> => {
    const res = await apiClient.post<Expense>('/expenses', data);
    return res.data;
  },
};

// Invoice & DIAN Splitting Endpoints
export const invoiceApi = {
  lookupDianQR: async (data: { qr_text?: string; cufe?: string; nit?: string }): Promise<Invoice> => {
    const res = await apiClient.post<Invoice>('/invoices/lookup-dian', data);
    return res.data;
  },
  uploadZip: async (file: File): Promise<Invoice> => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.post<Invoice>('/invoices/upload-zip', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },
  splitItems: async (
    invoiceId: string,
    selectedItemIds: string[],
    selectedAs: 'PERSONAL' | 'HOUSEHOLD'
  ): Promise<SplitResult> => {
    const res = await apiClient.post<SplitResult>(`/invoices/${invoiceId}/split`, {
      selected_item_ids: selectedItemIds,
      selected_as: selectedAs,
    });
    return res.data;
  },
};

// Bank Reconciliation Endpoints
export const reconciliationApi = {
  uploadStatement: async (file: File, bankName?: string): Promise<{ statement: any; transactions: BankTransaction[] }> => {
    const formData = new FormData();
    formData.append('file', file);
    if (bankName) formData.append('bank_name', bankName);
    const res = await apiClient.post('/reconciliation/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },
  getDuplicates: async (): Promise<BankTransaction[]> => {
    const res = await apiClient.get<BankTransaction[]>('/reconciliation/duplicates');
    return res.data;
  },
  linkDuplicate: async (transactionId: string, expenseId: string): Promise<void> => {
    await apiClient.post('/reconciliation/link', {
      transaction_id: transactionId,
      expense_id: expenseId,
    });
  },
  dismissDuplicate: async (transactionId: string): Promise<void> => {
    await apiClient.post('/reconciliation/dismiss', {
      transaction_id: transactionId,
    });
  },
};

// Cash Flow & Incomes Endpoints
export const cashFlowApi = {
  getMonthlySummary: async (): Promise<CashFlowSummary> => {
    const res = await apiClient.get<CashFlowSummary>('/cashflow/monthly');
    return res.data;
  },
  listIncomes: async (): Promise<Income[]> => {
    const res = await apiClient.get<Income[]>('/incomes');
    return res.data;
  },
  createIncome: async (data: {
    amount: number;
    type: 'SALARY_REGULAR' | 'PRIMA' | 'BONUS' | 'FREELANCE';
    date: string;
    description: string;
  }): Promise<Income> => {
    const res = await apiClient.post<Income>('/incomes', data);
    return res.data;
  },
};
