package statement

import (
	"bytes"
	"encoding/csv"
	"errors"
	"fmt"
	"io"
	"math"
	"strconv"
	"strings"
	"time"

	"github.com/xuri/excelize/v2"

	"house-expenses/internal/core/ports"
)

var (
	ErrUnsupportedFormat   = errors.New("unsupported statement format")
	ErrEmptyStatement      = errors.New("statement contains no transactions")
	ErrHeaderNotFound      = errors.New("unable to identify date, description, or amount columns in statement")
)

type MultiFormatStatementParser struct{}

func NewMultiFormatStatementParser() *MultiFormatStatementParser {
	return &MultiFormatStatementParser{}
}

func (p *MultiFormatStatementParser) Parse(r io.Reader, filename string) (*ports.ParsedStatement, error) {
	data, err := io.ReadAll(r)
	if err != nil {
		return nil, fmt.Errorf("failed to read statement data: %w", err)
	}

	lowerName := strings.ToLower(filename)
	if strings.HasSuffix(lowerName, ".xlsx") || strings.HasSuffix(lowerName, ".xls") {
		return p.parseExcel(bytes.NewReader(data))
	}

	return p.parseCSV(bytes.NewReader(data))
}

func (p *MultiFormatStatementParser) parseCSV(r io.Reader) (*ports.ParsedStatement, error) {
	raw, err := io.ReadAll(r)
	if err != nil {
		return nil, err
	}

	lines := strings.Split(string(raw), "\n")
	if len(lines) == 0 {
		return nil, ErrEmptyStatement
	}

	// Detect delimiter: comma, semicolon, tab
	delimiter := rune(',')
	for _, line := range lines {
		trimmed := strings.TrimSpace(line)
		if trimmed == "" {
			continue
		}
		if strings.Count(trimmed, ";") > strings.Count(trimmed, ",") && strings.Count(trimmed, ";") > strings.Count(trimmed, "\t") {
			delimiter = ';'
		} else if strings.Count(trimmed, "\t") > strings.Count(trimmed, ",") {
			delimiter = '\t'
		}
		break
	}

	csvReader := csv.NewReader(strings.NewReader(string(raw)))
	csvReader.Comma = delimiter
	csvReader.LazyQuotes = true
	csvReader.FieldsPerRecord = -1

	records, err := csvReader.ReadAll()
	if err != nil {
		return nil, fmt.Errorf("failed to parse CSV: %w", err)
	}

	return p.extractTransactionsFromRows(records)
}

func (p *MultiFormatStatementParser) parseExcel(r io.Reader) (*ports.ParsedStatement, error) {
	f, err := excelize.OpenReader(r)
	if err != nil {
		return nil, fmt.Errorf("failed to open excel file: %w", err)
	}
	defer f.Close()

	sheets := f.GetSheetList()
	if len(sheets) == 0 {
		return nil, ErrEmptyStatement
	}

	rows, err := f.GetRows(sheets[0])
	if err != nil {
		return nil, fmt.Errorf("failed to get sheet rows: %w", err)
	}

	return p.extractTransactionsFromRows(rows)
}

func (p *MultiFormatStatementParser) extractTransactionsFromRows(rows [][]string) (*ports.ParsedStatement, error) {
	if len(rows) < 2 {
		return nil, ErrEmptyStatement
	}

	dateCol := -1
	descCol := -1
	amountCol := -1
	headerRowIdx := -1

	for rowIdx, row := range rows {
		for colIdx, cell := range row {
			val := strings.ToLower(strings.TrimSpace(cell))
			if strings.Contains(val, "fecha") || strings.Contains(val, "date") {
				dateCol = colIdx
			}
			if strings.Contains(val, "descrip") || strings.Contains(val, "detalle") || strings.Contains(val, "concepto") || strings.Contains(val, "merchant") {
				descCol = colIdx
			}
			if strings.Contains(val, "valor") || strings.Contains(val, "monto") || strings.Contains(val, "amount") || strings.Contains(val, "importe") {
				amountCol = colIdx
			}
		}
		if dateCol != -1 && descCol != -1 && amountCol != -1 {
			headerRowIdx = rowIdx
			break
		}
	}

	// Fallback to columns 0, 1, 2 if no explicit header matched
	if headerRowIdx == -1 {
		if len(rows[0]) >= 3 {
			headerRowIdx = 0
			dateCol, descCol, amountCol = 0, 1, 2
		} else {
			return nil, ErrHeaderNotFound
		}
	}

	var parsedTxs []ports.ParsedTransaction
	var minDate, maxDate time.Time

	for i := headerRowIdx + 1; i < len(rows); i++ {
		row := rows[i]
		if len(row) <= dateCol || len(row) <= descCol || len(row) <= amountCol {
			continue
		}

		rawDate := strings.TrimSpace(row[dateCol])
		rawDesc := strings.TrimSpace(row[descCol])
		rawAmount := strings.TrimSpace(row[amountCol])

		if rawDate == "" || rawDesc == "" || rawAmount == "" {
			continue
		}

		txDate, err := parseDate(rawDate)
		if err != nil {
			continue // Skip non-date rows (e.g. summary or footers)
		}

		amount, err := parseAmount(rawAmount)
		if err != nil {
			continue
		}

		// Positive value for absolute expense/debit
		amount = math.Abs(amount)

		parsedTxs = append(parsedTxs, ports.ParsedTransaction{
			Date:        txDate,
			Description: rawDesc,
			Amount:      amount,
		})

		if minDate.IsZero() || txDate.Before(minDate) {
			minDate = txDate
		}
		if maxDate.IsZero() || txDate.After(maxDate) {
			maxDate = txDate
		}
	}

	if len(parsedTxs) == 0 {
		return nil, ErrEmptyStatement
	}

	return &ports.ParsedStatement{
		PeriodStart:  minDate,
		PeriodEnd:    maxDate,
		Transactions: parsedTxs,
	}, nil
}

func parseDate(s string) (time.Time, error) {
	formats := []string{
		"2006-01-02",
		"02/01/2006",
		"2006/01/02",
		"02-01-2006",
		"02/01/06",
		"2006-01-02 15:04:05",
		"02/01/2006 15:04:05",
		time.RFC3339,
	}

	s = strings.TrimSpace(s)
	for _, fmtStr := range formats {
		if t, err := time.Parse(fmtStr, s); err == nil {
			return t, nil
		}
	}
	return time.Time{}, fmt.Errorf("could not parse date: %s", s)
}

func parseAmount(s string) (float64, error) {
	s = strings.TrimSpace(s)
	s = strings.ReplaceAll(s, "$", "")
	s = strings.ReplaceAll(s, "COP", "")
	s = strings.ReplaceAll(s, " ", "")

	// Handle Colombian/European format: 120.000,50 vs 120,000.50
	lastComma := strings.LastIndex(s, ",")
	lastDot := strings.LastIndex(s, ".")

	if lastComma > lastDot {
		// Decimal separator is comma, dot is thousand separator
		s = strings.ReplaceAll(s, ".", "")
		s = strings.ReplaceAll(s, ",", ".")
	} else if lastDot > lastComma {
		// Decimal separator is dot, comma is thousand separator
		s = strings.ReplaceAll(s, ",", "")
	}

	return strconv.ParseFloat(s, 64)
}
