package service_test

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"dian-service/internal/service"
)

func TestExtractCUFE(t *testing.T) {
	t.Run("Extract from full QR URL", func(t *testing.T) {
		qrURL := "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=fe0459c3817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a"
		cufe, err := service.ExtractCUFE(qrURL)
		require.NoError(t, err)
		assert.Equal(t, "fe0459c3817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a", cufe)
	})

	t.Run("Extract from raw hex string", func(t *testing.T) {
		rawHex := "fe0459c3817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a"
		cufe, err := service.ExtractCUFE(rawHex)
		require.NoError(t, err)
		assert.Equal(t, rawHex, cufe)
	})

	t.Run("Invalid input returns error", func(t *testing.T) {
		_, err := service.ExtractCUFE("invalid_string")
		assert.ErrorIs(t, err, service.ErrInvalidQRURLOrCUFE)
	})
}

func TestCatalogServiceLookupFallback(t *testing.T) {
	svc := service.NewCatalogService("http://mock-unreachable-dian")
	ctx := context.Background()

	cufe := "fe0459c3817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a"
	invoice, err := svc.Lookup(ctx, cufe, "890900608-9")
	require.NoError(t, err)
	assert.Equal(t, cufe, invoice.CUFE)
	assert.NotEmpty(t, invoice.IssuerName)
	assert.Equal(t, "890900608-9", invoice.IssuerNIT)
	assert.Greater(t, invoice.TotalAmount, 0.0)
	assert.NotEmpty(t, invoice.Items)
}

func TestCatalogServiceDownloadPDF(t *testing.T) {
	svc := service.NewCatalogService("http://mock-unreachable-dian")
	ctx := context.Background()

	cufe := "fe0459c3817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a"
	stream, size, err := svc.DownloadPDF(ctx, cufe, "")
	require.NoError(t, err)
	defer stream.Close()
	assert.Greater(t, size, int64(0))
}

func TestParseQRText_RealColombianInvoice(t *testing.T) {
	realQR := `NumFac: M17A305004
FecFac: 2026-08-11
HorFac: 14:17:56-05:00
NitFac: 900480569
DocAdq: 222222222222
ValFac: 148302.52
ValIva: 28177.48
ValOtroIm: 0.00
ValTolFac: 176480.00
CUFE: 170560100562422c17df18fd3b74d0b8b2869130277945ac641429f36c308ca708d62f6e67973f6190df5a61b9207248
https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=170560100562422c17df18fd3b74d0b8b2869130277945ac641429f36c308ca708d62f6e67973f6190df5a61b9207248`

	inv := service.ParseQRText(realQR)
	require.NotNil(t, inv)
	assert.Equal(t, "M17A305004", inv.InvoiceNumber)
	assert.Equal(t, "900480569", inv.IssuerNIT)
	assert.Equal(t, "D1 S.A.S.", inv.IssuerName)
	assert.Equal(t, 176480.00, inv.TotalAmount)
	assert.Equal(t, 28177.48, inv.TaxAmount)
	assert.Equal(t, "170560100562422c17df18fd3b74d0b8b2869130277945ac641429f36c308ca708d62f6e67973f6190df5a61b9207248", inv.CUFE)
	assert.Equal(t, 2026, inv.IssueDate.Year())
	assert.Equal(t, 8, int(inv.IssueDate.Month()))
	assert.Equal(t, 11, inv.IssueDate.Day())

	// Test Lookup via CatalogService directly with this QR text
	svc := service.NewCatalogService("http://mock-unreachable-dian")
	lookupInv, err := svc.Lookup(context.Background(), realQR, "")
	require.NoError(t, err)
	assert.Equal(t, inv.CUFE, lookupInv.CUFE)
	assert.Equal(t, 176480.00, lookupInv.TotalAmount)
	assert.Equal(t, "D1 S.A.S.", lookupInv.IssuerName)
}

