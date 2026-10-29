package repository

import (
	"context"
	"errors"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type BankStatementRepository struct {
	db *gorm.DB
}

func NewBankStatementRepository(db *gorm.DB) ports.BankStatementRepository {
	return &BankStatementRepository{db: db}
}

func (r *BankStatementRepository) Create(ctx context.Context, statement *domain.BankStatement) error {
	return r.db.WithContext(ctx).Create(statement).Error
}

func (r *BankStatementRepository) FindByID(ctx context.Context, id uuid.UUID) (*domain.BankStatement, error) {
	var statement domain.BankStatement
	err := r.db.WithContext(ctx).Preload("Transactions").First(&statement, "id = ?", id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &statement, nil
}

func (r *BankStatementRepository) ListTransactionsByStatement(ctx context.Context, statementID uuid.UUID) ([]domain.BankTransaction, error) {
	var txs []domain.BankTransaction
	err := r.db.WithContext(ctx).
		Where("statement_id = ?", statementID).
		Order("transaction_date DESC").
		Find(&txs).Error
	return txs, err
}

func (r *BankStatementRepository) FindSuspectedDuplicates(ctx context.Context, userID uuid.UUID) ([]domain.BankTransaction, error) {
	var txs []domain.BankTransaction
	err := r.db.WithContext(ctx).
		Joins("JOIN bank_statements ON bank_statements.id = bank_transactions.statement_id").
		Where("bank_statements.user_id = ? AND bank_transactions.duplicate_status = ?", userID, domain.DuplicateStatusSuspected).
		Order("bank_transactions.transaction_date DESC").
		Find(&txs).Error
	return txs, err
}

func (r *BankStatementRepository) FindTransactionByID(ctx context.Context, id uuid.UUID) (*domain.BankTransaction, error) {
	var tx domain.BankTransaction
	err := r.db.WithContext(ctx).First(&tx, "id = ?", id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &tx, nil
}

func (r *BankStatementRepository) UpdateTransaction(ctx context.Context, tx *domain.BankTransaction) error {
	return r.db.WithContext(ctx).Save(tx).Error
}
