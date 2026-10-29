package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/core/usecase"
)

type ReconciliationHandler struct {
	reconUC *usecase.ReconciliationUseCase
}

func NewReconciliationHandler(reconUC *usecase.ReconciliationUseCase) *ReconciliationHandler {
	return &ReconciliationHandler{reconUC: reconUC}
}

func (h *ReconciliationHandler) RegisterRoutes(rg *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	recon := rg.Group("/reconciliation")
	recon.Use(authMiddleware)
	{
		recon.POST("/upload-statement", h.UploadStatement)
		recon.GET("/duplicates", h.GetDuplicates)
		recon.POST("/link", h.LinkTransaction)
		recon.POST("/dismiss", h.DismissTransaction)
	}
}

func (h *ReconciliationHandler) UploadStatement(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	file, err := c.FormFile("file")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "file is required (form-data key 'file')"})
		return
	}

	bankName := c.PostForm("bank_name")
	accountLastFour := c.PostForm("account_last_four")

	src, err := file.Open()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to open uploaded file"})
		return
	}
	defer src.Close()

	result, err := h.reconUC.UploadStatement(c.Request.Context(), userID, bankName, accountLastFour, src, file.Filename)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, result)
}

func (h *ReconciliationHandler) GetDuplicates(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	duplicates, err := h.reconUC.GetSuspectedDuplicates(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, duplicates)
}

type LinkTransactionInput struct {
	TransactionID uuid.UUID `json:"transaction_id" binding:"required"`
	ExpenseID     uuid.UUID `json:"expense_id" binding:"required"`
}

func (h *ReconciliationHandler) LinkTransaction(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input LinkTransactionInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if err := h.reconUC.LinkTransaction(c.Request.Context(), userID, input.TransactionID, input.ExpenseID); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "transaction linked successfully"})
}

type DismissTransactionInput struct {
	TransactionID uuid.UUID `json:"transaction_id" binding:"required"`
}

func (h *ReconciliationHandler) DismissTransaction(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input DismissTransactionInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if err := h.reconUC.DismissDuplicate(c.Request.Context(), userID, input.TransactionID); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "duplicate dismissed successfully"})
}
