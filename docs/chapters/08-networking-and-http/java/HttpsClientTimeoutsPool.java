// Scenario: outbound call to a card processor with bounded wait and connection reuse
// Demonstrates: HttpClient connect timeout, request timeout, shared pool
// Trade-off: one client bean per upstream vs per-request client (always reuse)

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executor;

final class HttpsClientTimeoutsPool {
  private final HttpClient client;

  HttpsClientTimeoutsPool(Executor executor) {
    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(150))
        .executor(executor)
        .version(HttpClient.Version.HTTP_2)
        .build();
  }

  HttpResponse<String> authorize(String url, String body) throws Exception {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .timeout(Duration.ofMillis(400))
        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    return client.send(req, HttpResponse.BodyHandlers.ofString());
  }
}
