package usecase

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type FlexibleDate struct {
	time.Time
}

func (fd *FlexibleDate) UnmarshalJSON(b []byte) error {
	s := strings.Trim(string(b), `"`)
	if s == "" || s == "null" {
		fd.Time = time.Time{}
		return nil
	}
	formats := []string{
		"2006-01-02",
		time.RFC3339,
		"2006-01-02T15:04:05Z07:00",
		"2006-01-02T15:04:05",
		"2006-01-02 15:04:05",
	}
	for _, f := range formats {
		if t, err := time.Parse(f, s); err == nil {
			fd.Time = t.UTC()
			return nil
		}
	}
	return fmt.Errorf("formato de fecha no soportado: %s", s)
}

type SyncUseCase struct {
	expenseRepo   ports.ExpenseRepository
	incomeRepo    ports.IncomeRepository
	userRepo      ports.UserRepository
	householdRepo ports.HouseholdRepository
}

func NewSyncUseCase(
	expenseRepo ports.ExpenseRepository,
	incomeRepo ports.IncomeRepository,
	userRepo ports.UserRepository,
	householdRepo ports.HouseholdRepository,
) *SyncUseCase {
	return &SyncUseCase{
		expenseRepo:   expenseRepo,
		incomeRepo:    incomeRepo,
		userRepo:      userRepo,
		householdRepo: householdRepo,
	}
}

type SyncExpenseItem struct {
	ClientID      string                      `json:"client_id"`
	ID            string                      `json:"id"`
	Amount        float64                     `json:"amount" binding:"required,gt=0"`
	Date          FlexibleDate                `json:"date"`
	Category      string                      `json:"category" binding:"required"`
	Description   string                      `json:"description" binding:"required"`
	Scope         domain.ExpenseScope         `json:"scope"`
	PaymentStatus domain.ExpensePaymentStatus `json:"payment_status"`
}

type SyncIncomeItem struct {
	ClientID    string            `json:"client_id"`
	ID          string            `json:"id"`
	Amount      float64           `json:"amount" binding:"required,gt=0"`
	Type        domain.IncomeType `json:"type" binding:"required"`
	Date        FlexibleDate      `json:"date"`
	Description string            `json:"description"`
}

type SyncPaymentItem struct {
	ClientID       string       `json:"client_id"`
	ID             string       `json:"id"`
	ReceiverUserID uuid.UUID    `json:"receiver_user_id" binding:"required"`
	Amount         float64      `json:"amount" binding:"required,gt=0"`
	Channel        string       `json:"channel"`
	Date           FlexibleDate `json:"date"`
	Notes          string       `json:"notes"`
}

type SyncBatchInput struct {
	ClientSyncTime FlexibleDate      `json:"client_sync_time"`
	Expenses       []SyncExpenseItem `json:"expenses"`
	Incomes        []SyncIncomeItem  `json:"incomes"`
	Payments       []SyncPaymentItem `json:"payments"`
}

type SyncItemAck struct {
	ClientID string    `json:"client_id"`
	ServerID uuid.UUID `json:"server_id"`
	Status   string    `json:"status"` // "SYNCED" or "ERROR"
	Error    string    `json:"error,omitempty"`
}

type SyncBatchResult struct {
	ServerSyncTime time.Time     `json:"server_sync_time"`
	SyncedExpenses []SyncItemAck `json:"synced_expenses"`
	SyncedIncomes  []SyncItemAck `json:"synced_incomes"`
	SyncedPayments []SyncItemAck `json:"synced_payments"`
}

