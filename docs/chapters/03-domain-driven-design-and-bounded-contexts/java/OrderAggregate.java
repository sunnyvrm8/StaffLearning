// Scenario: checkout order — lines cannot change after submit
// Demonstrates: Aggregate root enforcing invariants
// Trade-off: small aggregate vs loading entire order graph in one transaction

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

enum OrderStatus { DRAFT, SUBMITTED }

record LineItem(String sku, int qty) {}

final class Order {
    private final String orderId;
    private OrderStatus status = OrderStatus.DRAFT;
    private final List<LineItem> lines = new ArrayList<>();

    Order(String orderId) { this.orderId = Objects.requireNonNull(orderId); }

    void addLine(LineItem item) {
        if (status != OrderStatus.DRAFT) throw new IllegalStateException("cannot modify submitted order");
        lines.add(item);
    }

    void submit() {
        if (lines.isEmpty()) throw new IllegalStateException("empty order");
        status = OrderStatus.SUBMITTED;
    }

    String orderId() { return orderId; }
    OrderStatus status() { return status; }
    List<LineItem> lines() { return Collections.unmodifiableList(lines); }
}
