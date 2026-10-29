package usecase

import (
	"context"
	"errors"
	"fmt"
	"io"
	"math"
	"strings"
	"time"
	"unicode"

	"github.com/google/uuid"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

var (
	ErrStatementNotFound   = errors.New("bank statement not found")
	ErrTransactionNotFound = errors.New("bank transaction not found")
	ErrUnauthorizedAccess  = errors.New("unauthorized access to statement")
)

type ReconciliationUseCase struct {
	statementRepo ports.BankStatementRepository
	expenseRepo   ports.ExpenseRepository
	userRepo      ports.UserRepository
	parser        ports.StatementParserPort
}

func NewReconciliationUseCase(
	statementRepo ports.BankStatementRepository,
	expenseRepo ports.ExpenseRepository,
	userRepo ports.UserRepository,
	parser ports.StatementParserPort,
) *ReconciliationUseCase {
	return &ReconciliationUseCase{
		statementRepo: statementRepo,
		expenseRepo:   expenseRepo,
		userRepo:      userRepo,
		parser:        parser,
	}
}

type UploadStatementResult struct {
	Statement           *domain.BankStatement `json:"statement"`
	TotalTransactions   int                   `json:"total_transactions"`
	SuspectedDuplicates int                   `json:"suspected_duplicates"`
}

func (uc *ReconciliationUseCase) UploadStatement(
	ctx context.Context,
	userID uuid.UUID,
	bankName string,
	accountLastFour string,
	r io.Reader,
	filename string,
) (*UploadStatementResult, error) {
	user, err := uc.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, ErrUserNotFound
	}

	parsed, err := uc.parser.Parse(r, filename)
	if err != nil {
		return nil, fmt.Errorf("failed to parse statement: %w", err)
	}

	if bankName == "" && parsed.BankName != "" {
		bankName = parsed.BankName
	}
	if bankName == "" {
		bankName = "Unknown Bank"
	}

	if accountLastFour == "" && parsed.AccountLastFour != "" {
		accountLastFour = parsed.AccountLastFour
	}
	if accountLastFour == "" {
		accountLastFour = "0000"
	}

	// Fetch existing user expenses to run heuristic matching
	userExpenses, err := uc.expenseRepo.FindByUser(ctx, userID, 1000, 0)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch user expenses: %w", err)
	}

	statementID := uuid.New()
	statement := &domain.BankStatement{
		ID:              statementID,
		UserID:          userID,
		BankName:        bankName,
		AccountLastFour: accountLastFour,
		PeriodStart:     parsed.PeriodStart,
		PeriodEnd:       parsed.PeriodEnd,
		CreatedAt:       time.Now(),
	}

	var txEntities []domain.BankTransaction
	suspectedCount := 0

	for _, pt := range parsed.Transactions {
		tx := domain.BankTransaction{
			ID:              uuid.New(),
			StatementID:     statementID,
			TransactionDate: pt.Date,
			Description:     pt.Description,
			Amount:          pt.Amount,
			DuplicateStatus: domain.DuplicateStatusNone,
			ConfidenceScore: 0,
		}

		// Run heuristic duplicate detection engine
		matchedExpense, confidence := uc.evaluateDuplicate(pt.Date, pt.Amount, pt.Description, userExpenses)
		if matchedExpense != nil && confidence >= 80.0 {
			tx.DuplicateStatus = domain.DuplicateStatusSuspected
			tx.ConfidenceScore = confidence
			tx.MatchedExpenseID = &matchedExpense.ID
			suspectedCount++
		}

		txEntities = append(txEntities, tx)
	}

	statement.Transactions = txEntities
	if err := uc.statementRepo.Create(ctx, statement); err != nil {
		return nil, fmt.Errorf("failed to save statement: %w", err)
	}

	return &UploadStatementResult{
		Statement:           statement,
		TotalTransactions:   len(txEntities),
		SuspectedDuplicates: suspectedCount,
	}, nil
}

func (uc *ReconciliationUseCase) evaluateDuplicate(
	txDate time.Time,
	txAmount float64,
	txDesc string,
	expenses []domain.Expense,
) (*domain.Expense, float64) {
	var bestMatch *domain.Expense
	var highestConfidence float64

	for i := range expenses {
		exp := &expenses[i]

		// 1. Amount match check (|diff| < 1.00 COP)
		amountDiff := math.Abs(txAmount - exp.Amount)
		if amountDiff >= 1.00 {
			continue
		}

		// 2. Date tolerance window (+/- 4 days)
		daysDiff := math.Abs(txDate.Sub(exp.Date).Hours()) / 24.0
		if daysDiff > 4.0 {
			continue
		}

		// Score computation:
		// Base score for exact amount match within 4 days: 50%
		score := 50.0

		// Date proximity:
		if daysDiff < 0.5 {
			score += 30.0 // same day
		} else if daysDiff <= 1.0 {
			score += 25.0 // within 1 day
		} else if daysDiff <= 2.0 {
			score += 20.0 // within 2 days
		} else {
			score += 15.0 // within 4 days
		}

		// Description / Merchant token similarity:
		sim := calculateStringSimilarity(txDesc, exp.Description)
		score += sim * 20.0

		if score > highestConfidence {
			highestConfidence = score
			bestMatch = exp
		}
	}

	return bestMatch, highestConfidence
}

