package handlers

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/core/usecase"
)

type ExpenseHandler struct {
	expenseUC *usecase.ExpenseUseCase
}

func NewExpenseHandler(expenseUC *usecase.ExpenseUseCase) *ExpenseHandler {
	return &ExpenseHandler{expenseUC: expenseUC}
}

func (h *ExpenseHandler) RegisterRoutes(rg *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	expenses := rg.Group("/expenses")
	expenses.Use(authMiddleware)
	{
		expenses.POST("", h.CreateExpense)
		expenses.GET("", h.ListExpenses)
		expenses.POST("/rules", h.SaveRule)
	}

	invoices := rg.Group("/invoices")
	invoices.Use(authMiddleware)
	{
		invoices.POST("/:id/split-inverse", h.SplitInverse)
	}

	households := rg.Group("/households")
	households.Use(authMiddleware)
	{
		households.GET("/balance", h.GetHouseholdBalance)
	}
}

func (h *ExpenseHandler) CreateExpense(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.CreateExpenseInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	expense, err := h.expenseUC.CreateExpense(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, expense)
}

func (h *ExpenseHandler) ListExpenses(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	offset, _ := strconv.Atoi(c.DefaultQuery("offset", "0"))

	expenses, err := h.expenseUC.ListExpenses(c.Request.Context(), userID, limit, offset)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, expenses)
}

func (h *ExpenseHandler) SplitInverse(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	idParam := c.Param("id")
	invoiceID, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid invoice ID"})
		return
	}

	var input usecase.InverseSplitInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	input.InvoiceID = invoiceID

	result, err := h.expenseUC.SplitItemsInversely(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, result)
}

func (h *ExpenseHandler) SaveRule(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.SaveRuleInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	rule, err := h.expenseUC.SaveClassificationRule(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, rule)
}

func (h *ExpenseHandler) GetHouseholdBalance(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	summary, err := h.expenseUC.CalculateHouseholdBalance(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, summary)
}
