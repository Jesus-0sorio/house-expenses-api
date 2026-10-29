package main

import (
	"log"
	"net/http"
	"os"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"

	"dian-service/internal/handler"
	"dian-service/internal/service"
)

func main() {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found in dian-service, using system environment")
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	catalogBaseURL := os.Getenv("DIAN_CATALOG_BASE_URL")
	catalogSvc := service.NewCatalogService(catalogBaseURL)
	dianHandler := handler.NewDianHandler(catalogSvc)

	router := gin.Default()
	router.Use(cors.Default())

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":  "UP",
			"service": "dian-service",
		})
	})

	dianHandler.RegisterRoutes(router)

	log.Printf("DIAN Microservice starting on port %s...", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatalf("DIAN Microservice failed to run: %v", err)
	}
}
