package handlers

import (
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
	"house-expenses/internal/core/usecase"
)

type AuthHandler struct {
	authUC   *usecase.AuthUseCase
	userRepo ports.UserRepository
}

func NewAuthHandler(authUC *usecase.AuthUseCase, userRepo ports.UserRepository) *AuthHandler {
	return &AuthHandler{
		authUC:   authUC,
		userRepo: userRepo,
	}
}

func (h *AuthHandler) RegisterRoutes(rg *gin.RouterGroup) {
	authGroup := rg.Group("/auth")
	{
		authGroup.POST("/register", h.Register)
		authGroup.POST("/login", h.Login)
		authGroup.GET("/oauth/:provider/url", h.GetOAuthURL)
		authGroup.POST("/oauth/:provider/callback", h.HandleOAuthCallback)
	}
}

func (h *AuthHandler) Register(c *gin.Context) {
	var input usecase.RegisterInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	resp, err := h.authUC.Register(c.Request.Context(), input)
	if err != nil {
		if errors.Is(err, usecase.ErrUserAlreadyExists) {
			c.JSON(http.StatusConflict, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, resp)
}

func (h *AuthHandler) Login(c *gin.Context) {
	var input usecase.LoginInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	resp, err := h.authUC.Login(c.Request.Context(), input)
	if err != nil {
		if errors.Is(err, usecase.ErrInvalidCredentials) {
			c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, resp)
}

func (h *AuthHandler) GetOAuthURL(c *gin.Context) {
	provider := domain.AuthProvider(c.Param("provider"))
	state := c.DefaultQuery("state", uuid.New().String())

	url, err := h.authUC.GetOAuthConsentURL(provider, state)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"provider": provider, "url": url, "state": state})
}

type OAuthCallbackRequest struct {
	Code string `json:"code" binding:"required"`
}

func (h *AuthHandler) HandleOAuthCallback(c *gin.Context) {
	provider := domain.AuthProvider(c.Param("provider"))
	var req OAuthCallbackRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	resp, err := h.authUC.HandleOAuthCallback(c.Request.Context(), provider, req.Code)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, resp)
}
