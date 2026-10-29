package usecase

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

var (
	ErrDebtNotFound     = errors.New("recurring debt not found")
	ErrUnauthorizedDebt = errors.New("unauthorized to modify this recurring debt")
)

type CashFlowUseCase struct {
	incomeRepo    ports.IncomeRepository
	expenseRepo   ports.ExpenseRepository
	userRepo      ports.UserRepository
	householdRepo ports.HouseholdRepository
}

func NewCashFlowUseCase(
	incomeRepo ports.IncomeRepository,
	expenseRepo ports.ExpenseRepository,
	userRepo ports.UserRepository,
	householdRepo ports.HouseholdRepository,
) *CashFlowUseCase {
	return &CashFlowUseCase{
		incomeRepo:    incomeRepo,
		expenseRepo:   expenseRepo,
		userRepo:      userRepo,
		householdRepo: householdRepo,
	}
}

type CreateIncomeInput struct {
	Amount      float64           `json:"amount" binding:"required,gt=0"`
	Type        domain.IncomeType `json:"type" binding:"required"`
	Date        time.Time         `json:"date"`
	Description string            `json:"description"`
}

func (uc *CashFlowUseCase) CreateIncome(ctx context.Context, userID uuid.UUID, input CreateIncomeInput) (*domain.Income, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	date := input.Date
	if date.IsZero() {
		date = time.Now()
	}

	income := &domain.Income{
		ID:          uuid.New(),
		UserID:      userID,
		Amount:      input.Amount,
		Type:        input.Type,
		Date:        date,
		Description: input.Description,
		CreatedAt:   time.Now(),
	}

	if err := uc.incomeRepo.CreateIncome(ctx, income); err != nil {
		return nil, fmt.Errorf("failed to save income: %w", err)
	}

	return income, nil
}

func (uc *CashFlowUseCase) ListIncomes(ctx context.Context, userID uuid.UUID) ([]domain.Income, error) {
	return uc.incomeRepo.ListIncomesByUser(ctx, userID)
}

type CreateDebtInput struct {
	Name                  string              `json:"name" binding:"required"`
	MonthlyAmount         float64             `json:"monthly_amount" binding:"required,gt=0"`
	DueDay                int                 `json:"due_day" binding:"required,min=1,max=31"`
	RemainingInstallments int                 `json:"remaining_installments"`
	Scope                 domain.ExpenseScope `json:"scope"`
}

func (uc *CashFlowUseCase) CreateDebt(ctx context.Context, userID uuid.UUID, input CreateDebtInput) (*domain.RecurringDebt, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	scope := input.Scope
	if scope == "" {
		scope = domain.ExpenseScopePersonal
	}

	debt := &domain.RecurringDebt{
		ID:                    uuid.New(),
		UserID:                userID,
		Name:                  input.Name,
		MonthlyAmount:         input.MonthlyAmount,
		DueDay:                input.DueDay,
		RemainingInstallments: input.RemainingInstallments,
		Scope:                 scope,
		CreatedAt:             time.Now(),
	}

	if err := uc.incomeRepo.CreateDebt(ctx, debt); err != nil {
		return nil, fmt.Errorf("failed to save recurring debt: %w", err)
	}

	return debt, nil
}

func (uc *CashFlowUseCase) ListDebts(ctx context.Context, userID uuid.UUID) ([]domain.RecurringDebt, error) {
	return uc.incomeRepo.ListDebtsByUser(ctx, userID)
}

type UpdateDebtInput struct {
	Name                  string              `json:"name" binding:"required"`
	MonthlyAmount         float64             `json:"monthly_amount" binding:"required,gt=0"`
	DueDay                int                 `json:"due_day" binding:"required,min=1,max=31"`
	RemainingInstallments int                 `json:"remaining_installments"`
	Scope                 domain.ExpenseScope `json:"scope"`
}

func (uc *CashFlowUseCase) UpdateDebt(ctx context.Context, userID, debtID uuid.UUID, input UpdateDebtInput) (*domain.RecurringDebt, error) {
	debt, err := uc.incomeRepo.FindDebtByID(ctx, debtID)
	if err != nil {
		return nil, ErrDebtNotFound
	}
	if debt.UserID != userID {
		return nil, ErrUnauthorizedDebt
	}

	scope := input.Scope
	if scope == "" {
		scope = domain.ExpenseScopePersonal
	}

	debt.Name = input.Name
	debt.MonthlyAmount = input.MonthlyAmount
	debt.DueDay = input.DueDay
	debt.RemainingInstallments = input.RemainingInstallments
	debt.Scope = scope

	if err := uc.incomeRepo.UpdateDebt(ctx, debt); err != nil {
		return nil, fmt.Errorf("failed to update recurring debt: %w", err)
	}

	return debt, nil
}

