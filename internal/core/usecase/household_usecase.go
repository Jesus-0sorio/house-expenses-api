package usecase

import (
	"context"
	"crypto/rand"
	"encoding/hex"
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
	ErrHouseholdNotFound      = errors.New("household not found")
	ErrAlreadyHouseholdMember = errors.New("user is already a member of this household")
	ErrNoActiveHousehold      = errors.New("user has no active household")
	ErrUnauthorizedHousehold  = errors.New("solo el administrador o creador del hogar puede realizar esta acción")
	ErrInvalidSplitSum        = errors.New("la suma de los porcentajes de repartición debe ser exactamente 100%")
)

type HouseholdUseCase struct {
	householdRepo ports.HouseholdRepository
	userRepo      ports.UserRepository
	expenseRepo   ports.ExpenseRepository
}

func NewHouseholdUseCase(
	householdRepo ports.HouseholdRepository,
	userRepo ports.UserRepository,
	expenseRepo ports.ExpenseRepository,
) *HouseholdUseCase {
	return &HouseholdUseCase{
		householdRepo: householdRepo,
		userRepo:      userRepo,
		expenseRepo:   expenseRepo,
	}
}

type CreateHouseholdInput struct {
	Name string `json:"name" binding:"required"`
}

type JoinHouseholdInput struct {
	InviteCode string `json:"invite_code" binding:"required"`
}

type MemberSplitInput struct {
	UserID          uuid.UUID `json:"user_id" binding:"required"`
	SplitPercentage float64   `json:"split_percentage" binding:"required"`
}

type UpdateBillingCycleInput struct {
	BillingCycleDay int `json:"billing_cycle_day" binding:"required,min=1,max=31"`
}

type CycleSummary struct {
	CycleIndex  int     `json:"cycle_index"`
	StartDate   string  `json:"start_date"`
	EndDate     string  `json:"end_date"`
	TotalAmount float64 `json:"total_amount"`
	Status      string  `json:"status"` // "PENDING" or "SETTLED"
}

func generateInviteCode() string {
	bytes := make([]byte, 4)
	if _, err := rand.Read(bytes); err != nil {
		return strings.ToUpper(uuid.New().String()[:8])
	}
	return strings.ToUpper(hex.EncodeToString(bytes))
}

func (uc *HouseholdUseCase) CreateHousehold(ctx context.Context, userID uuid.UUID, input CreateHouseholdInput) (*domain.Household, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	household := &domain.Household{
		ID:              uuid.New(),
		Name:            input.Name,
		InviteCode:      generateInviteCode(),
		CreatedBy:       user.ID,
		BillingCycleDay: 1,
	}

	if err := uc.householdRepo.Create(ctx, household); err != nil {
		return nil, fmt.Errorf("failed to create household: %w", err)
	}

	member := &domain.HouseholdMember{
		ID:              uuid.New(),
		HouseholdID:     household.ID,
		UserID:          user.ID,
		Role:            "ADMIN",
		SplitPercentage: 100.00,
		JoinedAt:        time.Now(),
	}

	if err := uc.householdRepo.AddMember(ctx, member); err != nil {
		return nil, fmt.Errorf("failed to add initial member: %w", err)
	}

	user.ActiveHouseholdID = &household.ID
	if err := uc.userRepo.Update(ctx, user); err != nil {
		return nil, fmt.Errorf("failed to update user active household: %w", err)
	}

	household.Members = []domain.HouseholdMember{*member}
	return household, nil
}

