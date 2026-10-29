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

func strPtr(s string) *string {
	return &s
}

func TestExpenseUseCase(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	householdRepo := repository.NewHouseholdRepository(db)
	invoiceRepo := repository.NewInvoiceRepository(db)
	expenseRepo := repository.NewExpenseRepository(db)
	ctx := context.Background()

	expenseUC := usecase.NewExpenseUseCase(expenseRepo, invoiceRepo, userRepo, householdRepo)

	t.Run("CreateExpense - Solitary user creates expense gets PERSONAL scope", func(t *testing.T) {
		user := &domain.User{
			ID:           uuid.New(),
			Email:        "solitary@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Solitary User",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, user))

		expense, err := expenseUC.CreateExpense(ctx, user.ID, usecase.CreateExpenseInput{
			Amount:      45000,
			Category:    "Groceries",
			Description: "Supermarket run",
			Scope:       domain.ExpenseScopeHousehold, // Requested household, but user is solitary!
		})
		require.NoError(t, err)
		assert.Equal(t, domain.ExpenseScopePersonal, expense.Scope)
		assert.Nil(t, expense.HouseholdID)
	})

	t.Run("CreateExpense - Household member creates expense with rule matching", func(t *testing.T) {
		user := &domain.User{
			ID:           uuid.New(),
			Email:        "roomie1@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Roomie One",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, user))

		household := &domain.Household{
			ID:         uuid.New(),
			Name:       "Apt 302",
			InviteCode: "APT302",
			CreatedAt:  time.Now(),
			UpdatedAt:  time.Now(),
		}
		require.NoError(t, householdRepo.Create(ctx, household))
		require.NoError(t, householdRepo.AddMember(ctx, &domain.HouseholdMember{
			ID:              uuid.New(),
			HouseholdID:     household.ID,
			UserID:          user.ID,
			SplitPercentage: 50.0,
			JoinedAt:        time.Now(),
		}))

		user.ActiveHouseholdID = &household.ID
		require.NoError(t, userRepo.Update(ctx, user))

		// Save a rule: "Spotify" -> PERSONAL
		_, err := expenseUC.SaveClassificationRule(ctx, user.ID, usecase.SaveRuleInput{
			MerchantPattern: "Spotify",
			DefaultScope:    domain.ExpenseScopePersonal,
		})
		require.NoError(t, err)

		// Create expense with description "Spotify Subscription" without specifying scope
		expense, err := expenseUC.CreateExpense(ctx, user.ID, usecase.CreateExpenseInput{
			Amount:      18900,
			Category:    "Entertainment",
			Description: "Spotify Subscription",
		})
		require.NoError(t, err)
		assert.Equal(t, domain.ExpenseScopePersonal, expense.Scope)
		assert.Equal(t, household.ID, *expense.HouseholdID)

		// Create another expense with unmatched description -> defaults to HOUSEHOLD
		expense2, err := expenseUC.CreateExpense(ctx, user.ID, usecase.CreateExpenseInput{
			Amount:      75000,
			Category:    "Utilities",
			Description: "Enel Codensa Luz",
		})
		require.NoError(t, err)
		assert.Equal(t, domain.ExpenseScopeHousehold, expense2.Scope)
	})

	t.Run("SplitItemsInversely - Supermarket receipt inverse split", func(t *testing.T) {
		user := &domain.User{
			ID:           uuid.New(),
			Email:        "shopper@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Shopper",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, user))

		item1ID := uuid.New()
		item2ID := uuid.New()
		item3ID := uuid.New()

		invoice := &domain.Invoice{
			ID:            uuid.New(),
			IssuerName:    "Exito Calle 80",
			IssuerNIT:     "890900608-9",
			InvoiceNumber: "SETP-5544",
			TotalAmount:   100000,
			IssueDate:     time.Now(),
			SourceType:    domain.InvoiceSourceManual,
			CreatedAt:     time.Now(),
			Items: []domain.InvoiceItem{
				{
					ID:             item1ID,
					Description:    "Carne de Res 1kg",
					Quantity:       1,
					UnitPrice:      50000,
					TotalPrice:     50000,
					Classification: domain.ClassificationHousehold,
				},
				{
					ID:             item2ID,
					Description:    "Detergente Ariel 2kg",
					Quantity:       1,
					UnitPrice:      30000,
					TotalPrice:     30000,
					Classification: domain.ClassificationHousehold,
				},
				{
					ID:             item3ID,
					Description:    "Chocolates Ferrero",
					Quantity:       1,
					UnitPrice:      20000,
					TotalPrice:     20000,
					Classification: domain.ClassificationHousehold,
				},
			},
		}
		require.NoError(t, invoiceRepo.Create(ctx, invoice))

		// User only selects the Chocolates (item3) as PERSONAL
		// Inverse splitting must classify Chocolates as PERSONAL and all other items as HOUSEHOLD
		result, err := expenseUC.SplitItemsInversely(ctx, user.ID, usecase.InverseSplitInput{
			InvoiceID:     invoice.ID,
			SelectedItems: []uuid.UUID{item3ID},
			SelectedAs:    domain.ClassificationPersonal,
		})
		require.NoError(t, err)
		assert.Equal(t, 20000.0, result.PersonalTotal)
		assert.Equal(t, 80000.0, result.HouseholdTotal)

		// Verify persisted state
		updatedInvoice, err := invoiceRepo.FindByID(ctx, invoice.ID)
		require.NoError(t, err)
		for _, item := range updatedInvoice.Items {
			if item.ID == item3ID {
				assert.Equal(t, domain.ClassificationPersonal, item.Classification)
			} else {
				assert.Equal(t, domain.ClassificationHousehold, item.Classification)
			}
		}
	})

	t.Run("CalculateHouseholdBalance - 50/50 balance and settlement text", func(t *testing.T) {
		u1 := &domain.User{
			ID:           uuid.New(),
			Email:        "alice@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Alice",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		u2 := &domain.User{
			ID:           uuid.New(),
			Email:        "bob@test.com",
			PasswordHash: strPtr("hashed"),
			Name:         "Bob",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, u1))
		require.NoError(t, userRepo.Create(ctx, u2))

		household := &domain.Household{
			ID:         uuid.New(),
			Name:       "Shared Flat",
			InviteCode: "FLAT123",
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
		require.NoError(t, householdRepo.AddMember(ctx, &domain.HouseholdMember{
			ID:              uuid.New(),
			HouseholdID:     household.ID,
			UserID:          u2.ID,
			SplitPercentage: 50.0,
			JoinedAt:        time.Now(),
			User:            u2,
		}))

		u1.ActiveHouseholdID = &household.ID
		u2.ActiveHouseholdID = &household.ID
		require.NoError(t, userRepo.Update(ctx, u1))
		require.NoError(t, userRepo.Update(ctx, u2))

		// Alice pays 100,000 for Groceries (HOUSEHOLD)
		_, err := expenseUC.CreateExpense(ctx, u1.ID, usecase.CreateExpenseInput{
			Amount:      100000,
			Category:    "Groceries",
			Description: "D1 Supermarket",
			Scope:       domain.ExpenseScopeHousehold,
		})
		require.NoError(t, err)

		// Bob pays 40,000 for Internet (HOUSEHOLD)
		_, err = expenseUC.CreateExpense(ctx, u2.ID, usecase.CreateExpenseInput{
			Amount:      40000,
			Category:    "Utilities",
			Description: "Claro Fibra",
			Scope:       domain.ExpenseScopeHousehold,
		})
		require.NoError(t, err)

		// Alice also pays 25,000 for Personal Makeup (PERSONAL - should NOT affect household balance)
		_, err = expenseUC.CreateExpense(ctx, u1.ID, usecase.CreateExpenseInput{
			Amount:      25000,
			Category:    "Personal",
			Description: "Farmatodo Makeup",
			Scope:       domain.ExpenseScopePersonal,
		})
		require.NoError(t, err)

		summary, err := expenseUC.CalculateHouseholdBalance(ctx, u1.ID)
		require.NoError(t, err)

		assert.Equal(t, 140000.0, summary.TotalHousehold)
		require.Len(t, summary.Balances, 2)

		var aliceBal, bobBal *usecase.RoomieBalance
		for i := range summary.Balances {
			if summary.Balances[i].UserID == u1.ID {
				aliceBal = &summary.Balances[i]
			} else if summary.Balances[i].UserID == u2.ID {
				bobBal = &summary.Balances[i]
			}
		}

		require.NotNil(t, aliceBal)
		require.NotNil(t, bobBal)

		assert.Equal(t, 100000.0, aliceBal.TotalPaid)
		assert.Equal(t, 70000.0, aliceBal.ExpectedShare)
		assert.Equal(t, 30000.0, aliceBal.NetBalance) // Alice is owed 30,000

		assert.Equal(t, 40000.0, bobBal.TotalPaid)
		assert.Equal(t, 70000.0, bobBal.ExpectedShare)
		assert.Equal(t, -30000.0, bobBal.NetBalance) // Bob owes 30,000

		assert.Contains(t, summary.Settlement, "Bob le debe $30000 COP a Alice.")
	})
}
