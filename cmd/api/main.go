package main

import (
	"log"
	"net/http"
	"os"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"

	"house-expenses/internal/adapters/dian_client"
	"house-expenses/internal/adapters/handlers"
	"house-expenses/internal/adapters/oauth"
	"house-expenses/internal/adapters/repository"
	"house-expenses/internal/adapters/statement"
	"house-expenses/internal/core/usecase"
)

func main() {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using system environment variables")
	}

	dbDriver := os.Getenv("DB_DRIVER")
	if dbDriver == "" {
		dbDriver = "sqlite"
	}

	dbConfig := &repository.DatabaseConfig{
		Driver:   dbDriver,
		DSN:      os.Getenv("DB_DSN"),
		Host:     os.Getenv("DB_HOST"),
		Port:     os.Getenv("DB_PORT"),
		User:     os.Getenv("DB_USER"),
		Password: os.Getenv("DB_PASSWORD"),
		DBName:   os.Getenv("DB_NAME"),
		SSLMode:  os.Getenv("DB_SSLMODE"),
	}

	db, err := repository.Connect(dbConfig)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	if err := repository.Migrate(db); err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}
	log.Println("Database connection and migrations completed successfully!")

	// Initialize Repositories
	userRepo := repository.NewUserRepository(db)
	householdRepo := repository.NewHouseholdRepository(db)
	invoiceRepo := repository.NewInvoiceRepository(db)
	expenseRepo := repository.NewExpenseRepository(db)
	statementRepo := repository.NewBankStatementRepository(db)
	incomeRepo := repository.NewIncomeRepository(db)

	// Initialize Statement Parser
	statementParser := statement.NewMultiFormatStatementParser()

	// Initialize DIAN Microservice HTTP Client
	dianServiceURL := os.Getenv("DIAN_SERVICE_URL")
	if dianServiceURL == "" {
		dianServiceURL = "http://localhost:8081"
	}
	dianClient := dian_client.NewHTTPDianClient(dianServiceURL)

	// Initialize OAuth Registry
	oauthRegistry := oauth.NewProviderRegistry()
	googleProvider := oauth.NewGoogleOAuthProvider(oauth.GoogleConfig{
		ClientID:     os.Getenv("GOOGLE_CLIENT_ID"),
		ClientSecret: os.Getenv("GOOGLE_CLIENT_SECRET"),
		RedirectURL:  os.Getenv("GOOGLE_REDIRECT_URL"),
	})
	oauthRegistry.Register(googleProvider)

	// Initialize UseCases
	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		jwtSecret = "default_dev_secret_change_in_production"
	}
	authUC := usecase.NewAuthUseCase(userRepo, oauthRegistry, jwtSecret)
	householdUC := usecase.NewHouseholdUseCase(householdRepo, userRepo, expenseRepo)
	invoiceUC := usecase.NewInvoiceUseCase(invoiceRepo, dianClient, userRepo, householdRepo)
	expenseUC := usecase.NewExpenseUseCase(expenseRepo, invoiceRepo, userRepo, householdRepo)
	reconUC := usecase.NewReconciliationUseCase(statementRepo, expenseRepo, userRepo, statementParser)
	cashFlowUC := usecase.NewCashFlowUseCase(incomeRepo, expenseRepo, userRepo, householdRepo)
	syncUC := usecase.NewSyncUseCase(expenseRepo, incomeRepo, userRepo, householdRepo)

	// Initialize Handlers
	authHandler := handlers.NewAuthHandler(authUC, userRepo)
	householdHandler := handlers.NewHouseholdHandler(householdUC)
	invoiceHandler := handlers.NewInvoiceHandler(invoiceUC)
	expenseHandler := handlers.NewExpenseHandler(expenseUC)
	reconHandler := handlers.NewReconciliationHandler(reconUC)
	financeHandler := handlers.NewFinanceHandler(cashFlowUC)
	syncHandler := handlers.NewSyncHandler(syncUC)

	// Router setup
	router := gin.Default()
	router.Use(cors.Default())

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":   "UP",
			"service":  "house-expenses-api",
			"database": dbDriver,
		})
	})

	api := router.Group("/api/v1")
	{
		authHandler.RegisterRoutes(api)

		authMiddleware := handlers.AuthMiddleware(authUC)
		householdHandler.RegisterRoutes(api, authMiddleware)
		invoiceHandler.RegisterRoutes(api, authMiddleware)
		expenseHandler.RegisterRoutes(api, authMiddleware)
		reconHandler.RegisterRoutes(api, authMiddleware)
		financeHandler.RegisterRoutes(api, authMiddleware)
		syncHandler.RegisterRoutes(api, authMiddleware)
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("House Expenses API starting on port %s...", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatalf("Server failed to run: %v", err)
	}
}