func (uc *HouseholdUseCase) JoinHousehold(ctx context.Context, userID uuid.UUID, input JoinHouseholdInput) (*domain.Household, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	code := strings.TrimSpace(strings.ToUpper(input.InviteCode))
	household, err := uc.householdRepo.FindByInviteCode(ctx, code)
	if err != nil {
		return nil, err
	}
	if household == nil {
		return nil, ErrHouseholdNotFound
	}

	existingMember, err := uc.householdRepo.FindMember(ctx, household.ID, user.ID)
	if err != nil {
		return nil, err
	}
	if existingMember != nil {
		return nil, ErrAlreadyHouseholdMember
	}

	member := &domain.HouseholdMember{
		ID:              uuid.New(),
		HouseholdID:     household.ID,
		UserID:          user.ID,
		Role:            "MEMBER",
		SplitPercentage: 0,
		JoinedAt:        time.Now(),
	}

	if err := uc.householdRepo.AddMember(ctx, member); err != nil {
		return nil, fmt.Errorf("failed to join household: %w", err)
	}

	user.ActiveHouseholdID = &household.ID
	if err := uc.userRepo.Update(ctx, user); err != nil {
		return nil, fmt.Errorf("failed to update user active household: %w", err)
	}

	_ = uc.rebalanceMembersEquitably(ctx, household.ID)

	return uc.householdRepo.FindByID(ctx, household.ID)
}

func (uc *HouseholdUseCase) rebalanceMembersEquitably(ctx context.Context, householdID uuid.UUID) error {
	members, err := uc.householdRepo.GetMembers(ctx, householdID)
	if err != nil || len(members) == 0 {
		return err
	}
	n := len(members)
	basePct := math.Floor((100.0/float64(n))*100.0) / 100.0
	remainder := math.Round((100.0-(basePct*float64(n)))*100.0) / 100.0

	for i := range members {
		pct := basePct
		if i == 0 {
			pct += remainder // El primer integrante absorbe centésimos para sumar exactamente 100.00%
		}
		members[i].SplitPercentage = pct
		_ = uc.householdRepo.UpdateMember(ctx, &members[i])
	}
	return nil
}

func (uc *HouseholdUseCase) UpdateMemberSplits(ctx context.Context, userID uuid.UUID, splits []MemberSplitInput) error {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	callerMember, err := uc.householdRepo.FindMember(ctx, householdID, userID)
	if err != nil || callerMember == nil {
		return ErrUnauthorizedHousehold
	}

	household, err := uc.householdRepo.FindByID(ctx, householdID)
	if err != nil || household == nil {
		return ErrHouseholdNotFound
	}

	if callerMember.Role != "ADMIN" && household.CreatedBy != userID {
		return ErrUnauthorizedHousehold
	}

	// Validar que la suma sea exactamente 100%
	var totalPct float64
	splitMap := make(map[uuid.UUID]float64)
	for _, s := range splits {
		totalPct += s.SplitPercentage
		splitMap[s.UserID] = s.SplitPercentage
	}

	if math.Abs(totalPct-100.0) > 0.05 {
		return ErrInvalidSplitSum
	}

	members, err := uc.householdRepo.GetMembers(ctx, householdID)
	if err != nil {
		return err
	}

	for _, m := range members {
		if newPct, ok := splitMap[m.UserID]; ok {
			m.SplitPercentage = newPct
			if err := uc.householdRepo.UpdateMember(ctx, &m); err != nil {
				return err
			}
		}
	}

	return nil
}

func (uc *HouseholdUseCase) DeleteHousehold(ctx context.Context, userID uuid.UUID) error {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	household, err := uc.householdRepo.FindByID(ctx, householdID)
	if err != nil || household == nil {
		return ErrHouseholdNotFound
	}

	callerMember, err := uc.householdRepo.FindMember(ctx, householdID, userID)
	if err != nil || callerMember == nil {
		return ErrUnauthorizedHousehold
	}

	if callerMember.Role != "ADMIN" && household.CreatedBy != userID {
		return ErrUnauthorizedHousehold
	}

	return uc.householdRepo.Delete(ctx, householdID)
}