func (uc *ReconciliationUseCase) GetSuspectedDuplicates(ctx context.Context, userID uuid.UUID) ([]domain.BankTransaction, error) {
	return uc.statementRepo.FindSuspectedDuplicates(ctx, userID)
}

func (uc *ReconciliationUseCase) LinkTransaction(ctx context.Context, userID uuid.UUID, transactionID, expenseID uuid.UUID) error {
	tx, err := uc.statementRepo.FindTransactionByID(ctx, transactionID)
	if err != nil {
		return err
	}
	if tx == nil {
		return ErrTransactionNotFound
	}

	statement, err := uc.statementRepo.FindByID(ctx, tx.StatementID)
	if err != nil {
		return err
	}
	if statement == nil || statement.UserID != userID {
		return ErrUnauthorizedAccess
	}

	tx.MatchedExpenseID = &expenseID
	tx.DuplicateStatus = domain.DuplicateStatusConfirmed
	tx.ConfidenceScore = 100.0

	return uc.statementRepo.UpdateTransaction(ctx, tx)
}

func (uc *ReconciliationUseCase) DismissDuplicate(ctx context.Context, userID uuid.UUID, transactionID uuid.UUID) error {
	tx, err := uc.statementRepo.FindTransactionByID(ctx, transactionID)
	if err != nil {
		return err
	}
	if tx == nil {
		return ErrTransactionNotFound
	}

	statement, err := uc.statementRepo.FindByID(ctx, tx.StatementID)
	if err != nil {
		return err
	}
	if statement == nil || statement.UserID != userID {
		return ErrUnauthorizedAccess
	}

	tx.MatchedExpenseID = nil
	tx.DuplicateStatus = domain.DuplicateStatusDismissed

	return uc.statementRepo.UpdateTransaction(ctx, tx)
}

// Token-based and Levenshtein similarity
func calculateStringSimilarity(s1, s2 string) float64 {
	tokens1 := tokenize(s1)
	tokens2 := tokenize(s2)

	if len(tokens1) == 0 || len(tokens2) == 0 {
		return 0.0
	}

	matchCount := 0
	for _, t1 := range tokens1 {
		for _, t2 := range tokens2 {
			if t1 == t2 || strings.Contains(t1, t2) || strings.Contains(t2, t1) {
				matchCount++
				break
			}
		}
	}

	tokenScore := float64(matchCount) / float64(math.Max(float64(len(tokens1)), float64(len(tokens2))))

	// Also calculate basic Levenshtein ratio
	levDist := levenshteinDistance(strings.ToLower(s1), strings.ToLower(s2))
	maxLen := math.Max(float64(len(s1)), float64(len(s2)))
	levScore := 1.0 - (float64(levDist) / maxLen)
	if levScore < 0 {
		levScore = 0
	}

	return math.Max(tokenScore, levScore)
}

func tokenize(s string) []string {
	words := strings.FieldsFunc(s, func(r rune) bool {
		return !unicode.IsLetter(r) && !unicode.IsNumber(r)
	})

	var filtered []string
	stopWords := map[string]bool{
		"de": true, "la": true, "el": true, "los": true, "las": true,
		"en": true, "por": true, "para": true, "con": true, "compra": true,
		"pago": true, "sa": true, "sas": true,
	}

	for _, w := range words {
		lower := strings.ToLower(w)
		if len(lower) > 2 && !stopWords[lower] {
			filtered = append(filtered, lower)
		}
	}
	return filtered
}

func levenshteinDistance(s1, s2 string) int {
	r1, r2 := []rune(s1), []rune(s2)
	l1, l2 := len(r1), len(r2)

	dp := make([][]int, l1+1)
	for i := range dp {
		dp[i] = make([]int, l2+1)
		dp[i][0] = i
	}
	for j := 0; j <= l2; j++ {
		dp[0][j] = j
	}

	for i := 1; i <= l1; i++ {
		for j := 1; j <= l2; j++ {
			cost := 0
			if r1[i-1] != r2[j-1] {
				cost = 1
			}
			dp[i][j] = min3(
				dp[i-1][j]+1,      // deletion
				dp[i][j-1]+1,      // insertion
				dp[i-1][j-1]+cost, // substitution
			)
		}
	}

	return dp[l1][l2]
}

func min3(a, b, c int) int {
	if a <= b && a <= c {
		return a
	}
	if b <= a && b <= c {
		return b
	}
	return c
}
