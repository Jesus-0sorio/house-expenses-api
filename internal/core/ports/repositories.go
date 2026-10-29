package ports

import (
	"context"
	"time"

	"github.com/google/uuid"

	"house-expenses/internal/core/domain"
)

type UserRepository interface {
	Create(ctx context.Context, user *domain.User) error
	FindByID(ctx context.Context, id uuid.UUID) (*domain.User, error)
	FindByEmail(ctx context.Context, email string) (*domain.User, error)
	Update(ctx context.Context, user *domain.User) error
	FindOAuthAccount(ctx context.Context, provider domain.AuthProvider, providerUserID string) (*domain.UserOAuthAccount, error)
	CreateOAuthAccount(ctx context.Context, account *domain.UserOAuthAccount) error
}

type HouseholdRepository interface {
	Create(ctx context.Context, household *domain.Household) error
	FindByID(ctx context.Context, id uuid.UUID) (*domain.Household, error)
	FindByInviteCode(ctx context.Context, code string) (*domain.Household, error)
	Update(ctx context.Context, household *domain.Household) error
	Delete(ctx context.Context, id uuid.UUID) error
	AddMember(ctx context.Context, member *domain.HouseholdMember) error
	UpdateMember(ctx context.Context, member *domain.HouseholdMember) error
	RemoveMember(ctx context.Context, householdID, userID uuid.UUID) error
	GetMembers(ctx context.Context, householdID uuid.UUID) ([]domain.HouseholdMember, error)
	FindMember(ctx context.Context, householdID, userID uuid.UUID) (*domain.HouseholdMember, error)
	CreatePayment(ctx context.Context, payment *domain.SettlementPayment) error
	FindPaymentByID(ctx context.Context, id uuid.UUID) (*domain.SettlementPayment, error)
	GetPaymentsByHouseholdAndPeriod(ctx context.Context, householdID uuid.UUID, start, end time.Time) ([]domain.SettlementPayment, error)
	GetPaymentsByHousehold(ctx context.Context, householdID uuid.UUID, limit, offset int) ([]domain.SettlementPayment, error)
}

type ExpenseRepository interface {
	Create(ctx context.Context, expense *domain.Expense) error
	FindByID(ctx context.Context, id uuid.UUID) (*domain.Expense, error)
	FindByUser(ctx context.Context, userID uuid.UUID, limit, offset int) ([]domain.Expense, error)
	FindByHousehold(ctx context.Context, householdID uuid.UUID, limit, offset int) ([]domain.Expense, error)
	FindByUserOrHousehold(ctx context.Context, userID uuid.UUID, householdID *uuid.UUID, limit, offset int) ([]domain.Expense, error)
	FindByUserAndPeriod(ctx context.Context, userID uuid.UUID, start, end time.Time) ([]domain.Expense, error)
	FindByHouseholdAndPeriod(ctx context.Context, householdID uuid.UUID, start, end time.Time) ([]domain.Expense, error)
	Update(ctx context.Context, expense *domain.Expense) error
	Delete(ctx context.Context, id uuid.UUID) error
	FindClassificationRule(ctx context.Context, userID uuid.UUID, merchant string) (*domain.ClassificationRule, error)
	SaveClassificationRule(ctx context.Context, rule *domain.ClassificationRule) error
}

type InvoiceRepository interface {
	Create(ctx context.Context, invoice *domain.Invoice) error
	FindByID(ctx context.Context, id uuid.UUID) (*domain.Invoice, error)
	FindByCUFE(ctx context.Context, cufe string) (*domain.Invoice, error)
	Update(ctx context.Context, invoice *domain.Invoice) error
	UpdateItems(ctx context.Context, items []domain.InvoiceItem) error
}

type BankStatementRepository interface {
	Create(ctx context.Context, statement *domain.BankStatement) error
	FindByID(ctx context.Context, id uuid.UUID) (*domain.BankStatement, error)
	ListTransactionsByStatement(ctx context.Context, statementID uuid.UUID) ([]domain.BankTransaction, error)
	FindSuspectedDuplicates(ctx context.Context, userID uuid.UUID) ([]domain.BankTransaction, error)
	FindTransactionByID(ctx context.Context, id uuid.UUID) (*domain.BankTransaction, error)
	UpdateTransaction(ctx context.Context, tx *domain.BankTransaction) error
}

type IncomeRepository interface {
	CreateIncome(ctx context.Context, income *domain.Income) error
	FindIncomeByID(ctx context.Context, id uuid.UUID) (*domain.Income, error)
	ListIncomesByUser(ctx context.Context, userID uuid.UUID) ([]domain.Income, error)
	ListIncomesByUserAndPeriod(ctx context.Context, userID uuid.UUID, start, end time.Time) ([]domain.Income, error)
	CreateDebt(ctx context.Context, debt *domain.RecurringDebt) error
	FindDebtByID(ctx context.Context, id uuid.UUID) (*domain.RecurringDebt, error)
	ListDebtsByUser(ctx context.Context, userID uuid.UUID) ([]domain.RecurringDebt, error)
	UpdateDebt(ctx context.Context, debt *domain.RecurringDebt) error
	DeleteDebt(ctx context.Context, id uuid.UUID) error
}
