package repository

import (
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	"github.com/glebarez/sqlite"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"house-expenses/internal/core/domain"
)

type DatabaseConfig struct {
	Driver   string
	DSN      string
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
	SSLMode  string
}

func Connect(cfg *DatabaseConfig) (*gorm.DB, error) {
	var dialector gorm.Dialector

	gormLogLevel := logger.Warn
	if os.Getenv("DEBUG") == "true" {
		gormLogLevel = logger.Info
	}

	gormConfig := &gorm.Config{
		Logger: logger.Default.LogMode(gormLogLevel),
	}

	switch cfg.Driver {
	case "postgres":
		dsn := cfg.DSN
		// If DSN is not explicitly a postgres URI or key-value connection string, build from parameters
		if !strings.HasPrefix(dsn, "postgres://") && !strings.HasPrefix(dsn, "postgresql://") && !strings.Contains(dsn, "host=") {
			sslMode := cfg.SSLMode
			if sslMode == "" {
				sslMode = "disable"
			}
			host := cfg.Host
			if host == "" {
				host = "postgres"
			}
			port := cfg.Port
			if port == "" {
				port = "5432"
			}
			user := cfg.User
			if user == "" {
				user = "expenses"
			}
			pass := cfg.Password
			if pass == "" {
				pass = "expenses_secret"
			}
			dbName := cfg.DBName
			if dbName == "" {
				dbName = "house_expenses"
			}
			dsn = fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
				host, port, user, pass, dbName, sslMode)
		}
		dialector = postgres.Open(dsn)

	case "sqlite":
		fallthrough
	default:
		dsn := cfg.DSN
		if dsn == "" || strings.Contains(dsn, "host=") || strings.HasPrefix(dsn, "postgres") {
			dsn = "local_expenses.db?cache=shared&mode=rwc"
		}
		dialector = sqlite.Open(dsn)
	}

	var db *gorm.DB
	var err error

	maxRetries := 1
	retryDelay := 2 * time.Second
	if cfg.Driver == "postgres" {
		maxRetries = 10 // Retry for up to 20 seconds while postgres finishes booting
	}

	for attempt := 1; attempt <= maxRetries; attempt++ {
		db, err = gorm.Open(dialector, gormConfig)
		if err == nil {
			var sqlDBErr error
			sqlDB, sqlDBErr := db.DB()
			if sqlDBErr == nil {
				if pingErr := sqlDB.Ping(); pingErr == nil {
					sqlDB.SetMaxIdleConns(10)
					sqlDB.SetMaxOpenConns(100)
					sqlDB.SetConnMaxLifetime(time.Hour)
					return db, nil
				}
			}
		}

		if attempt < maxRetries {
			log.Printf("Waiting for database (%s) to be ready (attempt %d/%d)...", cfg.Driver, attempt, maxRetries)
			time.Sleep(retryDelay)
		}
	}

	if err != nil {
		return nil, fmt.Errorf("failed to open database with driver %s after retries: %w", cfg.Driver, err)
	}

	return db, nil
}

func Migrate(db *gorm.DB) error {
	log.Println("Running database migrations for all domain entities...")
	return db.AutoMigrate(
		&domain.User{},
		&domain.UserOAuthAccount{},
		&domain.Household{},
		&domain.HouseholdMember{},
		&domain.Invoice{},
		&domain.InvoiceItem{},
		&domain.Expense{},
		&domain.ClassificationRule{},
		&domain.BankStatement{},
		&domain.BankTransaction{},
		&domain.Income{},
		&domain.RecurringDebt{},
		&domain.SettlementPayment{},
	)
}
