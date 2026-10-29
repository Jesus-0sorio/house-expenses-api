package handlers

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/core/usecase"
)

type HouseholdHandler struct {
	householdUC *usecase.HouseholdUseCase
}

func NewHouseholdHandler(householdUC *usecase.HouseholdUseCase) *HouseholdHandler {
	return &HouseholdHandler{householdUC: householdUC}
}

func (h *HouseholdHandler) RegisterRoutes(rg *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	householdGroup := rg.Group("/households")
	householdGroup.Use(authMiddleware)
	{
		householdGroup.POST("", h.CreateHousehold)
		householdGroup.DELETE("", h.DeleteHousehold)
		householdGroup.POST("/join", h.JoinHousehold)
		householdGroup.POST("/leave", h.LeaveHousehold)
		householdGroup.GET("/current", h.GetCurrentHousehold)
		householdGroup.GET("/mode", h.GetAdaptiveMode)
		householdGroup.GET("/members", h.GetMembers)
		householdGroup.DELETE("/members/:user_id", h.RemoveMember)
		householdGroup.PUT("/members/splits", h.UpdateMemberSplits)
		householdGroup.PUT("/splits", h.UpdateMemberSplits)
		householdGroup.PUT("/cycle", h.UpdateBillingCycle)
		householdGroup.PUT("/billing-cycle", h.UpdateBillingCycle)
		householdGroup.GET("/cycles", h.GetCyclesHistory)
		householdGroup.GET("/cycles/:cycle_index", h.GetCycleDetail)
		householdGroup.POST("/payments", h.CreateSettlementPayment)
		householdGroup.GET("/payments", h.GetSettlementPayments)
	}
}

func (h *HouseholdHandler) CreateHousehold(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.CreateHouseholdInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	household, err := h.householdUC.CreateHousehold(c.Request.Context(), userID, input)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, household)
}

func (h *HouseholdHandler) JoinHousehold(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.JoinHouseholdInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	household, err := h.householdUC.JoinHousehold(c.Request.Context(), userID, input)
	if err != nil {
		if errors.Is(err, usecase.ErrHouseholdNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, usecase.ErrAlreadyHouseholdMember) {
			c.JSON(http.StatusConflict, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, household)
}

func (h *HouseholdHandler) DeleteHousehold(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	err := h.householdUC.DeleteHousehold(c.Request.Context(), userID)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, usecase.ErrUnauthorizedHousehold) {
			c.JSON(http.StatusForbidden, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Hogar eliminado exitosamente"})
}

func (h *HouseholdHandler) LeaveHousehold(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	err := h.householdUC.LeaveHousehold(c.Request.Context(), userID)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Has abandonado el hogar exitosamente"})
}

func (h *HouseholdHandler) UpdateMemberSplits(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input []usecase.MemberSplitInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	err := h.householdUC.UpdateMemberSplits(c.Request.Context(), userID, input)
	if err != nil {
		if errors.Is(err, usecase.ErrInvalidSplitSum) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, usecase.ErrUnauthorizedHousehold) {
			c.JSON(http.StatusForbidden, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Porcentajes de repartición actualizados correctamente"})
}

func (h *HouseholdHandler) UpdateBillingCycle(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.UpdateBillingCycleInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	household, err := h.householdUC.UpdateBillingCycle(c.Request.Context(), userID, input.BillingCycleDay)
	if err != nil {
		if errors.Is(err, usecase.ErrUnauthorizedHousehold) {
			c.JSON(http.StatusForbidden, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, household)
}

func (h *HouseholdHandler) GetCyclesHistory(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	cycles, err := h.householdUC.GetCyclesHistory(c.Request.Context(), userID)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, cycles)
}

func (h *HouseholdHandler) GetAdaptiveMode(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	modeResp, err := h.householdUC.GetAdaptiveMode(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, modeResp)
}

func (h *HouseholdHandler) GetMembers(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	members, err := h.householdUC.GetMembers(c.Request.Context(), userID)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, members)
}

func (h *HouseholdHandler) RemoveMember(c *gin.Context) {
	adminIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	adminID := adminIDVal.(uuid.UUID)

	targetIDStr := c.Param("user_id")
	targetID, err := uuid.Parse(targetIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid user ID"})
		return
	}

	if err := h.householdUC.RemoveMember(c.Request.Context(), adminID, targetID); err != nil {
		if errors.Is(err, usecase.ErrUnauthorizedHousehold) {
			c.JSON(http.StatusForbidden, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "miembro expulsado exitosamente"})
}

func (h *HouseholdHandler) GetCycleDetail(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	idxStr := c.Param("cycle_index")
	cycleIndex, err := strconv.Atoi(idxStr)
	if err != nil || cycleIndex < 0 {
		cycleIndex = 0
	}

	detail, err := h.householdUC.GetCycleDetail(c.Request.Context(), userID, cycleIndex)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, detail)
}

func (h *HouseholdHandler) GetCurrentHousehold(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	res, err := h.householdUC.GetCurrentHousehold(c.Request.Context(), userID)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, res)
}

func (h *HouseholdHandler) CreateSettlementPayment(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var input usecase.CreateSettlementPaymentInput
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	payment, err := h.householdUC.CreateSettlementPayment(c.Request.Context(), userID, input)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, payment)
}

func (h *HouseholdHandler) GetSettlementPayments(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	offset, _ := strconv.Atoi(c.DefaultQuery("offset", "0"))

	payments, err := h.householdUC.GetSettlementPayments(c.Request.Context(), userID, limit, offset)
	if err != nil {
		if errors.Is(err, usecase.ErrNoActiveHousehold) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, payments)
}

