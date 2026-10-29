package ports

import (
	"context"
	"io"
	"time"
)

type DianItemDTO struct {
	Description string  `json:"description"`
	Quantity    float64 `json:"quantity"`
	UnitPrice   float64 `json:"unit_price"`
	TotalPrice  float64 `json:"total_price"`
}

type DianInvoiceDTO struct {
	CUFE          string        `json:"cufe"`
	IssuerName    string        `json:"issuer_name"`
	IssuerNIT     string        `json:"issuer_nit"`
	InvoiceNumber string        `json:"invoice_number"`
	IssueDate     time.Time     `json:"issue_date"`
	TotalAmount   float64       `json:"total_amount"`
	TaxAmount     float64       `json:"tax_amount"`
	Items         []DianItemDTO `json:"items"`
}

type DianClientPort interface {
	LookupInvoice(ctx context.Context, qrURLOrCUFE string, nit string) (*DianInvoiceDTO, error)
	DownloadPDF(ctx context.Context, cufe string, nit string) (io.ReadCloser, error)
}
