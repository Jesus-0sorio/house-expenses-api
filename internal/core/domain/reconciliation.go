package domain

import (
	"time"

	"github.com/google/uuid"
)

type DuplicateStatus string

const (
	DuplicateStatusNone      DuplicateStatus = "NONE"
	DuplicateStatusSuspected DuplicateStatus = "SUSPECTED"
	DuplicateStatusConfirmed DuplicateStatus = "CONFIRMED"
	DuplicateStatusDismissed DuplicateStatus = "DISMISSED"
)

type BankStatement struct {
	ID              uuid.UUID         `json:"id" gorm:"type:uuid;primaryKey"`
	UserID          uuid.UUID         `json:"user_id" gorm:"type:uuid;not null;index"`
	BankName        string            `json:"bank_name" gorm:"type:varchar(255);not null"`
	AccountLastFour string            `json:"account_last_four" gorm:"type:varchar(10);not null"`
	PeriodStart     time.Time         `json:"period_start" gorm:"not null"`
	PeriodEnd       time.Time         `json:"period_end" gorm:"not null"`
	CreatedAt       time.Time         `json:"created_at"`
	Transactions    []BankTransaction `json:"transactions,omitempty" gorm:"foreignKey:StatementID"`
}

type BankTransaction struct {
	ID               uuid.UUID       `json:"id" gorm:"type:uuid;primaryKey"`
	StatementID      uuid.UUID       `json:"statement_id" gorm:"type:uuid;not null;index"`
	TransactionDate  time.Time       `json:"transaction_date" gorm:"not null;index"`
	Description      string          `json:"description" gorm:"type:varchar(500);not null"`
	Amount           float64         `json:"amount" gorm:"type:decimal(15,2);not null"`
	MatchedExpenseID *uuid.UUID      `json:"matched_expense_id,omitempty" gorm:"type:uuid;index"`
	DuplicateStatus  DuplicateStatus `json:"duplicate_status" gorm:"type:varchar(50);default:'NONE'"`
	ConfidenceScore  float64         `json:"confidence_score" gorm:"type:decimal(5,2);default:0"`
}
