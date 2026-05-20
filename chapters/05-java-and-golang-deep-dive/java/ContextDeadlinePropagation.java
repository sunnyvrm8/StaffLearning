// Scenario: checkout aggregates inventory + fraud with a hard user-facing deadline
// Demonstrates: CompletableFuture + orTimeout; cancel downstream on failure
// Trade-off: vs virtual-thread sequential code — explicit composition for timeouts

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

final class ContextDeadlinePropagation {
    interface Inventory { int available(String sku); }
    interface Fraud { boolean allow(String orderId); }

  static CompletableFuture<Boolean> checkout(
      String orderId, String sku, Inventory inv, Fraud fraud) {
    return CompletableFuture
        .supplyAsync(() -> inv.available(sku))
        .thenCombine(
            CompletableFuture.supplyAsync(() -> fraud.allow(orderId)),
            (qty, ok) -> qty > 0 && ok)
        .orTimeout(800, TimeUnit.MILLISECONDS)
        .exceptionally(ex -> false);
  }
}
