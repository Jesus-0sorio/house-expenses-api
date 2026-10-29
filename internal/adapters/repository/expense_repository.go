package repository

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type GormExpenseRepository struct {
	db *gorm.DB
}

func NewExpenseRepository(db *gorm.DB) ports.ExpenseRepository {
	return &GormExpenseRepository{db: db}
}

func (r *GormExpenseRepository) Create(ctx context.Context, expense *domain.Expense) error {
	if expense.ID == uuid.Nil {
		expense.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Create(expense).Error
}

func (r *GormExpenseRepository) FindByID(ctx context.Context, id uuid.UUID) (*domain.Expense, error) {
	var exp domain.Expense
	err := r.db.WithContext(ctx).Preload("Invoice.Items").Preload("User").First(&exp, "id = ?", id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &exp, nil
}

func (r *GormExpenseRepository) FindByUser(ctx context.Context, userID uuid.UUID, limit, offset int) ([]domain.Expense, error) {
	if limit <= 0 {
		limit = 50
	}
	var expenses []domain.Expense
	err := r.db.WithContext(ctx).Preload("Invoice").Where("user_id = ?", userID).Order("date DESC").Limit(limit).Offset(offset).Find(&expenses).Error
	return expenses, err
}

func (r *GormExpenseRepository) FindByHousehold(ctx context.Context, householdID uuid.UUID, limit, offset int) ([]domain.Expense, error) {
	if limit <= 0 {
		limit = 50
	}
	var expenses []domain.Expense
	err := r.db.WithContext(ctx).Preload("Invoice").Preload("User").Where("household_id = ?", householdID).Order("date DESC, created_at DESC").Limit(limit).Offset(offset).Find(&expenses).Error
	return expenses, err
}

func (r *GormExpenseRepository) FindByUserOrHousehold(ctx context.Context, userID uuid.UUID, householdID *uuid.UUID, limit, offset int) ([]domain.Expense, error) {
	if limit <= 0 {
		limit = 50
	}
	var expenses []domain.Expense
	query := r.db.WithContext(ctx).Preload("Invoice").Preload("User")
	if householdID != nil && *householdID != uuid.Nil {
		query = query.Where("user_id = ? OR (scope = ? AND household_id = ?)", userID, domain.ExpenseScopeHousehold, *householdID)
	} else {
		query = query.Where("user_id = ?", userID)
	}
	err := query.Order("date DESC, created_at DESC").Limit(limit).Offset(offset).Find(&expenses).Error
	return expenses, err
}

func (r *GormExpenseRepository) FindByUserAndPeriod(ctx context.Context, userID uuid.UUID, start, end time.Time) ([]domain.Expense, error) {
	var expenses []domain.Expense
	err := r.db.WithContext(ctx).Preload("Invoice").Where("user_id = ? AND date >= ? AND date <= ?", userID, start, end).Order("date DESC").Find(&expenses).Error
	return expenses, err
}

func (r *GormExpenseRepository) FindByHouseholdAndPeriod(ctx context.Context, householdID uuid.UUID, start, end time.Time) ([]domain.Expense, error) {
	var expenses []domain.Expense
	err := r.db.WithContext(ctx).Preload("Invoice").Preload("User").Where("household_id = ? AND date >= ? AND date <= ?", householdID, start, end).Order("date DESC").Find(&expenses).Error
	return expenses, err
}

func (r *GormExpenseRepository) Update(ctx context.Context, expense *domain.Expense) error {
	return r.db.WithContext(ctx).Save(expense).Error
}

func (r *GormExpenseRepository) Delete(ctx context.Context, id uuid.UUID) error {
	return r.db.WithContext(ctx).Delete(&domain.Expense{}, "id = ?", id).Error
}

func (r *GormExpenseRepository) FindClassificationRule(ctx context.Context, userID uuid.UUID, merchant string) (*domain.ClassificationRule, error) {
	var rules []domain.ClassificationRule
	err := r.db.WithContext(ctx).Where("user_id = ?", userID).Find(&rules).Error
	if err != nil {
		return nil, err
	}

	merchantLower := strings.ToLower(merchant)
	for _, rule := range rules {
		if strings.Contains(merchantLower, strings.ToLower(rule.MerchantPattern)) {
			return &rule, nil
		}
	}
	return nil, nil
}

func (r *GormExpenseRepository) SaveClassificationRule(ctx context.Context, rule *domain.ClassificationRule) error {
	if rule.ID == uuid.Nil {
		rule.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Save(rule).Error
}
