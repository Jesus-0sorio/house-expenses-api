package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/core/usecase"
)

type SyncHandler struct {
	syncUC *usecase.SyncUseCase
}

func NewSyncHandler(syncUC *usecase.SyncUseCase) *SyncHandler {
	return &SyncHandler{syncUC: syncUC}
}

func (h *SyncHandler) RegisterRoutes(rg *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	rg.POST("/sync", authMiddleware, h.SyncBatch)
}

func (h *SyncHandler) SyncBatch(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.SyncBatchInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	result, err := h.syncUC.SyncBatch(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, result)
}
