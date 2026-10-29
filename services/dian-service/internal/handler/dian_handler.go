package handler

import (
	"errors"
	"io"
	"net/http"

	"github.com/gin-gonic/gin"

	"dian-service/internal/model"
	"dian-service/internal/service"
)

type DianHandler struct {
	catalogSvc *service.CatalogService
}

func NewDianHandler(catalogSvc *service.CatalogService) *DianHandler {
	return &DianHandler{catalogSvc: catalogSvc}
}

func (h *DianHandler) RegisterRoutes(r *gin.Engine) {
	v1 := r.Group("/api/v1/dian")
	{
		v1.POST("/lookup", h.Lookup)
		v1.GET("/pdf/:cufe", h.DownloadPDF)
	}
}

func (h *DianHandler) Lookup(c *gin.Context) {
	var req model.LookupRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body", "details": err.Error()})
		return
	}

	query := req.QRURL
	if query == "" {
		query = req.CUFE
	}
	if query == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "either qr_url or cufe is required"})
		return
	}

	invoice, err := h.catalogSvc.Lookup(c.Request.Context(), query, req.NIT)
	if err != nil {
		if errors.Is(err, service.ErrInvalidQRURLOrCUFE) {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		if errors.Is(err, service.ErrNitRequired) {
			c.JSON(http.StatusUnprocessableEntity, gin.H{
				"error":   "NIT_REQUIRED",
				"message": "La DIAN requiere el NIT del emisor o del receptor para consultar este documento.",
			})
			return
		}
		if errors.Is(err, service.ErrDIANNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to query DIAN", "details": err.Error()})
		return
	}

	c.JSON(http.StatusOK, invoice)
}

func (h *DianHandler) DownloadPDF(c *gin.Context) {
	cufe := c.Param("cufe")
	if cufe == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "cufe is required"})
		return
	}

	nit := c.Query("nit")
	stream, size, err := h.catalogSvc.DownloadPDF(c.Request.Context(), cufe, nit)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to download PDF from DIAN", "details": err.Error()})
		return
	}
	defer stream.Close()

	c.Header("Content-Type", "application/pdf")
	c.Header("Content-Disposition", "attachment; filename=\"factura_"+cufe[:8]+".pdf\"")
	if size > 0 {
		c.Header("Content-Length", string(rune(size)))
	}

	io.Copy(c.Writer, stream)
}
