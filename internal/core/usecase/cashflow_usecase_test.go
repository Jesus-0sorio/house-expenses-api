package usecase_test

import (
	"context"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"house-expenses/internal/adapters/repository"
	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/usecase"
)

func TestCashFlowUseCase(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	householdRepo := repository.NewHouseholdRepository(db)
	expenseRepo := repository.NewExpenseRepository(db)
	incomeRepo := repository.NewIncomeRepository(db)
	ctx := context.Background()

	cashFlowUC := usecase.NewCashFlowUseCase(incomeRepo, expenseRepo, userRepo, householdRepo)

	t.Run("Solitary user net cash flow calculation", func(t *testing.T) {
		user := &domain.User{
			ID:           uuid.New(),
			Email:        "cashflow_solitary@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Solitary Cash Flow User",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, user))

		septemberDate := time.Date(2026, 9, 15, 12, 0, 0, 0, time.UTC)

		// 1. Regular Salary ($5,000,000 COP)
		_, err := cashFlowUC.CreateIncome(ctx, user.ID, usecase.CreateIncomeInput{
			Amount:      5000000.0,
			Type:        domain.IncomeTypeSalaryRegular,
			Date:        septemberDate,
			Description: "Salario Septiembre",
		})
		require.NoError(t, err)

		// 2. Personal Expenses in September ($800,000 COP)
		require.NoError(t, expenseRepo.Create(ctx, &domain.Expense{
			ID:            uuid.New(),
			UserID:        user.ID,
			Amount:        800000.0,
			Date:          septemberDate,
			Category:      "Shopping",
			Description:   "Ropa y calzado",
			Scope:         domain.ExpenseScopePersonal,
			PaymentStatus: domain.ExpenseStatusPaid,
			CreatedAt:     time.Now(),
			UpdatedAt:     time.Now(),
		}))

		// 3. Fixed recurring debts: Car loan ($600,000) and credit card ($200,000) = $800,000
		_, err = cashFlowUC.CreateDebt(ctx, user.ID, usecase.CreateDebtInput{
			Name:                  "Credito Vehiculo",
			MonthlyAmount:         600000.0,
			DueDay:                10,
			RemainingInstallments: 36,
		})
		require.NoError(t, err)

		_, err = cashFlowUC.CreateDebt(ctx, user.ID, usecase.CreateDebtInput{
			Name:                  "Cuota Tarjeta",
			MonthlyAmount:         200000.0,
			DueDay:                20,
			RemainingInstallments: 6,
		})
		require.NoError(t, err)

		// Net = 5,000,000 - (800,000 + 800,000 + 0) = 3,400,000 COP
		summary, err := cashFlowUC.CalculateCashFlow(ctx, user.ID, 2026, 9)
		require.NoError(t, err)
		assert.Equal(t, 2026, summary.Year)
		assert.Equal(t, 9, summary.Month)
		assert.Equal(t, 5000000.0, summary.TotalIncome)
		assert.Equal(t, 800000.0, summary.PersonalExpenses)
		assert.Equal(t, 800000.0, summary.FixedLiabilities)
		assert.Equal(t, 0.0, summary.HouseholdDebtShare)
		assert.Equal(t, 3400000.0, summary.NetDisposable)
	})

	t.Run("Household user net cash flow includes 50% household share", func(t *testing.T) {
		u1 := &domain.User{
			ID:           uuid.New(),
			Email:        "cf_roomie1@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Roomie 1",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, u1))

		household := &domain.Household{
			ID:         uuid.New(),
			Name:       "Roomie House",
			InviteCode: "CFHOUSE",
			CreatedAt:  time.Now(),
			UpdatedAt:  time.Now(),
		}
		require.NoError(t, householdRepo.Create(ctx, household))
		require.NoError(t, householdRepo.AddMember(ctx, &domain.HouseholdMember{
			ID:              uuid.New(),
			HouseholdID:     household.ID,
			UserID:          u1.ID,
			SplitPercentage: 50.0,
			JoinedAt:        time.Now(),
			User:            u1,
		}))

		u1.ActiveHouseholdID = &household.ID
		require.NoError(t, userRepo.Update(ctx, u1))

		septemberDate := time.Date(2026, 9, 20, 10, 0, 0, 0, time.UTC)

		// Income: $4,000,000
		_, err := cashFlowUC.CreateIncome(ctx, u1.ID, usecase.CreateIncomeInput{
			Amount:      4000000.0,
			Type:        domain.IncomeTypeSalaryRegular,
			Date:        septemberDate,
			Description: "Salario Quincenal",
		})
		require.NoError(t, err)

		// Personal Expense: $300,000
		require.NoError(t, expenseRepo.Create(ctx, &domain.Expense{
			ID:            uuid.New(),
			UserID:        u1.ID,
			Amount:        300000.0,
			Date:          septemberDate,
			Category:      "Personal",
			Description:   "Gym & Suplementos",
			Scope:         domain.ExpenseScopePersonal,
			PaymentStatus: domain.ExpenseStatusPaid,
			CreatedAt:     time.Now(),
			UpdatedAt:     time.Now(),
		}))

		// Fixed Debt: $200,000
		_, err = cashFlowUC.CreateDebt(ctx, u1.ID, usecase.CreateDebtInput{
			Name:          "Seguro Vida",
			MonthlyAmount: 200000.0,
			DueDay:        5,
		})
		require.NoError(t, err)

		// Household Expense: $1,000,000 (Rent/Utilities for the house)
		require.NoError(t, expenseRepo.Create(ctx, &domain.Expense{
			ID:            uuid.New(),
			UserID:        u1.ID,
			HouseholdID:   &household.ID,
			Amount:        1000000.0,
			Date:          septemberDate,
			Category:      "Housing",
			Description:   "Arriendo Apto",
			Scope:         domain.ExpenseScopeHousehold,
			PaymentStatus: domain.ExpenseStatusPaid,
			CreatedAt:     time.Now(),
			UpdatedAt:     time.Now(),
		}))

		// User's 50% share of 1,000,000 = 500,000
		// Net Disposable = 4,000,000 - (300,000 + 200,000 + 500,000) = 3,000,000 COP
		summary, err := cashFlowUC.CalculateCashFlow(ctx, u1.ID, 2026, 9)
		require.NoError(t, err)
		assert.Equal(t, 4000000.0, summary.TotalIncome)
		assert.Equal(t, 300000.0, summary.PersonalExpenses)
		assert.Equal(t, 200000.0, summary.FixedLiabilities)
		assert.Equal(t, 500000.0, summary.HouseholdDebtShare)
		assert.Equal(t, 3000000.0, summary.NetDisposable)
	})

	t.Run("Update and Delete recurring debt", func(t *testing.T) {
		u := &domain.User{
			ID:           uuid.New(),
			Email:        "cf_debt_owner@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Debt Owner",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, u))

		otherUser := &domain.User{
			ID:           uuid.New(),
			Email:        "cf_debt_stranger@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Debt Stranger",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, otherUser))

		// 1. Create debt
		created, err := cashFlowUC.CreateDebt(ctx, u.ID, usecase.CreateDebtInput{
			Name:                  "Netflix",
			MonthlyAmount:         45000.0,
			DueDay:                15,
			RemainingInstallments: 0,
			Scope:                 domain.ExpenseScopePersonal,
		})
		require.NoError(t, err)
		require.NotNil(t, created)

		// 2. Unauthorized update attempt by otherUser
		_, err = cashFlowUC.UpdateDebt(ctx, otherUser.ID, created.ID, usecase.UpdateDebtInput{
			Name:          "Hacked Netflix",
			MonthlyAmount: 10000.0,
			DueDay:        1,
		})
		assert.ErrorIs(t, err, usecase.ErrUnauthorizedDebt)

		// 3. Update by owner
		updated, err := cashFlowUC.UpdateDebt(ctx, u.ID, created.ID, usecase.UpdateDebtInput{
			Name:                  "Netflix 4K Premium",
			MonthlyAmount:         55000.0,
			DueDay:                20,
			RemainingInstallments: 12,
			Scope:                 domain.ExpenseScopeHousehold,
		})
		require.NoError(t, err)
		assert.Equal(t, "Netflix 4K Premium", updated.Name)
		assert.Equal(t, 55000.0, updated.MonthlyAmount)
		assert.Equal(t, 20, updated.DueDay)
		assert.Equal(t, domain.ExpenseScopeHousehold, updated.Scope)

		// 4. Unauthorized delete attempt by otherUser
		err = cashFlowUC.DeleteDebt(ctx, otherUser.ID, created.ID)
		assert.ErrorIs(t, err, usecase.ErrUnauthorizedDebt)

		// 5. Delete by owner
		err = cashFlowUC.DeleteDebt(ctx, u.ID, created.ID)
		require.NoError(t, err)

		// 6. Delete again returns not found
		err = cashFlowUC.DeleteDebt(ctx, u.ID, created.ID)
		assert.ErrorIs(t, err, usecase.ErrDebtNotFound)

		// 7. Update deleted returns not found
		_, err = cashFlowUC.UpdateDebt(ctx, u.ID, created.ID, usecase.UpdateDebtInput{
			Name:          "Nonexistent",
			MonthlyAmount: 1000.0,
			DueDay:        1,
		})
		assert.ErrorIs(t, err, usecase.ErrDebtNotFound)
	})
}
