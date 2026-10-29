package usecase_test

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"house-expenses/internal/adapters/repository"
	"house-expenses/internal/adapters/statement"
	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/usecase"
)

func TestReconciliationUseCase(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	expenseRepo := repository.NewExpenseRepository(db)
	statementRepo := repository.NewBankStatementRepository(db)
	parser := statement.NewMultiFormatStatementParser()
	ctx := context.Background()

	reconUC := usecase.NewReconciliationUseCase(statementRepo, expenseRepo, userRepo, parser)

	user := &domain.User{
		ID:           uuid.New(),
		Email:        "recon_user@test.com",
		PasswordHash: strPtr("hashed"),
		Name:         "Recon User",
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}
	require.NoError(t, userRepo.Create(ctx, user))

	// Register an existing expense: $120,000 COP for "Enel Codensa Luz" on 2026-09-01
	invoiceExpense := &domain.Expense{
		ID:            uuid.New(),
		UserID:        user.ID,
		Amount:        120000.0,
		Date:          time.Date(2026, 9, 1, 10, 0, 0, 0, time.UTC),
		Category:      "Utilities",
		Description:   "Factura Enel Codensa Luz",
		Scope:         domain.ExpenseScopeHousehold,
		PaymentStatus: domain.ExpenseStatusPaid,
		CreatedAt:     time.Now(),
		UpdatedAt:     time.Now(),
	}
	require.NoError(t, expenseRepo.Create(ctx, invoiceExpense))

	t.Run("UploadStatement flags potential duplicate with confidence score >= 80%", func(t *testing.T) {
		// Bank statement containing 3 transactions:
		// 1. $120,000 on 2026-09-03 (within +2 days, "PAGO ENEL CODENSA SERVICIOS") -> MATCH
		// 2. $45,000 on 2026-09-05 ("Restaurante Wok") -> NO MATCH (amount different)
		// 3. $120,000 on 2026-09-25 ("Otra Factura") -> NO MATCH (date > 4 days)
		csvData := `Fecha,Descripcion,Valor
2026-09-03,PAGO ENEL CODENSA SERVICIOS,120000
2026-09-05,Restaurante Wok,45000
2026-09-25,Otra Factura Diferente,120000
`
		result, err := reconUC.UploadStatement(
			ctx,
			user.ID,
			"Bancolombia",
			"9821",
			strings.NewReader(csvData),
			"extracto_septiembre.csv",
		)
		require.NoError(t, err)
		assert.Equal(t, 3, result.TotalTransactions)
		assert.Equal(t, 1, result.SuspectedDuplicates)

		// Check suspected duplicate list
		dups, err := reconUC.GetSuspectedDuplicates(ctx, user.ID)
		require.NoError(t, err)
		require.Len(t, dups, 1)

		dupTx := dups[0]
		assert.Equal(t, domain.DuplicateStatusSuspected, dupTx.DuplicateStatus)
		assert.GreaterOrEqual(t, dupTx.ConfidenceScore, 80.0)
		require.NotNil(t, dupTx.MatchedExpenseID)
		assert.Equal(t, invoiceExpense.ID, *dupTx.MatchedExpenseID)

		// Test manual link
		err = reconUC.LinkTransaction(ctx, user.ID, dupTx.ID, invoiceExpense.ID)
		require.NoError(t, err)

		// After link, it is no longer in suspected duplicates
		dupsAfterLink, err := reconUC.GetSuspectedDuplicates(ctx, user.ID)
		require.NoError(t, err)
		assert.Len(t, dupsAfterLink, 0)

		// Verify transaction is CONFIRMED in DB
		updatedTx, err := statementRepo.FindTransactionByID(ctx, dupTx.ID)
		require.NoError(t, err)
		require.NotNil(t, updatedTx)
		assert.Equal(t, domain.DuplicateStatusConfirmed, updatedTx.DuplicateStatus)
		assert.Equal(t, 100.0, updatedTx.ConfidenceScore)
	})

	t.Run("DismissDuplicate marks status as DISMISSED and clears matched expense", func(t *testing.T) {
		csvData := `Fecha,Descripcion,Valor
2026-09-02,ENEL CODENSA REF 12345,120000
`
		result, err := reconUC.UploadStatement(
			ctx,
			user.ID,
			"Davivienda",
			"1234",
			strings.NewReader(csvData),
			"davivienda.csv",
		)
		require.NoError(t, err)
		require.Equal(t, 1, result.SuspectedDuplicates)

		dups, err := reconUC.GetSuspectedDuplicates(ctx, user.ID)
		require.NoError(t, err)
		require.Len(t, dups, 1)

		err = reconUC.DismissDuplicate(ctx, user.ID, dups[0].ID)
		require.NoError(t, err)

		updatedTx, err := statementRepo.FindTransactionByID(ctx, dups[0].ID)
		require.NoError(t, err)
		require.NotNil(t, updatedTx)
		assert.Equal(t, domain.DuplicateStatusDismissed, updatedTx.DuplicateStatus)
		assert.Nil(t, updatedTx.MatchedExpenseID)
	})
}
