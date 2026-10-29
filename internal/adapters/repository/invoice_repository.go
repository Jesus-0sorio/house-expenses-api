package repository

import (
	"context"
	"errors"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type GormInvoiceRepository struct {
	db *gorm.DB
}

func NewInvoiceRepository(db *gorm.DB) ports.InvoiceRepository {
	return &GormInvoiceRepository{db: db}
}

func (r *GormInvoiceRepository) Create(ctx context.Context, invoice *domain.Invoice) error {
	if invoice.ID == uuid.Nil {
		invoice.ID = uuid.New()
	}
	for i := range invoice.Items {
		if invoice.Items[i].ID == uuid.Nil {
			invoice.Items[i].ID = uuid.New()
		}
		invoice.Items[i].InvoiceID = invoice.ID
	}
	return r.db.WithContext(ctx).Create(invoice).Error
}

func (r *GormInvoiceRepository) FindByID(ctx context.Context, id uuid.UUID) (*domain.Invoice, error) {
	var inv domain.Invoice
	err := r.db.WithContext(ctx).Preload("Items").First(&inv, "id = ?", id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &inv, nil
}

func (r *GormInvoiceRepository) FindByCUFE(ctx context.Context, cufe string) (*domain.Invoice, error) {
	var inv domain.Invoice
	err := r.db.WithContext(ctx).Preload("Items").First(&inv, "cufe = ?", cufe).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &inv, nil
}

func (r *GormInvoiceRepository) Update(ctx context.Context, invoice *domain.Invoice) error {
	return r.db.WithContext(ctx).Save(invoice).Error
}

func (r *GormInvoiceRepository) UpdateItems(ctx context.Context, items []domain.InvoiceItem) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		for _, item := range items {
			if err := tx.Save(&item).Error; err != nil {
				return err
			}
		}
		return nil
	})
}
