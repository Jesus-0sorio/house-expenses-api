package repository

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type GormHouseholdRepository struct {
	db *gorm.DB
}

func NewHouseholdRepository(db *gorm.DB) ports.HouseholdRepository {
	return &GormHouseholdRepository{db: db}
}

func (r *GormHouseholdRepository) Create(ctx context.Context, household *domain.Household) error {
	if household.ID == uuid.Nil {
		household.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Create(household).Error
}

func (r *GormHouseholdRepository) FindByID(ctx context.Context, id uuid.UUID) (*domain.Household, error) {
	var h domain.Household
	err := r.db.WithContext(ctx).Preload("Members.User").First(&h, "id = ?", id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &h, nil
}

func (r *GormHouseholdRepository) FindByInviteCode(ctx context.Context, code string) (*domain.Household, error) {
	var h domain.Household
	err := r.db.WithContext(ctx).Preload("Members.User").First(&h, "invite_code = ?", code).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &h, nil
}

func (r *GormHouseholdRepository) Update(ctx context.Context, household *domain.Household) error {
	return r.db.WithContext(ctx).Save(household).Error
}

func (r *GormHouseholdRepository) Delete(ctx context.Context, id uuid.UUID) error {
	// Limpiar active_household_id en users
	r.db.WithContext(ctx).Model(&domain.User{}).Where("active_household_id = ?", id).Update("active_household_id", nil)
	// Eliminar miembros del hogar
	r.db.WithContext(ctx).Where("household_id = ?", id).Delete(&domain.HouseholdMember{})
	// Eliminar gastos asociados al hogar (o desasociarlos)
	r.db.WithContext(ctx).Model(&domain.Expense{}).Where("household_id = ?", id).Update("household_id", nil)
	// Eliminar el hogar
	return r.db.WithContext(ctx).Delete(&domain.Household{}, "id = ?", id).Error
}

func (r *GormHouseholdRepository) UpdateMember(ctx context.Context, member *domain.HouseholdMember) error {
	return r.db.WithContext(ctx).Save(member).Error
}

func (r *GormHouseholdRepository) RemoveMember(ctx context.Context, householdID, userID uuid.UUID) error {
	r.db.WithContext(ctx).Model(&domain.User{}).Where("id = ? AND active_household_id = ?", userID, householdID).Update("active_household_id", nil)
	return r.db.WithContext(ctx).Where("household_id = ? AND user_id = ?", householdID, userID).Delete(&domain.HouseholdMember{}).Error
}

func (r *GormHouseholdRepository) AddMember(ctx context.Context, member *domain.HouseholdMember) error {
	if member.ID == uuid.Nil {
		member.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Create(member).Error
}

func (r *GormHouseholdRepository) GetMembers(ctx context.Context, householdID uuid.UUID) ([]domain.HouseholdMember, error) {
	var members []domain.HouseholdMember
	err := r.db.WithContext(ctx).Preload("User").Find(&members, "household_id = ?", householdID).Error
	return members, err
}

func (r *GormHouseholdRepository) FindMember(ctx context.Context, householdID, userID uuid.UUID) (*domain.HouseholdMember, error) {
	var member domain.HouseholdMember
	err := r.db.WithContext(ctx).First(&member, "household_id = ? AND user_id = ?", householdID, userID).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &member, nil
}

func (r *GormHouseholdRepository) CreatePayment(ctx context.Context, payment *domain.SettlementPayment) error {
	if payment.ID == uuid.Nil {
		payment.ID = uuid.New()
	}
	return r.db.WithContext(ctx).Create(payment).Error
}

func (r *GormHouseholdRepository) FindPaymentByID(ctx context.Context, id uuid.UUID) (*domain.SettlementPayment, error) {
	var payment domain.SettlementPayment
	err := r.db.WithContext(ctx).Preload("Payer").Preload("Receiver").First(&payment, "id = ?", id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &payment, nil
}

func (r *GormHouseholdRepository) GetPaymentsByHouseholdAndPeriod(ctx context.Context, householdID uuid.UUID, start, end time.Time) ([]domain.SettlementPayment, error) {
	var payments []domain.SettlementPayment
	err := r.db.WithContext(ctx).Preload("Payer").Preload("Receiver").Where("household_id = ? AND date >= ? AND date <= ?", householdID, start, end).Order("date DESC, created_at DESC").Find(&payments).Error
	return payments, err
}

func (r *GormHouseholdRepository) GetPaymentsByHousehold(ctx context.Context, householdID uuid.UUID, limit, offset int) ([]domain.SettlementPayment, error) {
	if limit <= 0 {
		limit = 50
	}
	var payments []domain.SettlementPayment
	err := r.db.WithContext(ctx).Preload("Payer").Preload("Receiver").Where("household_id = ?", householdID).Order("date DESC, created_at DESC").Limit(limit).Offset(offset).Find(&payments).Error
	return payments, err
}