func (uc *HouseholdUseCase) LeaveHousehold(ctx context.Context, userID uuid.UUID) error {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	members, err := uc.householdRepo.GetMembers(ctx, householdID)
	if err != nil {
		return err
	}

	user.ActiveHouseholdID = nil
	_ = uc.userRepo.Update(ctx, user)

	if len(members) <= 1 {
		// Si es el único integrante, se elimina el hogar completamente
		return uc.householdRepo.Delete(ctx, householdID)
	}

	if err := uc.householdRepo.RemoveMember(ctx, householdID, userID); err != nil {
		return err
	}

	return uc.rebalanceMembersEquitably(ctx, householdID)
}

func (uc *HouseholdUseCase) RemoveMember(ctx context.Context, adminUserID, targetUserID uuid.UUID) error {
	user, err := uc.userRepo.FindByID(ctx, adminUserID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	callerMember, err := uc.householdRepo.FindMember(ctx, householdID, adminUserID)
	if err != nil || callerMember == nil {
		return ErrUnauthorizedHousehold
	}

	household, err := uc.householdRepo.FindByID(ctx, householdID)
	if err != nil || household == nil {
		return ErrHouseholdNotFound
	}

	if callerMember.Role != "ADMIN" && household.CreatedBy != adminUserID {
		return ErrUnauthorizedHousehold
	}

	if adminUserID == targetUserID {
		return errors.New("el administrador no puede expulsarse a sí mismo; para disolver el hogar utilice eliminar hogar")
	}

	targetMember, err := uc.householdRepo.FindMember(ctx, householdID, targetUserID)
	if err != nil || targetMember == nil {
		return errors.New("el usuario a expulsar no es integrante de este hogar")
	}

	if err := uc.householdRepo.RemoveMember(ctx, householdID, targetUserID); err != nil {
		return err
	}

	targetUser, _ := uc.userRepo.FindByID(ctx, targetUserID)
	if targetUser != nil {
		targetUser.ActiveHouseholdID = nil
		_ = uc.userRepo.Update(ctx, targetUser)
	}

	return uc.rebalanceMembersEquitably(ctx, householdID)
}

func (uc *HouseholdUseCase) UpdateBillingCycle(ctx context.Context, userID uuid.UUID, cycleDay int) (*domain.Household, error) {
	if cycleDay < 1 || cycleDay > 31 {
		return nil, errors.New("el día de corte debe estar entre 1 y 31")
	}

	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return nil, ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	household, err := uc.householdRepo.FindByID(ctx, householdID)
	if err != nil || household == nil {
		return nil, ErrHouseholdNotFound
	}

	callerMember, err := uc.householdRepo.FindMember(ctx, householdID, userID)
	if err != nil || callerMember == nil {
		return nil, ErrUnauthorizedHousehold
	}

	if callerMember.Role != "ADMIN" && household.CreatedBy != userID {
		return nil, ErrUnauthorizedHousehold
	}

	household.BillingCycleDay = cycleDay
	if err := uc.householdRepo.Update(ctx, household); err != nil {
		return nil, err
	}

	return household, nil
}

func CalculateCycleBounds(cycleDay int, refDate time.Time) (time.Time, time.Time) {
	if cycleDay < 1 {
		cycleDay = 1
	}
	year, month, day := refDate.Date()
	var startYear int
	var startMonth time.Month

	if day >= cycleDay {
		startYear = year
		startMonth = month
	} else {
		prev := refDate.AddDate(0, -1, 0)
		startYear = prev.Year()
		startMonth = prev.Month()
	}

	startDate := clampDate(startYear, startMonth, cycleDay)
	next := startDate.AddDate(0, 1, 0)
	endDate := clampDate(next.Year(), next.Month(), cycleDay).Add(-time.Second)

	return startDate, endDate
}

func clampDate(year int, month time.Month, day int) time.Time {
	firstOfNextMonth := time.Date(year, month+1, 1, 0, 0, 0, 0, time.UTC)
	lastDayOfMonth := firstOfNextMonth.AddDate(0, 0, -1).Day()
	actualDay := day
	if actualDay > lastDayOfMonth {
		actualDay = lastDayOfMonth
	}
	return time.Date(year, month, actualDay, 0, 0, 0, 0, time.UTC)
}

func (uc *HouseholdUseCase) GetCyclesHistory(ctx context.Context, userID uuid.UUID) ([]CycleSummary, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return nil, ErrNoActiveHousehold
	}

	householdID := *user.ActiveHouseholdID
	household, err := uc.householdRepo.FindByID(ctx, householdID)
	if err != nil || household == nil {
		return nil, ErrHouseholdNotFound
	}

	cycleDay := household.BillingCycleDay
	if cycleDay < 1 {
		cycleDay = 1
	}

	now := time.Now().UTC()
	var cycles []CycleSummary

	// Generar los últimos 6 ciclos
	for i := 0; i < 6; i++ {
		refDate := now.AddDate(0, -i, 0)
		start, end := CalculateCycleBounds(cycleDay, refDate)

		var totalAmount float64
		var pendingCount int
		if uc.expenseRepo != nil {
			expenses, err := uc.expenseRepo.FindByHouseholdAndPeriod(ctx, householdID, start, end)
			if err == nil {
				for _, exp := range expenses {
					if exp.Scope == domain.ExpenseScopeHousehold {
						totalAmount += exp.Amount
						if exp.PaymentStatus == domain.ExpenseStatusPending {
							pendingCount++
						}
					}
				}
			}
		}

		status := "SETTLED"
		if i == 0 || pendingCount > 0 {
			// El ciclo en curso o con cuentas abiertas queda PENDING
			status = "PENDING"
		}

		cycles = append(cycles, CycleSummary{
			CycleIndex:  i,
			StartDate:   start.Format("2006-01-02"),
			EndDate:     end.Format("2006-01-02"),
			TotalAmount: totalAmount,
			Status:      status,
		})
	}

	return cycles, nil
}

