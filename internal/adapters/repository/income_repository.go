package repository

import (
	"context"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type GormIncomeRepository struct {
	db *gorm.DB
}

func NewIncomeRepository(db *gorm.DB) ports.IncomeRepository {
	return &GormIncomeRepository{db: db}
}

func (r *GormIncomeRepository) CreateIncome(ctx context.Context, income *domain.Income) error {
	if income.ID == uuid.Nil {
		income.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Create(income).Error
}

func (r *GormIncomeRepository) FindIncomeByID(ctx context.Context, id uuid.UUID) (*domain.Income, error) {
	var income domain.Income
	err := r.db.WithContext(ctx).First(&income, "id = ?", id).Error
	if err != nil {
		return nil, err
	}
	return &income, nil
}

func (r *GormIncomeRepository) ListIncomesByUser(ctx context.Context, userID uuid.UUID) ([]domain.Income, error) {
	var incomes []domain.Income
	err := r.db.WithContext(ctx).
		Where("user_id = ?", userID).
		Order("date DESC").
		Find(&incomes).Error
	return incomes, err
}

func (r *GormIncomeRepository) ListIncomesByUserAndPeriod(ctx context.Context, userID uuid.UUID, start, end time.Time) ([]domain.Income, error) {
	var incomes []domain.Income
	err := r.db.WithContext(ctx).
		Where("user_id = ? AND date >= ? AND date <= ?", userID, start, end).
		Order("date DESC").
		Find(&incomes).Error
	return incomes, err
}

func (r *GormIncomeRepository) CreateDebt(ctx context.Context, debt *domain.RecurringDebt) error {
	if debt.ID == uuid.Nil {
		debt.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Create(debt).Error
}

func (r *GormIncomeRepository) FindDebtByID(ctx context.Context, id uuid.UUID) (*domain.RecurringDebt, error) {
	var debt domain.RecurringDebt
	err := r.db.WithContext(ctx).First(&debt, "id = ?", id).Error
	if err != nil {
		return nil, err
	}
	return &debt, nil
}

func (r *GormIncomeRepository) ListDebtsByUser(ctx context.Context, userID uuid.UUID) ([]domain.RecurringDebt, error) {
	var debts []domain.RecurringDebt
	err := r.db.WithContext(ctx).
		Where("user_id = ?", userID).
		Order("due_day ASC").
		Find(&debts).Error
	return debts, err
}

func (r *GormIncomeRepository) UpdateDebt(ctx context.Context, debt *domain.RecurringDebt) error {
	return r.db.WithContext(ctx).Save(debt).Error
}

func (r *GormIncomeRepository) DeleteDebt(ctx context.Context, id uuid.UUID) error {
	return r.db.WithContext(ctx).Delete(&domain.RecurringDebt{}, "id = ?", id).Error
}
