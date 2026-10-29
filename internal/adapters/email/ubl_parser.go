package email

import (
	"archive/zip"
	"bytes"
	"encoding/xml"
	"errors"
	"fmt"
	"io"
	"strconv"
	"strings"
	"time"

	"house-expenses/internal/core/domain"
)

type ParsedZipResult struct {
	Invoice  *domain.Invoice
	PDFBytes []byte
}

type UBLAttachedDocument struct {
	XMLName    xml.Name `xml:"AttachedDocument"`
	Attachment struct {
		ExternalReference struct {
			Description string `xml:"Description"`
		} `xml:"ExternalReference"`
	} `xml:"Attachment"`
}

type UBLInvoice struct {
	XMLName   xml.Name `xml:"Invoice"`
	UUID      string   `xml:"UUID"`
	IssueDate string   `xml:"IssueDate"`
	ID        string   `xml:"ID"`
	AccountingSupplierParty struct {
		Party struct {
			PartyName struct {
				Name string `xml:"Name"`
			} `xml:"PartyName"`
			PartyTaxScheme struct {
				RegistrationName string `xml:"RegistrationName"`
				CompanyID        string `xml:"CompanyID"`
			} `xml:"PartyTaxScheme"`
		} `xml:"Party"`
	} `xml:"AccountingSupplierParty"`
	LegalMonetaryTotal struct {
		PayableAmount struct {
			Value string `xml:",chardata"`
		} `xml:"PayableAmount"`
	} `xml:"LegalMonetaryTotal"`
	TaxTotal struct {
		TaxAmount struct {
			Value string `xml:",chardata"`
		} `xml:"TaxAmount"`
	} `xml:"TaxTotal"`
	InvoiceLines []struct {
		InvoicedQuantity struct {
			Value string `xml:",chardata"`
		} `xml:"InvoicedQuantity"`
		LineExtensionAmount struct {
			Value string `xml:",chardata"`
		} `xml:"LineExtensionAmount"`
		Item struct {
			Description string `xml:"Description"`
		} `xml:"Item"`
		Price struct {
			PriceAmount struct {
				Value string `xml:",chardata"`
			} `xml:"PriceAmount"`
		} `xml:"Price"`
	} `xml:"InvoiceLine"`
}

func ParseZipAttachment(zipBytes []byte) (*ParsedZipResult, error) {
	reader, err := zip.NewReader(bytes.NewReader(zipBytes), int64(len(zipBytes)))
	if err != nil {
		return nil, fmt.Errorf("failed to open zip archive: %w", err)
	}

	var xmlBytes []byte
	var pdfBytes []byte

	for _, file := range reader.File {
		lowerName := strings.ToLower(file.Name)
		if strings.HasSuffix(lowerName, ".xml") {
			rc, err := file.Open()
			if err == nil {
				xmlBytes, _ = io.ReadAll(rc)
				rc.Close()
			}
		} else if strings.HasSuffix(lowerName, ".pdf") {
			rc, err := file.Open()
			if err == nil {
				pdfBytes, _ = io.ReadAll(rc)
				rc.Close()
			}
		}
	}

	if len(xmlBytes) == 0 {
		return nil, errors.New("no XML invoice found inside zip archive")
	}

	invoice, err := ParseUBLInvoiceXML(xmlBytes)
	if err != nil {
		return nil, err
	}

	return &ParsedZipResult{
		Invoice:  invoice,
		PDFBytes: pdfBytes,
	}, nil
}

func ParseUBLInvoiceXML(xmlContent []byte) (*domain.Invoice, error) {
	// First check if this is an AttachedDocument wrapping the inner Invoice
	var attachedDoc UBLAttachedDocument
	var innerXML = xmlContent
	if err := xml.Unmarshal(xmlContent, &attachedDoc); err == nil && attachedDoc.Attachment.ExternalReference.Description != "" {
		innerXML = []byte(attachedDoc.Attachment.ExternalReference.Description)
	}

	var ubl UBLInvoice
	if err := xml.Unmarshal(innerXML, &ubl); err != nil {
		return nil, fmt.Errorf("failed to parse UBL 2.1 XML: %w", err)
	}

	issuerName := ubl.AccountingSupplierParty.Party.PartyTaxScheme.RegistrationName
	if issuerName == "" {
		issuerName = ubl.AccountingSupplierParty.Party.PartyName.Name
	}
	if issuerName == "" {
		issuerName = "Proveedor DIAN"
	}

	total, _ := strconv.ParseFloat(strings.TrimSpace(ubl.LegalMonetaryTotal.PayableAmount.Value), 64)
	tax, _ := strconv.ParseFloat(strings.TrimSpace(ubl.TaxTotal.TaxAmount.Value), 64)

	issueDate, err := time.Parse("2006-01-02", strings.TrimSpace(ubl.IssueDate))
	if err != nil {
		issueDate = time.Now()
	}

	cufe := strings.TrimSpace(ubl.UUID)
	invoiceNumber := strings.TrimSpace(ubl.ID)
	if invoiceNumber == "" {
		invoiceNumber = "FE-" + cufe[:8]
	}

	invoice := &domain.Invoice{
		CUFE:          &cufe,
		IssuerName:    issuerName,
		IssuerNIT:     strings.TrimSpace(ubl.AccountingSupplierParty.Party.PartyTaxScheme.CompanyID),
		InvoiceNumber: invoiceNumber,
		IssueDate:     issueDate,
		TotalAmount:   total,
		TaxAmount:     tax,
		SourceType:    domain.InvoiceSourceEmailZip,
		CreatedAt:     time.Now(),
	}

	var items []domain.InvoiceItem
	for _, line := range ubl.InvoiceLines {
		qty, _ := strconv.ParseFloat(strings.TrimSpace(line.InvoicedQuantity.Value), 64)
		if qty == 0 {
			qty = 1
		}
		unitPrice, _ := strconv.ParseFloat(strings.TrimSpace(line.Price.PriceAmount.Value), 64)
		totalPrice, _ := strconv.ParseFloat(strings.TrimSpace(line.LineExtensionAmount.Value), 64)
		if totalPrice == 0 {
			totalPrice = qty * unitPrice
		}

		desc := strings.TrimSpace(line.Item.Description)
		if desc == "" {
			desc = "Item Factura"
		}

		items = append(items, domain.InvoiceItem{
			Description:    desc,
			Quantity:       qty,
			UnitPrice:      unitPrice,
			TotalPrice:     totalPrice,
			Classification: domain.ClassificationHousehold,
		})
	}

	invoice.Items = items
	return invoice, nil
}
