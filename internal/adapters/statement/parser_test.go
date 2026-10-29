package statement_test

import (
	"bytes"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/xuri/excelize/v2"

	"house-expenses/internal/adapters/statement"
)

func TestMultiFormatStatementParser_CSV(t *testing.T) {
	parser := statement.NewMultiFormatStatementParser()

	t.Run("Standard comma-separated CSV with header", func(t *testing.T) {
		csvContent := `Date,Description,Amount
2026-09-01,Enel Codensa Luz,120000
2026-09-03,Almacenes Exito Calle 80,85000.50
`
		parsed, err := parser.Parse(strings.NewReader(csvContent), "extracto.csv")
		require.NoError(t, err)
		require.Len(t, parsed.Transactions, 2)

		assert.Equal(t, "Enel Codensa Luz", parsed.Transactions[0].Description)
		assert.Equal(t, 120000.0, parsed.Transactions[0].Amount)
		assert.Equal(t, 2026, parsed.Transactions[0].Date.Year())
		assert.Equal(t, time.September, parsed.Transactions[0].Date.Month())
		assert.Equal(t, 1, parsed.Transactions[0].Date.Day())

		assert.Equal(t, "Almacenes Exito Calle 80", parsed.Transactions[1].Description)
		assert.Equal(t, 85000.50, parsed.Transactions[1].Amount)
	})

	t.Run("Semicolon-separated CSV with Colombian currency format", func(t *testing.T) {
		csvContent := `Fecha;Detalle;Valor
05/09/2026;Claro Soluciones Moviles;$ 45.000,00
08/09/2026;D1 Supermercado;-$ 32.500,50
`
		parsed, err := parser.Parse(strings.NewReader(csvContent), "bancolombia.csv")
		require.NoError(t, err)
		require.Len(t, parsed.Transactions, 2)

		assert.Equal(t, "Claro Soluciones Moviles", parsed.Transactions[0].Description)
		assert.Equal(t, 45000.0, parsed.Transactions[0].Amount)
		assert.Equal(t, 5, parsed.Transactions[0].Date.Day())

		assert.Equal(t, "D1 Supermercado", parsed.Transactions[1].Description)
		assert.Equal(t, 32500.50, parsed.Transactions[1].Amount)
	})
}

func TestMultiFormatStatementParser_Excel(t *testing.T) {
	parser := statement.NewMultiFormatStatementParser()

	// Create an Excel file in-memory
	f := excelize.NewFile()
	sheet := f.GetSheetName(0)
	f.SetCellValue(sheet, "A1", "Fecha")
	f.SetCellValue(sheet, "B1", "Concepto")
	f.SetCellValue(sheet, "C1", "Monto")

	f.SetCellValue(sheet, "A2", "2026-09-02")
	f.SetCellValue(sheet, "B2", "Transferencia Arriendo")
	f.SetCellValue(sheet, "C2", "1500000")

	f.SetCellValue(sheet, "A3", "2026-09-04")
	f.SetCellValue(sheet, "B3", "Farmatodo Medicamentos")
	f.SetCellValue(sheet, "C3", "65000")

	var buf bytes.Buffer
	require.NoError(t, f.Write(&buf))

	parsed, err := parser.Parse(&buf, "movimientos.xlsx")
	require.NoError(t, err)
	require.Len(t, parsed.Transactions, 2)

	assert.Equal(t, "Transferencia Arriendo", parsed.Transactions[0].Description)
	assert.Equal(t, 1500000.0, parsed.Transactions[0].Amount)

	assert.Equal(t, "Farmatodo Medicamentos", parsed.Transactions[1].Description)
	assert.Equal(t, 65000.0, parsed.Transactions[1].Amount)
}
