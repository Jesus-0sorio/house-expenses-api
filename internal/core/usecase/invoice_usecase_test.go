package usecase_test

import (
	"archive/zip"
	"bytes"
	"context"
	"io"
	"strings"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"house-expenses/internal/adapters/repository"
	"house-expenses/internal/core/ports"
	"house-expenses/internal/core/usecase"
)

type mockDianClient struct {
	lookupFn      func(ctx context.Context, qr string, nit string) (*ports.DianInvoiceDTO, error)
	downloadPDFFn func(ctx context.Context, cufe string, nit string) (io.ReadCloser, error)
}

func (m *mockDianClient) LookupInvoice(ctx context.Context, qr string, nit string) (*ports.DianInvoiceDTO, error) {
	return m.lookupFn(ctx, qr, nit)
}

func (m *mockDianClient) DownloadPDF(ctx context.Context, cufe string, nit string) (io.ReadCloser, error) {
	return m.downloadPDFFn(ctx, cufe, nit)
}

func TestInvoiceUseCase(t *testing.T) {
	db := setupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	householdRepo := repository.NewHouseholdRepository(db)
	invoiceRepo := repository.NewInvoiceRepository(db)
	ctx := context.Background()

	testCUFE := "fe0459c3817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a"

	var capturedNIT string
	mockClient := &mockDianClient{
		lookupFn: func(ctx context.Context, qr string, nit string) (*ports.DianInvoiceDTO, error) {
			capturedNIT = nit
			return &ports.DianInvoiceDTO{
				CUFE:          testCUFE,
				IssuerName:    "Almacenes Éxito S.A.",
				IssuerNIT:     "890900608-9",
				InvoiceNumber: "SETP-99214",
				IssueDate:     time.Now(),
				TotalAmount:   85000.0,
				TaxAmount:     13500.0,
				Items: []ports.DianItemDTO{
					{
						Description: "Arroz Diana 5kg",
						Quantity:    1,
						UnitPrice:   22000.0,
						TotalPrice:  22000.0,
					},
					{
						Description: "Aceite Premier 1000ml",
						Quantity:    2,
						UnitPrice:   18000.0,
						TotalPrice:  36000.0,
					},
				},
			}, nil
		},
		downloadPDFFn: func(ctx context.Context, cufe string, nit string) (io.ReadCloser, error) {
			return io.NopCloser(strings.NewReader("%PDF-1.4 dummy pdf content")), nil
		},
	}

	invoiceUC := usecase.NewInvoiceUseCase(invoiceRepo, mockClient, userRepo, householdRepo)
	userID := uuid.New()

	t.Run("ProcessDianQR creates invoice with items and persists", func(t *testing.T) {
		qrURL := "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + testCUFE
		inv, err := invoiceUC.ProcessDianQR(ctx, userID, qrURL, "890900608-9")
		require.NoError(t, err)
		assert.Equal(t, testCUFE, *inv.CUFE)
		assert.Equal(t, "Almacenes Éxito S.A.", inv.IssuerName)
		assert.Equal(t, 85000.0, inv.TotalAmount)
		assert.Len(t, inv.Items, 2)
		assert.Equal(t, "890900608-9", capturedNIT)

		// Verify persisted in repository
		found, err := invoiceRepo.FindByCUFE(ctx, testCUFE)
		require.NoError(t, err)
		require.NotNil(t, found)
		assert.Equal(t, inv.ID, found.ID)
		assert.Len(t, found.Items, 2)
	})

	t.Run("ProcessDianQR is idempotent when called with same CUFE", func(t *testing.T) {
		qrURL := "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + testCUFE
		inv2, err := invoiceUC.ProcessDianQR(ctx, userID, qrURL, "")
		require.NoError(t, err)
		assert.Equal(t, testCUFE, *inv2.CUFE)
	})

	t.Run("DownloadInvoicePDF streams PDF content from DIAN", func(t *testing.T) {
		inv, err := invoiceRepo.FindByCUFE(ctx, testCUFE)
		require.NoError(t, err)

		stream, err := invoiceUC.DownloadInvoicePDF(ctx, inv.ID, "")
		require.NoError(t, err)
		defer stream.Close()

		content, err := io.ReadAll(stream)
		require.NoError(t, err)
		assert.Contains(t, string(content), "%PDF-1.4")
	})

	t.Run("ProcessZipUpload parses and saves XML invoice", func(t *testing.T) {
		xmlSample := `<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2">
    <UUID>99999999817f54c86b36be9f390082f42a59a721c0ad52c7104b7f80874e4c9c1b3e8c9735d6bb36c3e72239d10e054a</UUID>
    <ID>FEV-1020</ID>
    <IssueDate>2026-09-01</IssueDate>
    <AccountingSupplierParty>
        <Party>
            <PartyTaxScheme>
                <RegistrationName>Jumbo Cencosud</RegistrationName>
                <CompanyID>900123456-7</CompanyID>
            </PartyTaxScheme>
        </Party>
    </AccountingSupplierParty>
    <LegalMonetaryTotal>
        <PayableAmount>45000.00</PayableAmount>
    </LegalMonetaryTotal>
    <TaxTotal>
        <TaxAmount>7200.00</TaxAmount>
    </TaxTotal>
    <InvoiceLine>
        <InvoicedQuantity>1</InvoicedQuantity>
        <LineExtensionAmount>45000.00</LineExtensionAmount>
        <Item>
            <Description>Detergente Ariel 4kg</Description>
        </Item>
        <Price>
            <PriceAmount>45000.00</PriceAmount>
        </Price>
    </InvoiceLine>
</Invoice>`

		// Create in-memory zip
		buf := new(bytes.Buffer)
		w := zip.NewWriter(buf)
		f, err := w.Create("invoice.xml")
		require.NoError(t, err)
		_, err = f.Write([]byte(xmlSample))
		require.NoError(t, err)
		require.NoError(t, w.Close())

		invZip, err := invoiceUC.ProcessZipUpload(ctx, userID, buf.Bytes())
		require.NoError(t, err)
		assert.Equal(t, "Jumbo Cencosud", invZip.IssuerName)
		assert.Equal(t, 45000.00, invZip.TotalAmount)
		assert.Len(t, invZip.Items, 1)
		assert.Equal(t, "Detergente Ariel 4kg", invZip.Items[0].Description)
	})
}
