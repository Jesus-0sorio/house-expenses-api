package usecase

import (
	"context"
	"errors"
	"fmt"
	"math"
	"strings"
	"time"

	"github.com/google/uuid"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

var (
	ErrExpenseNotFound = errors.New("expense not found")
)

type ExpenseUseCase struct {
	expenseRepo   ports.ExpenseRepository
	invoiceRepo   ports.InvoiceRepository
	userRepo      ports.UserRepository
	householdRepo ports.HouseholdRepository
}

func NewExpenseUseCase(
	expenseRepo ports.ExpenseRepository,
	invoiceRepo ports.InvoiceRepository,
	userRepo ports.UserRepository,
	householdRepo ports.HouseholdRepository,
) *ExpenseUseCase {
	return &ExpenseUseCase{
		expenseRepo:   expenseRepo,
		invoiceRepo:   invoiceRepo,
		userRepo:      userRepo,
		householdRepo: householdRepo,
	}
}

type CreateExpenseInput struct {
	Amount        float64                     `json:"amount" binding:"required,gt=0"`
	Date          time.Time                   `json:"date"`
	Category      string                      `json:"category" binding:"required"`
	Description   string                      `json:"description" binding:"required"`
	Scope         domain.ExpenseScope         `json:"scope"`
	InvoiceID     *uuid.UUID                  `json:"invoice_id"`
	PaymentStatus domain.ExpensePaymentStatus `json:"payment_status"`
}

func (uc *ExpenseUseCase) CreateExpense(ctx context.Context, userID uuid.UUID, input CreateExpenseInput) (*domain.Expense, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	scope := input.Scope
	var householdID *uuid.UUID

	// Adaptive mode check: if user has no household, always 100% personal
	if user.ActiveHouseholdID == nil {
		scope = domain.ExpenseScopePersonal
	} else {
		householdID = user.ActiveHouseholdID
		if scope == "" {
			// Check merchant rule
			rule, _ := uc.expenseRepo.FindClassificationRule(ctx, userID, input.Description)
			if rule != nil {
				scope = rule.DefaultScope
			} else {
				scope = domain.ExpenseScopeHousehold
			}
		}
	}

	date := input.Date
	if date.IsZero() {
		date = time.Now()
	}

	paymentStatus := input.PaymentStatus
	if paymentStatus == "" {
		paymentStatus = domain.ExpenseStatusPaid
	}

	expense := &domain.Expense{
		ID:            uuid.New(),
		UserID:        user.ID,
		HouseholdID:   householdID,
		InvoiceID:     input.InvoiceID,
		Amount:        input.Amount,
		Date:          date,
		Category:      input.Category,
		Description:   input.Description,
		Scope:         scope,
		PaymentStatus: paymentStatus,
		CreatedAt:     time.Now(),
		UpdatedAt:     time.Now(),
	}

	if err := uc.expenseRepo.Create(ctx, expense); err != nil {
		return nil, fmt.Errorf("failed to create expense: %w", err)
	}

	return expense, nil
}

type InverseSplitInput struct {
	InvoiceID     uuid.UUID                 `json:"invoice_id" binding:"required"`
	SelectedItems []uuid.UUID               `json:"selected_items"`
	SelectedAs    domain.ItemClassification `json:"selected_as" binding:"required"` // PERSONAL or HOUSEHOLD
}

type SplitResult struct {
	InvoiceID      uuid.UUID            `json:"invoice_id"`
	Items          []domain.InvoiceItem `json:"items"`
	PersonalTotal  float64              `json:"personal_total"`
	HouseholdTotal float64              `json:"household_total"`
}

func (uc *ExpenseUseCase) SplitItemsInversely(ctx context.Context, userID uuid.UUID, input InverseSplitInput) (*SplitResult, error) {
	invoice, err := uc.invoiceRepo.FindByID(ctx, input.InvoiceID)
	if err != nil {
		return nil, err
	}
	if invoice == nil {
		return nil, ErrInvoiceNotFound
	}

	opposite := domain.ClassificationHousehold
	if input.SelectedAs == domain.ClassificationHousehold {
		opposite = domain.ClassificationPersonal
	}

	selectedSet := make(map[uuid.UUID]bool)
	for _, id := range input.SelectedItems {
		selectedSet[id] = true
	}

	var personalTotal, householdTotal float64
	for i := range invoice.Items {
		if selectedSet[invoice.Items[i].ID] {
			invoice.Items[i].Classification = input.SelectedAs
		} else {
			invoice.Items[i].Classification = opposite
		}

		if invoice.Items[i].Classification == domain.ClassificationPersonal {
			personalTotal += invoice.Items[i].TotalPrice
		} else {
			householdTotal += invoice.Items[i].TotalPrice
		}
	}

	if err := uc.invoiceRepo.UpdateItems(ctx, invoice.Items); err != nil {
		return nil, fmt.Errorf("failed to update item classifications: %w", err)
	}

	return &SplitResult{
		InvoiceID:      invoice.ID,
		Items:          invoice.Items,
		PersonalTotal:  personalTotal,
		HouseholdTotal: householdTotal,
	}, nil
}

type SaveRuleInput struct {
	MerchantPattern string                 `json:"merchant_pattern" binding:"required"`
	DefaultScope    domain.ExpenseScope    `json:"default_scope" binding:"required"`
	AutoSplitPolicy domain.AutoSplitPolicy `json:"auto_split_policy"`
}

func (uc *ExpenseUseCase) SaveClassificationRule(ctx context.Context, userID uuid.UUID, input SaveRuleInput) (*domain.ClassificationRule, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	policy := input.AutoSplitPolicy
	if policy == "" {
		policy = domain.AutoSplitPrompt
	}

	rule := &domain.ClassificationRule{
		ID:              uuid.New(),
		UserID:          userID,
		HouseholdID:     user.ActiveHouseholdID,
		MerchantPattern: input.MerchantPattern,
		DefaultScope:    input.DefaultScope,
		AutoSplitPolicy: policy,
		CreatedAt:       time.Now(),
	}

	if err := uc.expenseRepo.SaveClassificationRule(ctx, rule); err != nil {
		return nil, err
	}

	return rule, nil
}

type RoomieBalance struct {
	UserID          uuid.UUID `json:"user_id"`
	UserName        string    `json:"user_name"`
	SplitPercentage float64   `json:"split_percentage"`
	TotalPaid       float64   `json:"total_paid"`
	ExpectedShare   float64   `json:"expected_share"`
	NetBalance      float64   `json:"net_balance"` // Positive = should receive, Negative = owes
}

type SettlementSummary struct {
	HouseholdID    uuid.UUID       `json:"household_id"`
	CycleStart     string          `json:"cycle_start,omitempty"`
	CycleEnd       string          `json:"cycle_end,omitempty"`
	TotalHousehold float64         `json:"total_household"`
	Balances       []RoomieBalance `json:"balances"`
	Settlement     string          `json:"settlement"` // Human-readable explanation in Spanish
}

func (uc *ExpenseUseCase) CalculateHouseholdBalance(ctx context.Context, userID uuid.UUID) (*SettlementSummary, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil || user.ActiveHouseholdID == nil {
		return nil, ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	household, err := uc.householdRepo.FindByID(ctx, householdID)
	if err != nil || household == nil {
		return nil, ErrHouseholdNotFound
	}

	members, err := uc.householdRepo.GetMembers(ctx, householdID)
	if err != nil {
		return nil, err
	}

	cycleDay := household.BillingCycleDay
	if cycleDay < 1 {
		cycleDay = 1
	}
	start, end := CalculateCycleBounds(cycleDay, time.Now().UTC())

	expenses, err := uc.expenseRepo.FindByHouseholdAndPeriod(ctx, householdID, start, end)
	if err != nil {
		return nil, err
	}

	paidByUser := make(map[uuid.UUID]float64)
	var totalHousehold float64
	for _, exp := range expenses {
		if exp.Scope == domain.ExpenseScopeHousehold {
			paidByUser[exp.UserID] += exp.Amount
			totalHousehold += exp.Amount
		}
	}

	payments, _ := uc.householdRepo.GetPaymentsByHouseholdAndPeriod(ctx, householdID, start, end)
	paidToOthers := make(map[uuid.UUID]float64)
	receivedFromOthers := make(map[uuid.UUID]float64)
	for _, p := range payments {
		paidToOthers[p.PayerUserID] += p.Amount
		receivedFromOthers[p.ReceiverUserID] += p.Amount
	}

	var balances []RoomieBalance
	memberCount := len(members)
	if memberCount == 0 {
		memberCount = 1
	}

	// Rollover de deudas pendientes del ciclo inmediato anterior
	prevRefDate := time.Now().UTC().AddDate(0, -1, 0)
	prevStart, prevEnd := CalculateCycleBounds(cycleDay, prevRefDate)
	prevExpenses, _ := uc.expenseRepo.FindByHouseholdAndPeriod(ctx, householdID, prevStart, prevEnd)

	prevPaidByUser := make(map[uuid.UUID]float64)
	var prevTotalHousehold float64
	for _, exp := range prevExpenses {
		if exp.Scope == domain.ExpenseScopeHousehold {
			prevPaidByUser[exp.UserID] += exp.Amount
			prevTotalHousehold += exp.Amount
		}
	}

	prevPayments, _ := uc.householdRepo.GetPaymentsByHouseholdAndPeriod(ctx, householdID, prevStart, prevEnd)
	prevPaidToOthers := make(map[uuid.UUID]float64)
	prevReceivedFromOthers := make(map[uuid.UUID]float64)
	for _, p := range prevPayments {
		prevPaidToOthers[p.PayerUserID] += p.Amount
		prevReceivedFromOthers[p.ReceiverUserID] += p.Amount
	}

	carryoverNet := make(map[uuid.UUID]float64)
	if prevTotalHousehold > 0 || len(prevPayments) > 0 {
		for _, m := range members {
			pct := m.SplitPercentage
			if pct <= 0 {
				pct = 100.0 / float64(memberCount)
			}
			prevExpected := prevTotalHousehold * (pct / 100.0)
			carryoverNet[m.UserID] = (prevPaidByUser[m.UserID] - prevExpected) + prevPaidToOthers[m.UserID] - prevReceivedFromOthers[m.UserID]
		}
	}

	for _, m := range members {
		splitPct := m.SplitPercentage
		if splitPct <= 0 {
			splitPct = 100.0 / float64(memberCount)
		}
		expectedShare := totalHousehold * (splitPct / 100.0)
		paid := paidByUser[m.UserID]
		net := (paid - expectedShare) + paidToOthers[m.UserID] - receivedFromOthers[m.UserID] + carryoverNet[m.UserID]

		name := "Roomie"
		if m.User != nil && m.User.Name != "" {
			name = m.User.Name
		}
		balances = append(balances, RoomieBalance{
			UserID:          m.UserID,
			UserName:        name,
			SplitPercentage: splitPct,
			TotalPaid:       paid,
			ExpectedShare:   expectedShare,
			NetBalance:      net,
		})
	}

	settlementText := "Las cuentas están equilibradas."
	if len(balances) == 2 {
		r1 := balances[0]
		r2 := balances[1]
		if r1.NetBalance < -0.01 {
			settlementText = fmt.Sprintf("%s le debe $%.0f COP a %s.", r1.UserName, math.Abs(r1.NetBalance), r2.UserName)
		} else if r2.NetBalance < -0.01 {
			settlementText = fmt.Sprintf("%s le debe $%.0f COP a %s.", r2.UserName, math.Abs(r2.NetBalance), r1.UserName)
		}
	} else if len(balances) > 2 {
		type debtorNode struct {
			name   string
			amount float64
		}
		var debtors []debtorNode
		var creditors []debtorNode

		for _, b := range balances {
			if b.NetBalance < -0.01 {
				debtors = append(debtors, debtorNode{name: b.UserName, amount: math.Abs(b.NetBalance)})
			} else if b.NetBalance > 0.01 {
				creditors = append(creditors, debtorNode{name: b.UserName, amount: b.NetBalance})
			}
		}

		var settlements []string
		dIdx, cIdx := 0, 0
		for dIdx < len(debtors) && cIdx < len(creditors) {
			transfer := math.Min(debtors[dIdx].amount, creditors[cIdx].amount)
			if transfer > 0.01 {
				settlements = append(settlements, fmt.Sprintf("%s le debe $%.0f COP a %s", debtors[dIdx].name, transfer, creditors[cIdx].name))
			}
			debtors[dIdx].amount -= transfer
			creditors[cIdx].amount -= transfer

			if debtors[dIdx].amount <= 0.01 {
				dIdx++
			}
			if creditors[cIdx].amount <= 0.01 {
				cIdx++
			}
		}

		if len(settlements) > 0 {
			settlementText = strings.Join(settlements, "; ") + "."
		}
	}

	return &SettlementSummary{
		HouseholdID:    householdID,
		CycleStart:     start.Format("2006-01-02"),
		CycleEnd:       end.Format("2006-01-02"),
		TotalHousehold: totalHousehold,
		Balances:       balances,
		Settlement:     settlementText,
	}, nil
}

func (uc *ExpenseUseCase) ListExpenses(ctx context.Context, userID uuid.UUID, limit, offset int) ([]domain.Expense, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	var householdID *uuid.UUID
	if user != nil {
		householdID = user.ActiveHouseholdID
	}
	return uc.expenseRepo.FindByUserOrHousehold(ctx, userID, householdID, limit, offset)
}
