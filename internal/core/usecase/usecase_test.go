package usecase_test

import (
	"context"
	"testing"
	"time"

	"github.com/glebarez/sqlite"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"

	"house-expenses/internal/adapters/oauth"
	"house-expenses/internal/adapters/repository"
	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/usecase"
)

func setupTestDB(t *testing.T) *gorm.DB {
	db, err := gorm.Open(sqlite.Open(":memory:"), &gorm.Config{})
	require.NoError(t, err)
	err = repository.Migrate(db)
	require.NoError(t, err)
	return db
}

func TestAuthUseCase(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	oauthReg := oauth.NewProviderRegistry()
	authUC := usecase.NewAuthUseCase(userRepo, oauthReg, "test_secret_1234567890")
	ctx := context.Background()

	t.Run("Register success", func(t *testing.T) {
		resp, err := authUC.Register(ctx, usecase.RegisterInput{
			Email:    "test@example.com",
			Password: "password123",
			Name:     "Test User",
		})
		require.NoError(t, err)
		assert.NotEmpty(t, resp.Token)
		assert.Equal(t, "test@example.com", resp.User.Email)
		assert.Equal(t, "Test User", resp.User.Name)
	})

	t.Run("Register duplicate email fails", func(t *testing.T) {
		_, err := authUC.Register(ctx, usecase.RegisterInput{
			Email:    "test@example.com",
			Password: "password456",
			Name:     "Duplicate",
		})
		assert.ErrorIs(t, err, usecase.ErrUserAlreadyExists)
	})

	t.Run("Login success", func(t *testing.T) {
		resp, err := authUC.Login(ctx, usecase.LoginInput{
			Email:    "test@example.com",
			Password: "password123",
		})
		require.NoError(t, err)
		assert.NotEmpty(t, resp.Token)

		// Validate JWT token
		claims, err := authUC.ValidateJWT(resp.Token)
		require.NoError(t, err)
		assert.Equal(t, "test@example.com", claims.Email)
	})

	t.Run("Login invalid password fails", func(t *testing.T) {
		_, err := authUC.Login(ctx, usecase.LoginInput{
			Email:    "test@example.com",
			Password: "wrongpassword",
		})
		assert.ErrorIs(t, err, usecase.ErrInvalidCredentials)
	})
}

