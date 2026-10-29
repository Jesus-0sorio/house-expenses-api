package usecase_test

import (
	"context"
	"encoding/json"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"house-expenses/internal/adapters/repository"
	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/usecase"
)

func TestSyncUseCase(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	expenseRepo := repository.NewExpenseRepository(db)
	incomeRepo := repository.NewIncomeRepository(db)
	householdRepo := repository.NewHouseholdRepository(db)
	ctx := context.Background()

	syncUC := usecase.NewSyncUseCase(expenseRepo, incomeRepo, userRepo, householdRepo)

	user := &domain.User{
		ID:           uuid.New(),
		Email:        "sync_user@test.com",
		PasswordHash: strPtr("hashed"),
		Name:         "Sync User",
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}
	require.NoError(t, userRepo.Create(ctx, user))

	t.Run("SyncBatch processes offline expenses and incomes", func(t *testing.T) {
		expID1 := uuid.New().String()
		expID2 := uuid.New().String()
		incID1 := uuid.New().String()

		batch := usecase.SyncBatchInput{
			ClientSyncTime: usecase.FlexibleDate{Time: time.Now()},
			Expenses: []usecase.SyncExpenseItem{
				{
					ClientID:    expID1,
					Amount:      25000,
					Category:    "Food",
					Description: "Almuerzo Corriente",
				},
				{
					ClientID:    expID2,
					Amount:      15000,
					Category:    "Transport",
					Description: "Pasajes Transmilenio",
				},
			},
			Incomes: []usecase.SyncIncomeItem{
				{
					ClientID:    incID1,
					Amount:      1200000,
					Type:        domain.IncomeTypeFreelance,
					Description: "Diseño Web Freelance",
				},
			},
		}

		res, err := syncUC.SyncBatch(ctx, user.ID, batch)
		require.NoError(t, err)
		require.Len(t, res.SyncedExpenses, 2)
		require.Len(t, res.SyncedIncomes, 1)

		assert.Equal(t, expID1, res.SyncedExpenses[0].ClientID)
		assert.Equal(t, "SYNCED", res.SyncedExpenses[0].Status)
		assert.Equal(t, expID2, res.SyncedExpenses[1].ClientID)
		assert.Equal(t, "SYNCED", res.SyncedExpenses[1].Status)

		assert.Equal(t, incID1, res.SyncedIncomes[0].ClientID)
		assert.Equal(t, "SYNCED", res.SyncedIncomes[0].Status)

		// Verify persisted in DB
		savedExpenses, err := expenseRepo.FindByUser(ctx, user.ID, 10, 0)
		require.NoError(t, err)
		assert.Len(t, savedExpenses, 2)

		savedIncomes, err := incomeRepo.ListIncomesByUser(ctx, user.ID)
		require.NoError(t, err)
		assert.Len(t, savedIncomes, 1)

		// Re-sending same batch must be idempotent and succeed without duplicates
		res2, err := syncUC.SyncBatch(ctx, user.ID, batch)
		require.NoError(t, err)
		assert.Len(t, res2.SyncedExpenses, 2)
		assert.Equal(t, "SYNCED", res2.SyncedExpenses[0].Status)
	})

	t.Run("SyncBatch processes items with ID field and household assignment", func(t *testing.T) {
		householdID := uuid.New()
		userWithHousehold := &domain.User{
			ID:                uuid.New(),
			Email:             "sync_household@test.com",
			PasswordHash:      strPtr("hashed"),
			Name:              "Household Roomie",
			ActiveHouseholdID: &householdID,
			CreatedAt:         time.Now(),
			UpdatedAt:         time.Now(),
		}
		require.NoError(t, userRepo.Create(ctx, userWithHousehold))

		expID := uuid.New().String()
		batch := usecase.SyncBatchInput{
			ClientSyncTime: usecase.FlexibleDate{Time: time.Now()},
			Expenses: []usecase.SyncExpenseItem{
				{
					ID:          expID, // mobile sends 'id' instead of 'client_id'
					Amount:      55000,
					Category:    "Supermercado",
					Description: "Mercado D1 Compartido",
					Scope:       domain.ExpenseScopeHousehold,
				},
			},
		}

		res, err := syncUC.SyncBatch(ctx, userWithHousehold.ID, batch)
		require.NoError(t, err)
		require.Len(t, res.SyncedExpenses, 1)
		assert.Equal(t, expID, res.SyncedExpenses[0].ClientID)
		assert.Equal(t, "SYNCED", res.SyncedExpenses[0].Status)

		parsedUUID, _ := uuid.Parse(expID)
		persisted, err := expenseRepo.FindByID(ctx, parsedUUID)
		require.NoError(t, err)
		require.NotNil(t, persisted)
		assert.Equal(t, domain.ExpenseScopeHousehold, persisted.Scope)
		require.NotNil(t, persisted.HouseholdID)
		assert.Equal(t, householdID, *persisted.HouseholdID)
	})

	t.Run("SyncBatch unmarshals JSON with yyyy-MM-dd date and persists to database", func(t *testing.T) {
		jsonPayload := `{
			"client_sync_time": "2026-09-05",
			"expenses": [
				{
					"id": "` + uuid.New().String() + `",
					"amount": 75000,
					"date": "2026-09-05",
					"category": "Servicios",
					"description": "Recibo de Luz",
					"scope": "HOUSEHOLD"
				}
			],
			"incomes": [
				{
					"id": "` + uuid.New().String() + `",
					"amount": 500000,
					"date": "2026-09-05",
					"type": "SALARY",
					"description": "Quincena"
				}
			]
		}`

		var batch usecase.SyncBatchInput
		err := json.Unmarshal([]byte(jsonPayload), &batch)
		require.NoError(t, err)
		require.Len(t, batch.Expenses, 1)
		require.Len(t, batch.Incomes, 1)

		res, err := syncUC.SyncBatch(ctx, user.ID, batch)
		require.NoError(t, err)
		require.Len(t, res.SyncedExpenses, 1)
		require.Len(t, res.SyncedIncomes, 1)
		assert.Equal(t, "SYNCED", res.SyncedExpenses[0].Status)
		assert.Equal(t, "SYNCED", res.SyncedIncomes[0].Status)
	})
}
