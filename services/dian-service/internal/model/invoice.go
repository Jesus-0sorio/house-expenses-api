package model

import "time"

type LookupRequest struct {
	QRURL string `json:"qr_url"`
	CUFE  string `json:"cufe"`
	NIT   string `json:"nit,omitempty"`
}

type DianItem struct {
	Description string  `json:"description"`
	Quantity    float64 `json:"quantity"`
	UnitPrice   float64 `json:"unit_price"`
	TotalPrice  float64 `json:"total_price"`
}

type DianInvoice struct {
	CUFE          string     `json:"cufe"`
	IssuerName    string     `json:"issuer_name"`
	IssuerNIT     string     `json:"issuer_nit"`
	InvoiceNumber string     `json:"invoice_number"`
	IssueDate     time.Time  `json:"issue_date"`
	TotalAmount   float64    `json:"total_amount"`
	TaxAmount     float64    `json:"tax_amount"`
	Items         []DianItem `json:"items"`
}
