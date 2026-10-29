package domain

import (
	"time"

	"github.com/google/uuid"
)

type ExpenseScope string

const (
	ExpenseScopePersonal  ExpenseScope = "PERSONAL"
	ExpenseScopeHousehold ExpenseScope = "HOUSEHOLD"
)

type ExpensePaymentStatus string

const (
	ExpenseStatusPaid    ExpensePaymentStatus = "PAID"
	ExpenseStatusPending ExpensePaymentStatus = "PENDING"
)

type AutoSplitPolicy string

const (
	AutoSplitAlwaysHousehold AutoSplitPolicy = "ALWAYS_HOUSEHOLD"
	AutoSplitPrompt          AutoSplitPolicy = "PROMPT"
)

type Expense struct {
	ID            uuid.UUID            `json:"id" gorm:"type:uuid;primaryKey"`
	UserID        uuid.UUID            `json:"user_id" gorm:"type:uuid;not null;index"`
	HouseholdID   *uuid.UUID           `json:"household_id,omitempty" gorm:"type:uuid;index"`
	InvoiceID     *uuid.UUID           `json:"invoice_id,omitempty" gorm:"type:uuid;index"`
	Amount        float64              `json:"amount" gorm:"type:decimal(15,2);not null"`
	Date          time.Time            `json:"date" gorm:"not null;index"`
	Category      string               `json:"category" gorm:"type:varchar(100);not null"`
	Description   string               `json:"description" gorm:"type:varchar(500);not null"`
	Scope         ExpenseScope         `json:"scope" gorm:"type:varchar(50);not null;default:'PERSONAL'"`
	PaymentStatus ExpensePaymentStatus `json:"payment_status" gorm:"type:varchar(50);default:'PAID'"`
	CreatedAt     time.Time            `json:"created_at"`
	UpdatedAt     time.Time            `json:"updated_at"`

	Invoice *Invoice `json:"invoice,omitempty" gorm:"foreignKey:InvoiceID"`
	User    *User    `json:"user,omitempty" gorm:"foreignKey:UserID"`
}

type ClassificationRule struct {
	ID              uuid.UUID       `json:"id" gorm:"type:uuid;primaryKey"`
	UserID          uuid.UUID       `json:"user_id" gorm:"type:uuid;not null;index"`
	HouseholdID     *uuid.UUID      `json:"household_id,omitempty" gorm:"type:uuid;index"`
	MerchantPattern string          `json:"merchant_pattern" gorm:"type:varchar(255);not null"`
	DefaultScope    ExpenseScope    `json:"default_scope" gorm:"type:varchar(50);not null;default:'HOUSEHOLD'"`
	AutoSplitPolicy AutoSplitPolicy `json:"auto_split_policy" gorm:"type:varchar(50);not null;default:'PROMPT'"`
	CreatedAt       time.Time       `json:"created_at"`
}