type AdaptiveModeResponse struct {
	Mode            string     `json:"mode"` // "SOLITARY" or "HOUSEHOLD"
	HouseholdID     *uuid.UUID `json:"household_id,omitempty"`
	HouseholdName   string     `json:"household_name,omitempty"`
	BillingCycleDay int        `json:"billing_cycle_day"`
	MembersCount    int        `json:"members_count"`
	DefaultSplitPct float64    `json:"default_split_pct"`
}

func (uc *HouseholdUseCase) GetAdaptiveMode(ctx context.Context, userID uuid.UUID) (*AdaptiveModeResponse, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	if user.ActiveHouseholdID == nil {
		return &AdaptiveModeResponse{
			Mode:            "SOLITARY",
			BillingCycleDay: 1,
			MembersCount:    1,
			DefaultSplitPct: 100.0,
		}, nil
	}

	household, err := uc.householdRepo.FindByID(ctx, *user.ActiveHouseholdID)
	if err != nil || household == nil {
		return &AdaptiveModeResponse{
			Mode:            "SOLITARY",
			BillingCycleDay: 1,
			MembersCount:    1,
			DefaultSplitPct: 100.0,
		}, nil
	}

	memberCount := len(household.Members)
	if memberCount <= 1 {
		return &AdaptiveModeResponse{
			Mode:            "SOLITARY",
			HouseholdID:     &household.ID,
			HouseholdName:   household.Name,
			BillingCycleDay: household.BillingCycleDay,
			MembersCount:    memberCount,
			DefaultSplitPct: 100.0,
		}, nil
	}

	return &AdaptiveModeResponse{
		Mode:            "HOUSEHOLD",
		HouseholdID:     &household.ID,
		HouseholdName:   household.Name,
		BillingCycleDay: household.BillingCycleDay,
		MembersCount:    memberCount,
		DefaultSplitPct: 50.0,
	}, nil
}