func TestHouseholdAdaptiveMode(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	householdRepo := repository.NewHouseholdRepository(db)
	oauthReg := oauth.NewProviderRegistry()
	authUC := usecase.NewAuthUseCase(userRepo, oauthReg, "test_secret_1234567890")
	expenseRepo := repository.NewExpenseRepository(db)
	expenseUC := usecase.NewExpenseUseCase(expenseRepo, nil, userRepo, householdRepo)
	householdUC := usecase.NewHouseholdUseCase(householdRepo, userRepo, expenseRepo)
	ctx := context.Background()

	// Register User 1 and User 2
	u1Resp, err := authUC.Register(ctx, usecase.RegisterInput{
		Email:    "roomie1@example.com",
		Password: "password123",
		Name:     "Roomie One",
	})
	require.NoError(t, err)

	u2Resp, err := authUC.Register(ctx, usecase.RegisterInput{
		Email:    "roomie2@example.com",
		Password: "password123",
		Name:     "Roomie Two",
	})
	require.NoError(t, err)

	t.Run("Solitary mode before household creation", func(t *testing.T) {
		mode, err := householdUC.GetAdaptiveMode(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, "SOLITARY", mode.Mode)
		assert.Equal(t, 100.0, mode.DefaultSplitPct)
	})

	var inviteCode string
	t.Run("Create household stays solitary until 2nd member joins", func(t *testing.T) {
		h, err := householdUC.CreateHousehold(ctx, u1Resp.User.ID, usecase.CreateHouseholdInput{
			Name: "Apartamento 402",
		})
		require.NoError(t, err)
		assert.NotEmpty(t, h.InviteCode)
		inviteCode = h.InviteCode

		mode, err := householdUC.GetAdaptiveMode(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, "SOLITARY", mode.Mode)
	})

	t.Run("Switches to HOUSEHOLD mode when 2nd roomie joins", func(t *testing.T) {
		_, err := householdUC.JoinHousehold(ctx, u2Resp.User.ID, usecase.JoinHouseholdInput{
			InviteCode: inviteCode,
		})
		require.NoError(t, err)

		mode1, err := householdUC.GetAdaptiveMode(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, "HOUSEHOLD", mode1.Mode)
		assert.Equal(t, 50.0, mode1.DefaultSplitPct)
		assert.Equal(t, 2, mode1.MembersCount)

		mode2, err := householdUC.GetAdaptiveMode(ctx, u2Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, "HOUSEHOLD", mode2.Mode)
		assert.Equal(t, 50.0, mode2.DefaultSplitPct)
	})

	t.Run("Cross-roomie household expense visibility", func(t *testing.T) {
		// Roomie 1 creates a shared household expense
		exp1, err := expenseUC.CreateExpense(ctx, u1Resp.User.ID, usecase.CreateExpenseInput{
			Amount:      230000,
			Category:    "Servicios",
			Description: "Factura de Luz y Gas",
			Scope:       domain.ExpenseScopeHousehold,
		})
		require.NoError(t, err)
		assert.Equal(t, domain.ExpenseScopeHousehold, exp1.Scope)

		// Roomie 1 creates a personal expense
		_, err = expenseUC.CreateExpense(ctx, u1Resp.User.ID, usecase.CreateExpenseInput{
			Amount:      50000,
			Category:    "Ocio",
			Description: "Cine Personal",
			Scope:       domain.ExpenseScopePersonal,
		})
		require.NoError(t, err)

		// Roomie 2 lists expenses: should see Roomie 1's household expense, but NOT Roomie 1's personal expense
		r2Expenses, err := expenseUC.ListExpenses(ctx, u2Resp.User.ID, 50, 0)
		require.NoError(t, err)

		var foundShared, foundPersonal bool
		for _, e := range r2Expenses {
			if e.Description == "Factura de Luz y Gas" {
				foundShared = true
			}
			if e.Description == "Cine Personal" {
				foundPersonal = true
			}
		}
		assert.True(t, foundShared, "Roomie 2 should see Roomie 1's household expense")
		assert.False(t, foundPersonal, "Roomie 2 should NOT see Roomie 1's personal expense")
	})

	t.Run("Auto-rebalance on third member join and ejection by admin", func(t *testing.T) {
		// Register User 3
		u3Resp, err := authUC.Register(ctx, usecase.RegisterInput{
			Email:    "roomie3@example.com",
			Password: "password123",
			Name:     "Roomie Three",
		})
		require.NoError(t, err)

		// User 3 joins household
		_, err = householdUC.JoinHousehold(ctx, u3Resp.User.ID, usecase.JoinHouseholdInput{
			InviteCode: inviteCode,
		})
		require.NoError(t, err)

		// Members should now be 3, with sum of splits == 100.0
		members, err := householdUC.GetMembers(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, 3, len(members))

		var totalPct float64
		for _, m := range members {
			totalPct += m.SplitPercentage
		}
		assert.InDelta(t, 100.0, totalPct, 0.01, "Splits must sum to 100%")

		// Test Cycle Detail endpoint
		detail, err := householdUC.GetCycleDetail(ctx, u1Resp.User.ID, 0)
		require.NoError(t, err)
		assert.Equal(t, 0, detail.CycleIndex)
		assert.Greater(t, detail.TotalAmount, 0.0)
		assert.NotEmpty(t, detail.Expenses)
		assert.Equal(t, 3, len(detail.Balances))

		// Non-admin (User 3) cannot remove anyone
		err = householdUC.RemoveMember(ctx, u3Resp.User.ID, u2Resp.User.ID)
		assert.Error(t, err)

		// Admin (User 1) removes User 3
		err = householdUC.RemoveMember(ctx, u1Resp.User.ID, u3Resp.User.ID)
		require.NoError(t, err)

		// User 3 should now have no active household
		u3Updated, err := userRepo.FindByID(ctx, u3Resp.User.ID)
		require.NoError(t, err)
		assert.Nil(t, u3Updated.ActiveHouseholdID)

		// Remaining members should be 2, auto-rebalanced to 50% each
		remainingMembers, err := householdUC.GetMembers(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, 2, len(remainingMembers))
		assert.Equal(t, 50.0, remainingMembers[0].SplitPercentage)
		assert.Equal(t, 50.0, remainingMembers[1].SplitPercentage)
	})

	t.Run("Unsupported OAuth provider fails gracefully", func(t *testing.T) {
		_, err := authUC.GetOAuthConsentURL(domain.AuthProviderMicrosoft, "state123")
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "not registered or configured")
	})

	t.Run("Settlement payment balances out debt between roomies without inflating household expenses", func(t *testing.T) {
		// User 2 pays $30,000 COP to User 1 to balance debt
		payment, err := householdUC.CreateSettlementPayment(ctx, u2Resp.User.ID, usecase.CreateSettlementPaymentInput{
			ReceiverUserID: u1Resp.User.ID,
			Amount:         30000.0,
			Channel:        "Transferencia",
			Date:           time.Now().UTC(),
			Notes:          "Liquidación ciclo",
		})
		require.NoError(t, err)
		assert.Equal(t, 30000.0, payment.Amount)
		assert.Equal(t, u2Resp.User.ID, payment.PayerUserID)
		assert.Equal(t, u1Resp.User.ID, payment.ReceiverUserID)

		// Check payments list
		payments, err := householdUC.GetSettlementPayments(ctx, u1Resp.User.ID, 10, 0)
		require.NoError(t, err)
		assert.NotEmpty(t, payments)

		// Calculate household balance: total expenses shouldn't increase, balances should reflect payment
		balance, err := expenseUC.CalculateHouseholdBalance(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, 230000.0, balance.TotalHousehold, "Household total expenses must NOT be inflated by settlement payment")

		// Verify GetCurrentHousehold
		currH, err := householdUC.GetCurrentHousehold(ctx, u1Resp.User.ID)
		require.NoError(t, err)
		assert.Equal(t, "Apartamento 402", currH.Name)
		assert.Equal(t, "ADMIN", currH.Role)
		assert.NotEmpty(t, currH.InviteCode)
		assert.Equal(t, 2, len(currH.Members))
	})
}

