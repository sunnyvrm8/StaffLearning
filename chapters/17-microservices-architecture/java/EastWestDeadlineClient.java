// Scenario: Order service calls Inventory over HTTP with remaining deadline budget
// Demonstrates: subtract margin from parent timeout; fail fast before user SLA blows
// Trade-off: library support (gRPC deadline) vs hand-rolled HTTP — same budget idea

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

final class EastWestDeadlineClient {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String inventoryBase;

  EastWestDeadlineClient(String inventoryBase) { this.inventoryBase = inventoryBase; }

  CompletableFuture<Boolean> reserve(String sku, int qty, Duration parentBudget) {
    Duration callBudget = parentBudget.minus(Duration.ofMillis(50));
    if (callBudget.isNegative() || callBudget.isZero()) {
      return CompletableFuture.completedFuture(false);
    }
    HttpRequest req = HttpRequest.newBuilder()
        .uri(java.net.URI.create(inventoryBase + "/reserve?sku=" + sku + "&qty=" + qty))
        .timeout(callBudget)
        .header("X-Request-Id", java.util.UUID.randomUUID().toString())
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();
    return http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
        .thenApply(r -> r.statusCode() == 200)
        .exceptionally(ex -> false);
  }
}
