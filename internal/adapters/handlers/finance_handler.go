package handlers

import (
	"errors"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/core/usecase"
)

type FinanceHandler struct {
	cashFlowUC *usecase.CashFlowUseCase
}

func NewFinanceHandler(cashFlowUC *usecase.CashFlowUseCase) *FinanceHandler {
	return &FinanceHandler{cashFlowUC: cashFlowUC}
}

func (h *FinanceHandler) RegisterRoutes(rg *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	incomes := rg.Group("/incomes")
	incomes.Use(authMiddleware)
	{
		incomes.POST("", h.CreateIncome)
		incomes.GET("", h.ListIncomes)
	}

	finance := rg.Group("/finance")
	finance.Use(authMiddleware)
	{
		finance.POST("/debts", h.CreateDebt)
		finance.GET("/debts", h.ListDebts)
		finance.PUT("/debts/:id", h.UpdateDebt)
		finance.DELETE("/debts/:id", h.DeleteDebt)
		finance.GET("/cash-flow", h.GetCashFlow)
	}
}

func (h *FinanceHandler) CreateIncome(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.CreateIncomeInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	income, err := h.cashFlowUC.CreateIncome(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, income)
}

func (h *FinanceHandler) ListIncomes(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	incomes, err := h.cashFlowUC.ListIncomes(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, incomes)
}

func (h *FinanceHandler) CreateDebt(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.CreateDebtInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	debt, err := h.cashFlowUC.CreateDebt(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, debt)
}

func (h *FinanceHandler) ListDebts(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	debts, err := h.cashFlowUC.ListDebts(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, debts)
}

func (h *FinanceHandler) UpdateDebt(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	debtID, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid debt id"})
		return
	}

	var input usecase.UpdateDebtInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	debt, err := h.cashFlowUC.UpdateDebt(c.Request.Context(), userID, debtID, input)
	if err != nil {
		if errors.Is(err, usecase.ErrDebtNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, usecase.ErrUnauthorizedDebt) {
			c.JSON(http.StatusForbidden, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, debt)
}

func (h *FinanceHandler) DeleteDebt(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	debtID, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid debt id"})
		return
	}

	if err := h.cashFlowUC.DeleteDebt(c.Request.Context(), userID, debtID); err != nil {
		if errors.Is(err, usecase.ErrDebtNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, usecase.ErrUnauthorizedDebt) {
			c.JSON(http.StatusForbidden, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "recurring debt deleted successfully"})
}

func (h *FinanceHandler) GetCashFlow(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	year, _ := strconv.Atoi(c.DefaultQuery("year", strconv.Itoa(time.Now().Year())))
	month, _ := strconv.Atoi(c.DefaultQuery("month", strconv.Itoa(int(time.Now().Month()))))

	summary, err := h.cashFlowUC.CalculateCashFlow(c.Request.Context(), userID, year, month)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, summary)
}
