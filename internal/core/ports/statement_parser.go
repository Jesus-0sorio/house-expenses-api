package ports

import (
	"io"
	"time"
)

type ParsedTransaction struct {
	Date        time.Time
	Description string
	Amount      float64
}

type ParsedStatement struct {
	BankName        string
	AccountLastFour string
	PeriodStart     time.Time
	PeriodEnd       time.Time
	Transactions    []ParsedTransaction
}

type StatementParserPort interface {
	Parse(r io.Reader, filename string) (*ParsedStatement, error)
}
