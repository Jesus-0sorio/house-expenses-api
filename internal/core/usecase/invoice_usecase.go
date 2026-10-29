package usecase

import (
	"context"
	"errors"
	"fmt"
	"io"
	"time"

	"github.com/google/uuid"

	"house-expenses/internal/adapters/email"
	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

var (
	ErrInvoiceNotFound = errors.New("invoice not found")
	ErrNoCUFE          = errors.New("invoice does not have a registered CUFE")
)

type InvoiceUseCase struct {
	invoiceRepo   ports.InvoiceRepository
	dianClient    ports.DianClientPort
	userRepo      ports.UserRepository
	householdRepo ports.HouseholdRepository
}

func NewInvoiceUseCase(
	invoiceRepo ports.InvoiceRepository,
	dianClient ports.DianClientPort,
	userRepo ports.UserRepository,
	householdRepo ports.HouseholdRepository,
) *InvoiceUseCase {
	return &InvoiceUseCase{
		invoiceRepo:   invoiceRepo,
		dianClient:    dianClient,
		userRepo:      userRepo,
		householdRepo: householdRepo,
	}
}

func (uc *InvoiceUseCase) ProcessDianQR(ctx context.Context, userID uuid.UUID, qrURL string, nit string) (*domain.Invoice, error) {
	// If no NIT was provided explicitly, check if user has a TaxID (cedula/NIT) configured
	if nit == "" {
		if user, err := uc.userRepo.FindByID(ctx, userID); err == nil && user != nil && user.TaxID != nil {
			nit = *user.TaxID
		}
	}

	// Call autonomous DIAN microservice
	dto, err := uc.dianClient.LookupInvoice(ctx, qrURL, nit)
	if err != nil {
		return nil, fmt.Errorf("failed to lookup invoice via DIAN microservice: %w", err)
	}

	// Idempotency check: see if invoice already registered
	if dto.CUFE != "" {
		existing, err := uc.invoiceRepo.FindByCUFE(ctx, dto.CUFE)
		if err == nil && existing != nil {
			return existing, nil
		}
	}

	invoiceID := uuid.New()
	invoice := &domain.Invoice{
		ID:            invoiceID,
		CUFE:          &dto.CUFE,
		IssuerName:    dto.IssuerName,
		IssuerNIT:     dto.IssuerNIT,
		InvoiceNumber: dto.InvoiceNumber,
		IssueDate:     dto.IssueDate,
		TotalAmount:   dto.TotalAmount,
		TaxAmount:     dto.TaxAmount,
		SourceType:    domain.InvoiceSourceQRDian,
		CreatedAt:     time.Now(),
	}

	var items []domain.InvoiceItem
	for _, itemDTO := range dto.Items {
		items = append(items, domain.InvoiceItem{
			ID:             uuid.New(),
			InvoiceID:      invoiceID,
			Description:    itemDTO.Description,
			Quantity:       itemDTO.Quantity,
			UnitPrice:      itemDTO.UnitPrice,
			TotalPrice:     itemDTO.TotalPrice,
			Classification: domain.ClassificationHousehold,
		})
	}
	invoice.Items = items

	if err := uc.invoiceRepo.Create(ctx, invoice); err != nil {
		return nil, fmt.Errorf("failed to persist invoice: %w", err)
	}

	return invoice, nil
}

func (uc *InvoiceUseCase) DownloadInvoicePDF(ctx context.Context, invoiceID uuid.UUID, nit string) (io.ReadCloser, error) {
	invoice, err := uc.invoiceRepo.FindByID(ctx, invoiceID)
	if err != nil {
		return nil, err
	}
	if invoice == nil {
		return nil, ErrInvoiceNotFound
	}
	if invoice.CUFE == nil || *invoice.CUFE == "" {
		return nil, ErrNoCUFE
	}

	return uc.dianClient.DownloadPDF(ctx, *invoice.CUFE, nit)
}

func (uc *InvoiceUseCase) ProcessZipUpload(ctx context.Context, userID uuid.UUID, zipBytes []byte) (*domain.Invoice, error) {
	result, err := email.ParseZipAttachment(zipBytes)
	if err != nil {
		return nil, fmt.Errorf("failed to parse invoice zip: %w", err)
	}

	invoice := result.Invoice
	invoice.ID = uuid.New()

	if invoice.CUFE != nil && *invoice.CUFE != "" {
		existing, err := uc.invoiceRepo.FindByCUFE(ctx, *invoice.CUFE)
		if err == nil && existing != nil {
			return existing, nil
		}
	}

	if err := uc.invoiceRepo.Create(ctx, invoice); err != nil {
		return nil, fmt.Errorf("failed to save invoice from zip: %w", err)
	}

	return invoice, nil
}
