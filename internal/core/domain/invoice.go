package domain

import (
	"time"

	"github.com/google/uuid"
)

type InvoiceSourceType string

const (
	InvoiceSourceEmailZip InvoiceSourceType = "EMAIL_ZIP"
	InvoiceSourceQRDian   InvoiceSourceType = "QR_DIAN"
	InvoiceSourceManual   InvoiceSourceType = "MANUAL"
)

type ItemClassification string

const (
	ClassificationPersonal  ItemClassification = "PERSONAL"
	ClassificationHousehold ItemClassification = "HOUSEHOLD"
)

type Invoice struct {
	ID             uuid.UUID         `json:"id" gorm:"type:uuid;primaryKey"`
	CUFE           *string           `json:"cufe,omitempty" gorm:"type:varchar(255);uniqueIndex"`
	IssuerName     string            `json:"issuer_name" gorm:"type:varchar(255);not null"`
	IssuerNIT      string            `json:"issuer_nit" gorm:"type:varchar(50);not null;index"`
	InvoiceNumber  string            `json:"invoice_number" gorm:"type:varchar(100);not null"`
	IssueDate      time.Time         `json:"issue_date" gorm:"not null"`
	TotalAmount    float64           `json:"total_amount" gorm:"type:decimal(15,2);not null"`
	TaxAmount      float64           `json:"tax_amount" gorm:"type:decimal(15,2);default:0"`
	PDFStoragePath *string           `json:"pdf_storage_path,omitempty" gorm:"type:text"`
	SourceType     InvoiceSourceType `json:"source_type" gorm:"type:varchar(50);not null"`
	Items          []InvoiceItem     `json:"items,omitempty" gorm:"foreignKey:InvoiceID"`
	CreatedAt      time.Time         `json:"created_at"`
}

type InvoiceItem struct {
	ID             uuid.UUID          `json:"id" gorm:"type:uuid;primaryKey"`
	InvoiceID      uuid.UUID          `json:"invoice_id" gorm:"type:uuid;not null;index"`
	Description    string             `json:"description" gorm:"type:varchar(500);not null"`
	Quantity       float64            `json:"quantity" gorm:"type:decimal(10,3);default:1"`
	UnitPrice      float64            `json:"unit_price" gorm:"type:decimal(15,2);not null"`
	TotalPrice     float64            `json:"total_price" gorm:"type:decimal(15,2);not null"`
	Classification ItemClassification `json:"classification" gorm:"type:varchar(50);default:'HOUSEHOLD'"`
}
