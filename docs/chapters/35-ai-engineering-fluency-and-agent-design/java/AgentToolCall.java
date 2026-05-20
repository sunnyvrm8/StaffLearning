package agent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

// Scenario: support agent proposes a refund; orchestrator calls payments with a stable idempotency key.
// Demonstrates: SHA-256 idempotency key from tenant + tool + canonical JSON; request-level HttpClient timeout.
// Trade-off: key omits per-step UUID—good for retries; add stepId if two distinct refunds share same args.
public final class AgentToolCall {

  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  public static String refund(String tenantId, String orderId, String amount, Duration budget) throws Exception {
    String body = "{\"orderId\":\"" + orderId + "\",\"amount\":\"" + amount + "\"}";
    HttpRequest req =
        HttpRequest.newBuilder(URI.create("https://payments.example/refunds"))
            .timeout(budget)
            .header("Idempotency-Key", idempotencyKey(tenantId, "refund", body))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    return resp.body();
  }

  static String idempotencyKey(String tenant, String tool, String canonicalJson) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update((tenant + "|" + tool + "|").getBytes(StandardCharsets.UTF_8));
    md.update(canonicalJson.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(md.digest()).substring(0, 32);
  }
}
