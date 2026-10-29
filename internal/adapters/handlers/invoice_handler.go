package handlers

import (
	"errors"
	"io"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"house-expenses/internal/adapters/dian_client"
	"house-expenses/internal/core/usecase"
)

type InvoiceHandler struct {
	invoiceUC *usecase.InvoiceUseCase
}

func NewInvoiceHandler(invoiceUC *usecase.InvoiceUseCase) *InvoiceHandler {
	return &InvoiceHandler{invoiceUC: invoiceUC}
}

func (h *InvoiceHandler) RegisterRoutes(rg *gin.RouterGroup, authMiddleware gin.HandlerFunc) {
	invoices := rg.Group("/invoices")
	invoices.Use(authMiddleware)
	{
		invoices.POST("/dian-qr", h.LookupDianQR)
		invoices.GET("/:id/pdf", h.DownloadPDF)
		invoices.POST("/upload-zip", h.UploadZip)
	}
}

type DianQRRequest struct {
	QRURL string `json:"qr_url" binding:"required"`
	NIT   string `json:"nit"` // Opcional: NIT del emisor o receptor (por defecto usa el TaxID del usuario)
}

func (h *InvoiceHandler) LookupDianQR(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	var req DianQRRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "qr_url is required"})
		return
	}

	invoice, err := h.invoiceUC.ProcessDianQR(c.Request.Context(), userID, req.QRURL, req.NIT)
	if err != nil {
		if errors.Is(err, dian_client.ErrDianNitRequired) {
			c.JSON(http.StatusUnprocessableEntity, gin.H{
				"error":   "NIT_REQUIRED",
				"message": "La DIAN requiere el NIT del emisor o del receptor (tu cédula o NIT) para consultar esta factura. Por favor proporciona el campo 'nit' o regístralo en tu perfil.",
			})
			return
		}
		if errors.Is(err, dian_client.ErrDianServiceTimeout) {
			c.JSON(http.StatusGatewayTimeout, gin.H{"error": "DIAN service timed out, please retry later", "details": err.Error()})
			return
		}
		if errors.Is(err, dian_client.ErrDianServiceUnavailable) {
			c.JSON(http.StatusBadGateway, gin.H{"error": "DIAN service is unreachable", "details": err.Error()})
			return
		}
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, invoice)
}

func (h *InvoiceHandler) DownloadPDF(c *gin.Context) {
	idParam := c.Param("id")
	invoiceID, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid invoice ID"})
		return
	}

	nit := c.Query("nit")
	stream, err := h.invoiceUC.DownloadInvoicePDF(c.Request.Context(), invoiceID, nit)
	if err != nil {
		if errors.Is(err, usecase.ErrInvoiceNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, dian_client.ErrDianServiceUnavailable) {
			c.JSON(http.StatusBadGateway, gin.H{"error": "DIAN service unavailable to fetch PDF"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	defer stream.Close()

	c.Header("Content-Type", "application/pdf")
	c.Header("Content-Disposition", "attachment; filename=\"factura_"+idParam[:8]+".pdf\"")
	io.Copy(c.Writer, stream)
}

func (h *InvoiceHandler) UploadZip(c *gin.Context) {
	userIDVal, exists := c.Get(ContextKeyUserID)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not authenticated"})
		return
	}
	userID := userIDVal.(uuid.UUID)

	fileHeader, err := c.FormFile("file")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "file field is required in multipart form"})
		return
	}

	file, err := fileHeader.Open()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to open uploaded file"})
		return
	}
	defer file.Close()

	fileBytes, err := io.ReadAll(file)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to read file bytes"})
		return
	}

	invoice, err := h.invoiceUC.ProcessZipUpload(c.Request.Context(), userID, fileBytes)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, invoice)
}