func (uc *HouseholdUseCase) GetMembers(ctx context.Context, userID uuid.UUID) ([]domain.HouseholdMember, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil || user.ActiveHouseholdID == nil {
		return nil, ErrNoActiveHousehold
	}

	return uc.householdRepo.GetMembers(ctx, *user.ActiveHouseholdID)
}

type CycleExpenseItem struct {
	ID            uuid.UUID `json:"id"`
	Date          string    `json:"date"`
	Description   string    `json:"description"`
	Category      string    `json:"category"`
	Amount        float64   `json:"amount"`
	PayerName     string    `json:"payer_name"`
	PaymentStatus string    `json:"payment_status"`
}

type CycleDetailResponse struct {
	CycleIndex  int                `json:"cycle_index"`
	StartDate   string             `json:"start_date"`
	EndDate     string             `json:"end_date"`
	Status      string             `json:"status"` // "PENDING" or "SETTLED"
	TotalAmount float64            `json:"total_amount"`
	Expenses    []CycleExpenseItem `json:"expenses"`
	Balances    []RoomieBalance    `json:"balances"`
	Settlement  string             `json:"settlement"`
}

func (uc *HouseholdUseCase) GetCycleDetail(ctx context.Context, userID uuid.UUID, cycleIndex int) (*CycleDetailResponse, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
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

	refDate := time.Now().UTC().AddDate(0, -cycleIndex, 0)
	start, end := CalculateCycleBounds(cycleDay, refDate)

	var expenseItems []CycleExpenseItem
	paidByUser := make(map[uuid.UUID]float64)
	var totalHousehold float64
	var pendingCount int

	if uc.expenseRepo != nil {
		expenses, err := uc.expenseRepo.FindByHouseholdAndPeriod(ctx, householdID, start, end)
		if err == nil {
			for _, exp := range expenses {
				if exp.Scope == domain.ExpenseScopeHousehold {
					totalHousehold += exp.Amount
					paidByUser[exp.UserID] += exp.Amount
					if exp.PaymentStatus == domain.ExpenseStatusPending {
						pendingCount++
					}

					payer := "Roomie"
					if exp.User != nil && exp.User.Name != "" {
						payer = exp.User.Name
					}
					expenseItems = append(expenseItems, CycleExpenseItem{
						ID:            exp.ID,
						Date:          exp.Date.Format("2006-01-02"),
						Description:   exp.Description,
						Category:      exp.Category,
						Amount:        exp.Amount,
						PayerName:     payer,
						PaymentStatus: string(exp.PaymentStatus),
					})
				}
			}
		}
	}

	memberCount := len(members)
	if memberCount == 0 {
		memberCount = 1
	}

	payments, _ := uc.householdRepo.GetPaymentsByHouseholdAndPeriod(ctx, householdID, start, end)
	paidToOthers := make(map[uuid.UUID]float64)
	receivedFromOthers := make(map[uuid.UUID]float64)
	for _, p := range payments {
		paidToOthers[p.PayerUserID] += p.Amount
		receivedFromOthers[p.ReceiverUserID] += p.Amount
	}

	var balances []RoomieBalance
	for _, m := range members {
		splitPct := m.SplitPercentage
		if splitPct <= 0 {
			splitPct = 100.0 / float64(memberCount)
		}
		expectedShare := totalHousehold * (splitPct / 100.0)
		paid := paidByUser[m.UserID]
		net := (paid - expectedShare) + paidToOthers[m.UserID] - receivedFromOthers[m.UserID]

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

	settlementText := "Las cuentas de este ciclo están equilibradas."
	if len(balances) == 2 {
		r1 := balances[0]
		r2 := balances[1]
		if r1.NetBalance < -0.01 {
			settlementText = fmt.Sprintf("%s le debe $%.0f COP a %s.", r1.UserName, math.Abs(r1.NetBalance), r2.UserName)
		} else if r2.NetBalance < -0.01 {
			settlementText = fmt.Sprintf("%s le debe $%.0f COP a %s.", r2.UserName, math.Abs(r2.NetBalance), r1.UserName)
		}
	}

	status := "SETTLED"
	if cycleIndex == 0 || pendingCount > 0 {
		status = "PENDING"
	}

	return &CycleDetailResponse{
		CycleIndex:  cycleIndex,
		StartDate:   start.Format("2006-01-02"),
		EndDate:     end.Format("2006-01-02"),
		Status:      status,
		TotalAmount: totalHousehold,
		Expenses:    expenseItems,
		Balances:    balances,
		Settlement:  settlementText,
	}, nil
}

type CreateSettlementPaymentInput struct {
	ReceiverUserID uuid.UUID `json:"receiver_user_id" binding:"required"`
	Amount         float64   `json:"amount" binding:"required,gt=0"`
	Channel        string    `json:"channel" binding:"required"` // "Transferencia" o "Efectivo"
	Date           time.Time `json:"date"`
	Notes          string    `json:"notes"`
}

type CurrentHouseholdResponse struct {
	ID              uuid.UUID                `json:"id"`
	Name            string                   `json:"name"`
	InviteCode      string                   `json:"invite_code"`
	CreatedBy       uuid.UUID                `json:"created_by"`
	BillingCycleDay int                      `json:"billing_cycle_day"`
	Role            string                   `json:"role"`
	Members         []domain.HouseholdMember `json:"members"`
}

func (uc *HouseholdUseCase) CreateSettlementPayment(ctx context.Context, userID uuid.UUID, input CreateSettlementPaymentInput) (*domain.SettlementPayment, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return nil, ErrNoActiveHousehold
	}
	householdID := *user.ActiveHouseholdID

	if userID == input.ReceiverUserID {
		return nil, errors.New("no puedes realizar un pago de liquidación a ti mismo")
	}

	receiverMember, err := uc.householdRepo.FindMember(ctx, householdID, input.ReceiverUserID)
	if err != nil || receiverMember == nil {
		return nil, errors.New("el integrante destinatario no pertenece a este hogar")
	}

	date := input.Date
	if date.IsZero() {
		date = time.Now()
	}

	channel := input.Channel
	if channel != "Transferencia" && channel != "Efectivo" {
		channel = "Transferencia"
	}

	payment := &domain.SettlementPayment{
		ID:             uuid.New(),
		HouseholdID:    householdID,
		PayerUserID:    userID,
		ReceiverUserID: input.ReceiverUserID,
		Amount:         input.Amount,
		Channel:        channel,
		Date:           date,
		Notes:          input.Notes,
		CreatedAt:      time.Now(),
	}

	if err := uc.householdRepo.CreatePayment(ctx, payment); err != nil {
		return nil, fmt.Errorf("failed to create settlement payment: %w", err)
	}

	return uc.householdRepo.FindPaymentByID(ctx, payment.ID)
}

func (uc *HouseholdUseCase) GetSettlementPayments(ctx context.Context, userID uuid.UUID, limit, offset int) ([]domain.SettlementPayment, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
		return nil, ErrNoActiveHousehold
	}
	return uc.householdRepo.GetPaymentsByHousehold(ctx, *user.ActiveHouseholdID, limit, offset)
}

func (uc *HouseholdUseCase) GetCurrentHousehold(ctx context.Context, userID uuid.UUID) (*CurrentHouseholdResponse, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil || user == nil || user.ActiveHouseholdID == nil {
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

	userRole := "MEMBER"
	for _, m := range members {
		if m.UserID == userID {
			userRole = m.Role
			break
		}
	}
	if household.CreatedBy == userID {
		userRole = "ADMIN"
	}

	return &CurrentHouseholdResponse{
		ID:              household.ID,
		Name:            household.Name,
		InviteCode:      household.InviteCode,
		CreatedBy:       household.CreatedBy,
		BillingCycleDay: household.BillingCycleDay,
		Role:            userRole,
		Members:         members,
	}, nil
}

