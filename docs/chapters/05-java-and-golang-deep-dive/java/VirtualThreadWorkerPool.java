// Scenario: fan-out price checks for N line items without unbounded platform threads
// Demonstrates: virtual threads + Semaphore for backpressure
// Trade-off: pinning risk if tasks hold synchronized/native locks — keep sections short

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

final class VirtualThreadWorkerPool {
  interface Pricing { double price(String sku); }

  static List<Double> prices(List<String> skus, Pricing pricing, int maxConcurrent)
      throws InterruptedException {
    var gate = new Semaphore(maxConcurrent);
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<java.util.concurrent.Future<Double>>();
      for (String sku : skus) {
        gate.acquire();
        futures.add(executor.submit(() -> {
          try {
            return pricing.price(sku);
          } finally {
            gate.release();
          }
        }));
      }
      var out = new ArrayList<Double>(skus.size());
      for (var f : futures) out.add(f.get());
      return out;
    }
  }
}
