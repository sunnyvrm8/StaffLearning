// Scenario: billing dashboard reads invoices; settlement workers write ledger rows
// Demonstrates: Interface Segregation — Reader vs Writer interfaces in Go
// Trade-off: compile-time enforcement via small interfaces; no fat Repository

package main

type Invoice struct {
	ID          string
	AmountCents int64
}

type LedgerEntry struct {
	ID        string
	InvoiceID string
}

type InvoiceReader interface {
	FindByID(id string) (Invoice, bool)
	ListOpen() []Invoice
}

type LedgerWriter interface {
	Append(entry LedgerEntry) error
}

type ReportingService struct{ invoices InvoiceReader }

func (s *ReportingService) OpenBalance() int64 {
	var sum int64
	for _, inv := range s.invoices.ListOpen() {
		sum += inv.AmountCents
	}
	return sum
}

type SettlementWorker struct{ ledger LedgerWriter }

func (w *SettlementWorker) Settle(inv Invoice) error {
	return w.ledger.Append(LedgerEntry{ID: "le-" + inv.ID, InvoiceID: inv.ID})
}
