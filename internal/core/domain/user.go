package domain

import (
	"time"

	"github.com/google/uuid"
)

type AuthProvider string

const (
	AuthProviderLocal     AuthProvider = "LOCAL"
	AuthProviderGoogle    AuthProvider = "GOOGLE"
	AuthProviderMicrosoft AuthProvider = "MICROSOFT"
	AuthProviderGithub    AuthProvider = "GITHUB"
)

type User struct {
	ID                  uuid.UUID          `json:"id" gorm:"type:uuid;primaryKey"`
	Email               string             `json:"email" gorm:"type:varchar(255);uniqueIndex;not null"`
	PasswordHash        *string            `json:"-" gorm:"type:varchar(255)"`
	Name                string             `json:"name" gorm:"type:varchar(255);not null"`
	TaxID               *string            `json:"tax_id,omitempty" gorm:"type:varchar(50)"`
	AvatarURL           *string            `json:"avatar_url" gorm:"type:text"`
	PrimaryAuthProvider AuthProvider       `json:"primary_auth_provider" gorm:"type:varchar(50);default:'LOCAL'"`
	ActiveHouseholdID   *uuid.UUID         `json:"active_household_id" gorm:"type:uuid;index"`
	OAuthAccounts       []UserOAuthAccount `json:"oauth_accounts,omitempty" gorm:"foreignKey:UserID"`
	CreatedAt           time.Time          `json:"created_at"`
	UpdatedAt           time.Time          `json:"updated_at"`
}

type UserOAuthAccount struct {
	ID             uuid.UUID    `json:"id" gorm:"type:uuid;primaryKey"`
	UserID         uuid.UUID    `json:"user_id" gorm:"type:uuid;not null;index"`
	Provider       AuthProvider `json:"provider" gorm:"type:varchar(50);not null;uniqueIndex:idx_provider_user"`
	ProviderUserID string       `json:"provider_user_id" gorm:"type:varchar(255);not null;uniqueIndex:idx_provider_user"`
	Email          string       `json:"email" gorm:"type:varchar(255);not null"`
	CreatedAt      time.Time    `json:"created_at"`
}
