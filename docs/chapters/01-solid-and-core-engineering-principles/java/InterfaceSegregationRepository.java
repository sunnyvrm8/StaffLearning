// Scenario: billing dashboard reads invoices; settlement workers write ledger rows
// Demonstrates: Interface Segregation — narrow Reader vs Writer ports
// Trade-off: two interfaces vs fat repo with unsupported write methods

import java.util.List;
import java.util.Optional;

record Invoice(String id, long amountCents) {}
record LedgerEntry(String id, String invoiceId) {}

interface InvoiceReader {
    Optional<Invoice> findById(String id);
    List<Invoice> listOpen();
}

interface LedgerWriter {
    void append(LedgerEntry entry);
}

final class ReportingService {
    private final InvoiceReader invoices;
    ReportingService(InvoiceReader invoices) { this.invoices = invoices; }
    long openBalance() {
        return invoices.listOpen().stream().mapToLong(Invoice::amountCents).sum();
    }
}

final class SettlementWorker {
    private final LedgerWriter ledger;
    SettlementWorker(LedgerWriter ledger) { this.ledger = ledger; }
    void settle(Invoice inv) {
        ledger.append(new LedgerEntry("le-" + inv.id(), inv.id()));
    }
}