func (uc *SyncUseCase) SyncBatch(ctx context.Context, userID uuid.UUID, input SyncBatchInput) (*SyncBatchResult, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	result := &SyncBatchResult{
		ServerSyncTime: time.Now().UTC(),
		SyncedExpenses: make([]SyncItemAck, 0, len(input.Expenses)),
		SyncedIncomes:  make([]SyncItemAck, 0, len(input.Incomes)),
		SyncedPayments: make([]SyncItemAck, 0, len(input.Payments)),
	}

	// 1. Process Expenses
	for _, expItem := range input.Expenses {
		clientID := expItem.ClientID
		if clientID == "" {
			clientID = expItem.ID
		}
		expID, parseErr := uuid.Parse(clientID)
		if parseErr != nil {
			expID = uuid.New()
			if clientID == "" {
				clientID = expID.String()
			}
		}

		scope := expItem.Scope
		var householdID *uuid.UUID
		if user.ActiveHouseholdID == nil {
			scope = domain.ExpenseScopePersonal
		} else {
			if scope == "" {
				scope = domain.ExpenseScopeHousehold
			}
			if scope == domain.ExpenseScopeHousehold {
				householdID = user.ActiveHouseholdID
			}
		}

		date := expItem.Date.Time
		if date.IsZero() {
			date = time.Now().UTC()
		}

		paymentStatus := expItem.PaymentStatus
		if paymentStatus == "" {
			paymentStatus = domain.ExpenseStatusPaid
		}

		expense := &domain.Expense{
			ID:            expID,
			UserID:        user.ID,
			HouseholdID:   householdID,
			Amount:        expItem.Amount,
			Date:          date,
			Category:      expItem.Category,
			Description:   expItem.Description,
			Scope:         scope,
			PaymentStatus: paymentStatus,
			CreatedAt:     time.Now(),
			UpdatedAt:     time.Now(),
		}

		// Try to find if already exists (idempotency by ID)
		existing, _ := uc.expenseRepo.FindByID(ctx, expID)
		if existing == nil {
			if createErr := uc.expenseRepo.Create(ctx, expense); createErr != nil {
				result.SyncedExpenses = append(result.SyncedExpenses, SyncItemAck{
					ClientID: clientID,
					Status:   "ERROR",
					Error:    createErr.Error(),
				})
				continue
			}
		}

		result.SyncedExpenses = append(result.SyncedExpenses, SyncItemAck{
			ClientID: clientID,
			ServerID: expID,
			Status:   "SYNCED",
		})
	}

	// 2. Process Incomes
	for _, incItem := range input.Incomes {
		clientID := incItem.ClientID
		if clientID == "" {
			clientID = incItem.ID
		}
		incID, parseErr := uuid.Parse(clientID)
		if parseErr != nil {
			incID = uuid.New()
			if clientID == "" {
				clientID = incID.String()
			}
		}

		date := incItem.Date.Time
		if date.IsZero() {
			date = time.Now().UTC()
		}

		income := &domain.Income{
			ID:          incID,
			UserID:      user.ID,
			Amount:      incItem.Amount,
			Type:        incItem.Type,
			Date:        date,
			Description: incItem.Description,
			CreatedAt:   time.Now(),
		}

		existing, _ := uc.incomeRepo.FindIncomeByID(ctx, incID)
		if existing == nil {
			if createErr := uc.incomeRepo.CreateIncome(ctx, income); createErr != nil {
				result.SyncedIncomes = append(result.SyncedIncomes, SyncItemAck{
					ClientID: clientID,
					Status:   "ERROR",
					Error:    createErr.Error(),
				})
				continue
			}
		}

		result.SyncedIncomes = append(result.SyncedIncomes, SyncItemAck{
			ClientID: clientID,
			ServerID: incID,
			Status:   "SYNCED",
		})
	}

	// 3. Process Settlement Payments
	for _, payItem := range input.Payments {
		clientID := payItem.ClientID
		payID, parseErr := uuid.Parse(payItem.ID)
		if parseErr != nil {
			payID = uuid.New()
			if clientID == "" {
				clientID = payID.String()
			}
		}

		if user.ActiveHouseholdID == nil || uc.householdRepo == nil {
			result.SyncedPayments = append(result.SyncedPayments, SyncItemAck{
				ClientID: clientID,
				Status:   "ERROR",
				Error:    "usuario no tiene hogar activo para registrar pagos",
			})
			continue
		}

		householdID := *user.ActiveHouseholdID
		channel := payItem.Channel
		if channel != "Transferencia" && channel != "Efectivo" {
			channel = "Transferencia"
		}

		date := payItem.Date.Time
		if date.IsZero() {
			date = time.Now().UTC()
		}

		payment := &domain.SettlementPayment{
			ID:             payID,
			HouseholdID:    householdID,
			PayerUserID:    user.ID,
			ReceiverUserID: payItem.ReceiverUserID,
			Amount:         payItem.Amount,
			Channel:        channel,
			Date:           date,
			Notes:          payItem.Notes,
			CreatedAt:      time.Now(),
		}

		existing, _ := uc.householdRepo.FindPaymentByID(ctx, payID)
		if existing == nil {
			if err := uc.householdRepo.CreatePayment(ctx, payment); err != nil {
				result.SyncedPayments = append(result.SyncedPayments, SyncItemAck{
					ClientID: clientID,
					Status:   "ERROR",
					Error:    err.Error(),
				})
				continue
			}
		}

		result.SyncedPayments = append(result.SyncedPayments, SyncItemAck{
			ClientID: clientID,
			ServerID: payID,
			Status:   "SYNCED",
		})
	}

	return result, nil
}
