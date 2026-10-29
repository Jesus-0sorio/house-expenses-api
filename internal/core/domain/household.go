package domain

import (
	"time"

	"github.com/google/uuid"
)

type Household struct {
	ID              uuid.UUID         `json:"id" gorm:"type:uuid;primaryKey"`
	Name            string            `json:"name" gorm:"type:varchar(255);not null"`
	InviteCode      string            `json:"invite_code" gorm:"type:varchar(32);uniqueIndex;not null"`
	CreatedBy       uuid.UUID         `json:"created_by" gorm:"type:uuid"`
	BillingCycleDay int               `json:"billing_cycle_day" gorm:"default:1"`
	CreatedAt       time.Time         `json:"created_at"`
	UpdatedAt       time.Time         `json:"updated_at"`
	Members         []HouseholdMember `json:"members,omitempty" gorm:"foreignKey:HouseholdID"`
}

type HouseholdMember struct {
	ID              uuid.UUID `json:"id" gorm:"type:uuid;primaryKey"`
	HouseholdID     uuid.UUID `json:"household_id" gorm:"type:uuid;not null;index"`
	UserID          uuid.UUID `json:"user_id" gorm:"type:uuid;not null;index"`
	Role            string    `json:"role" gorm:"type:varchar(20);default:'MEMBER'"`
	SplitPercentage float64   `json:"split_percentage" gorm:"type:decimal(5,2);default:50.00"`
	JoinedAt        time.Time `json:"joined_at"`
	User            *User     `json:"user,omitempty" gorm:"foreignKey:UserID"`
}

type SettlementPayment struct {
	ID             uuid.UUID `json:"id" gorm:"type:uuid;primaryKey"`
	HouseholdID    uuid.UUID `json:"household_id" gorm:"type:uuid;not null;index"`
	PayerUserID    uuid.UUID `json:"payer_user_id" gorm:"type:uuid;not null;index"`
	ReceiverUserID uuid.UUID `json:"receiver_user_id" gorm:"type:uuid;not null;index"`
	Amount         float64   `json:"amount" gorm:"type:decimal(12,2);not null"`
	Channel        string    `json:"channel" gorm:"type:varchar(50);not null"` // "Transferencia" o "Efectivo"
	Date           time.Time `json:"date" gorm:"not null;index"`
	Notes          string    `json:"notes" gorm:"type:text"`
	CreatedAt      time.Time `json:"created_at"`

	Payer          *User     `json:"payer,omitempty" gorm:"foreignKey:PayerUserID"`
	Receiver       *User     `json:"receiver,omitempty" gorm:"foreignKey:ReceiverUserID"`
}
