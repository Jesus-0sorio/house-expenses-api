package usecase

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

var (
	ErrUserAlreadyExists = errors.New("user with this email already exists")
	ErrInvalidCredentials = errors.New("invalid email or password")
	ErrUserNotFound       = errors.New("user not found")
)

type JWTClaims struct {
	UserID    uuid.UUID           `json:"user_id"`
	Email     string              `json:"email"`
	Name      string              `json:"name"`
	Provider  domain.AuthProvider `json:"provider"`
	jwt.RegisteredClaims
}

type AuthUseCase struct {
	userRepo      ports.UserRepository
	oauthRegistry ports.OAuthRegistry
	jwtSecret     string
}

func NewAuthUseCase(userRepo ports.UserRepository, oauthRegistry ports.OAuthRegistry, jwtSecret string) *AuthUseCase {
	return &AuthUseCase{
		userRepo:      userRepo,
		oauthRegistry: oauthRegistry,
		jwtSecret:     jwtSecret,
	}
}

type RegisterInput struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=6"`
	Name     string `json:"name" binding:"required"`
}

type AuthResponse struct {
	Token string       `json:"token"`
	User  *domain.User `json:"user"`
}

func (uc *AuthUseCase) Register(ctx context.Context, input RegisterInput) (*AuthResponse, error) {
	existing, err := uc.userRepo.FindByEmail(ctx, input.Email)
	if err != nil {
		return nil, err
	}
	if existing != nil {
		return nil, ErrUserAlreadyExists
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(input.Password), bcrypt.DefaultCost)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %w", err)
	}
	hashStr := string(hashedPassword)

	user := &domain.User{
		ID:                  uuid.New(),
		Email:               input.Email,
		PasswordHash:        &hashStr,
		Name:                input.Name,
		PrimaryAuthProvider: domain.AuthProviderLocal,
	}

	if err := uc.userRepo.Create(ctx, user); err != nil {
		return nil, err
	}

	token, err := uc.GenerateJWT(user)
	if err != nil {
		return nil, err
	}

	return &AuthResponse{
		Token: token,
		User:  user,
	}, nil
}

type LoginInput struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required"`
}

func (uc *AuthUseCase) Login(ctx context.Context, input LoginInput) (*AuthResponse, error) {
	user, err := uc.userRepo.FindByEmail(ctx, input.Email)
	if err != nil {
		return nil, err
	}
	if user == nil || user.PasswordHash == nil {
		return nil, ErrInvalidCredentials
	}

	if err := bcrypt.CompareHashAndPassword([]byte(*user.PasswordHash), []byte(input.Password)); err != nil {
		return nil, ErrInvalidCredentials
	}

	token, err := uc.GenerateJWT(user)
	if err != nil {
		return nil, err
	}

	return &AuthResponse{
		Token: token,
		User:  user,
	}, nil
}

func (uc *AuthUseCase) GetOAuthConsentURL(providerName domain.AuthProvider, state string) (string, error) {
	if uc.oauthRegistry == nil {
		return "", errors.New("oauth registry is not configured")
	}
	provider, err := uc.oauthRegistry.Get(providerName)
	if err != nil {
		return "", err
	}
	return provider.GetAuthURL(state), nil
}

func (uc *AuthUseCase) HandleOAuthCallback(ctx context.Context, providerName domain.AuthProvider, code string) (*AuthResponse, error) {
	if uc.oauthRegistry == nil {
		return nil, errors.New("oauth registry is not configured")
	}
	provider, err := uc.oauthRegistry.Get(providerName)
	if err != nil {
		return nil, err
	}

	userInfo, err := provider.ExchangeCode(ctx, code)
	if err != nil {
		return nil, fmt.Errorf("oauth verification failed: %w", err)
	}

	// Check if OAuth account link already exists
	oauthAccount, err := uc.userRepo.FindOAuthAccount(ctx, providerName, userInfo.ProviderUserID)
	if err != nil {
		return nil, err
	}

	var user *domain.User
	if oauthAccount != nil {
		user, err = uc.userRepo.FindByID(ctx, oauthAccount.UserID)
		if err != nil {
			return nil, err
		}
	} else {
		// Check if user exists by email
		user, err = uc.userRepo.FindByEmail(ctx, userInfo.Email)
		if err != nil {
			return nil, err
		}

		if user == nil {
			// Auto provision user without password
			user = &domain.User{
				ID:                  uuid.New(),
				Email:               userInfo.Email,
				Name:                userInfo.Name,
				AvatarURL:           &userInfo.AvatarURL,
				PrimaryAuthProvider: providerName,
			}
			if err := uc.userRepo.Create(ctx, user); err != nil {
				return nil, err
			}
		}

		// Link OAuth account
		newLink := &domain.UserOAuthAccount{
			ID:             uuid.New(),
			UserID:         user.ID,
			Provider:       providerName,
			ProviderUserID: userInfo.ProviderUserID,
			Email:          userInfo.Email,
		}
		if err := uc.userRepo.CreateOAuthAccount(ctx, newLink); err != nil {
			return nil, err
		}
	}

	token, err := uc.GenerateJWT(user)
	if err != nil {
		return nil, err
	}

	return &AuthResponse{
		Token: token,
		User:  user,
	}, nil
}

func (uc *AuthUseCase) GenerateJWT(user *domain.User) (string, error) {
	expirationTime := time.Now().Add(72 * time.Hour)
	claims := &JWTClaims{
		UserID:   user.ID,
		Email:    user.Email,
		Name:     user.Name,
		Provider: user.PrimaryAuthProvider,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expirationTime),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Subject:   user.ID.String(),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(uc.jwtSecret))
}

func (uc *AuthUseCase) ValidateJWT(tokenString string) (*JWTClaims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(uc.jwtSecret), nil
	})

	if err != nil {
		return nil, err
	}

	if claims, ok := token.Claims.(*JWTClaims); ok && token.Valid {
		return claims, nil
	}

	return nil, errors.New("invalid token")
}
