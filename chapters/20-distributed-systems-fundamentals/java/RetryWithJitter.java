// Scenario: Checkout calls payment provider; transient 503s need bounded retries
// Demonstrates: exponential backoff + full jitter; idempotency-key on every attempt
// Trade-off: more latency on failure vs retry storm without jitter

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

final class RetryWithJitter {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String payBase;

  RetryWithJitter(String payBase) { this.payBase = payBase; }

  boolean charge(String idempotencyKey, String orderId, Duration totalBudget) throws InterruptedException {
    long deadline = System.nanoTime() + totalBudget.toNanos();
    int attempt = 0;
    while (System.nanoTime() < deadline && attempt < 4) {
      HttpRequest req = HttpRequest.newBuilder()
          .uri(java.net.URI.create(payBase + "/charge"))
          .timeout(Duration.ofMillis(400))
          .header("Idempotency-Key", idempotencyKey)
          .POST(HttpRequest.BodyPublishers.ofString("{\"orderId\":\"" + orderId + "\"}"))
          .build();
      try {
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) return true;
        if (resp.statusCode() < 500) return false;
      } catch (Exception ignored) { /* retryable */ }
      sleepJitter(attempt++);
    }
    return false;
  }

  private static void sleepJitter(int attempt) throws InterruptedException {
    long cap = Math.min(2000, 100L << attempt);
    Thread.sleep(ThreadLocalRandom.current().nextLong(cap));
  }
}
