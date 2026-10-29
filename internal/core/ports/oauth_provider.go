package ports

import (
	"context"

	"house-expenses/internal/core/domain"
)

type OAuthUserInfo struct {
	Provider       domain.AuthProvider
	ProviderUserID string
	Email          string
	Name           string
	AvatarURL      string
}

type OAuthProvider interface {
	GetProviderName() domain.AuthProvider
	GetAuthURL(state string) string
	ExchangeCode(ctx context.Context, code string) (*OAuthUserInfo, error)
}

type OAuthRegistry interface {
	Register(provider OAuthProvider)
	Get(name domain.AuthProvider) (OAuthProvider, error)
}