func (uc *CashFlowUseCase) DeleteDebt(ctx context.Context, userID, debtID uuid.UUID) error {
	debt, err := uc.incomeRepo.FindDebtByID(ctx, debtID)
	if err != nil {
		return ErrDebtNotFound
	}
	if debt.UserID != userID {
		return ErrUnauthorizedDebt
	}

	if err := uc.incomeRepo.DeleteDebt(ctx, debtID); err != nil {
		return fmt.Errorf("failed to delete recurring debt: %w", err)
	}

	return nil
}

type CashFlowSummary struct {
	Year               int     `json:"year"`
	Month              int     `json:"month"`
	TotalIncome        float64 `json:"total_income"`
	PersonalExpenses   float64 `json:"personal_expenses"`
	FixedLiabilities   float64 `json:"fixed_liabilities"`
	HouseholdDebtShare float64 `json:"household_debt_share"`
	NetDisposable      float64 `json:"net_disposable"`
}

func (uc *CashFlowUseCase) CalculateCashFlow(ctx context.Context, userID uuid.UUID, year, month int) (*CashFlowSummary, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	if year <= 0 {
		year = time.Now().Year()
	}
	if month <= 0 || month > 12 {
		month = int(time.Now().Month())
	}

	startDate := time.Date(year, time.Month(month), 1, 0, 0, 0, 0, time.UTC)
	// Last moment of the month
	endDate := startDate.AddDate(0, 1, 0).Add(-time.Nanosecond)

	// 1. Total Income
	incomes, err := uc.incomeRepo.ListIncomesByUserAndPeriod(ctx, userID, startDate, endDate)
	if err != nil {
		return nil, fmt.Errorf("failed to query incomes: %w", err)
	}
	var totalIncome float64
	for _, inc := range incomes {
		totalIncome += inc.Amount
	}

	// 2. Personal Expenses
	expenses, err := uc.expenseRepo.FindByUserAndPeriod(ctx, userID, startDate, endDate)
	if err != nil {
		return nil, fmt.Errorf("failed to query user expenses: %w", err)
	}
	var personalExpenses float64
	for _, exp := range expenses {
		if exp.Scope == domain.ExpenseScopePersonal {
			personalExpenses += exp.Amount
		}
	}

	// 3. Fixed Liabilities (Monthly recurring debts)
	debts, err := uc.incomeRepo.ListDebtsByUser(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to query debts: %w", err)
	}
	var fixedLiabilities float64
	for _, d := range debts {
		fixedLiabilities += d.MonthlyAmount
	}

	// 4. Household Debt Share
	var householdDebtShare float64
	if user.ActiveHouseholdID != nil {
		householdID := *user.ActiveHouseholdID
		member, err := uc.householdRepo.FindMember(ctx, householdID, userID)
		if err == nil && member != nil {
			splitPct := member.SplitPercentage
			if splitPct <= 0 {
				splitPct = 50.0
			}

			householdExpenses, err := uc.expenseRepo.FindByHouseholdAndPeriod(ctx, householdID, startDate, endDate)
			if err == nil {
				var totalHouseholdExp float64
				for _, he := range householdExpenses {
					if he.Scope == domain.ExpenseScopeHousehold {
						totalHouseholdExp += he.Amount
					}
				}
				householdDebtShare = totalHouseholdExp * (splitPct / 100.0)
			}
		}
	}

	// 5. Net Disposable = Total Income - (Personal Expenses + Fixed Liabilities + Household Debt Share)
	netDisposable := totalIncome - (personalExpenses + fixedLiabilities + householdDebtShare)

	return &CashFlowSummary{
		Year:               year,
		Month:              month,
		TotalIncome:        totalIncome,
		PersonalExpenses:   personalExpenses,
		FixedLiabilities:   fixedLiabilities,
		HouseholdDebtShare: householdDebtShare,
		NetDisposable:      netDisposable,
	}, nil
}
