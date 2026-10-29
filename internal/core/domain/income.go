package domain

import (
	"time"

	"github.com/google/uuid"
)

type IncomeType string

const (
	IncomeTypeSalaryRegular IncomeType = "SALARY_REGULAR"
	IncomeTypePrima         IncomeType = "PRIMA"
	IncomeTypeBonus         IncomeType = "BONUS"
	IncomeTypeFreelance     IncomeType = "FREELANCE"
)

type Income struct {
	ID          uuid.UUID  `json:"id" gorm:"type:uuid;primaryKey"`
	UserID      uuid.UUID  `json:"user_id" gorm:"type:uuid;not null;index"`
	Amount      float64    `json:"amount" gorm:"type:decimal(15,2);not null"`
	Type        IncomeType `json:"type" gorm:"type:varchar(50);not null;default:'SALARY_REGULAR'"`
	Date        time.Time  `json:"date" gorm:"not null;index"`
	Description string     `json:"description" gorm:"type:varchar(500)"`
	CreatedAt   time.Time  `json:"created_at"`
}

type RecurringDebt struct {
	ID                    uuid.UUID    `json:"id" gorm:"type:uuid;primaryKey"`
	UserID                uuid.UUID    `json:"user_id" gorm:"type:uuid;not null;index"`
	Name                  string       `json:"name" gorm:"type:varchar(255);not null"`
	MonthlyAmount         float64      `json:"monthly_amount" gorm:"type:decimal(15,2);not null"`
	DueDay                int          `json:"due_day" gorm:"not null"`
	RemainingInstallments int          `json:"remaining_installments" gorm:"default:0"`
	Scope                 ExpenseScope `json:"scope" gorm:"type:varchar(50);default:'PERSONAL'"`
	CreatedAt             time.Time    `json:"created_at"`
}
