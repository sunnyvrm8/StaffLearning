// Scenario: map domain failures to RFC 7807 Problem+JSON for public REST
// Demonstrates: stable type URI, title, status, retryable flag extension
// Trade-off: verbose JSON vs ad-hoc { "error": "bad" } — machines and docs need structure

import java.net.URI;
import java.util.Map;

record ProblemDetail(
    URI type,
    String title,
    int status,
    String detail,
    String instance,
    boolean retryable
) {}

final class ProblemDetailsError {
    static final URI INSUFFICIENT_FUNDS =
        URI.create("https://api.example.com/problems/insufficient-funds");

    static ProblemDetail fromDecline(String orderId, boolean retryable) {
        return new ProblemDetail(
            INSUFFICIENT_FUNDS,
            "Insufficient funds",
            402,
            "Card declined for order " + orderId,
            "/orders/" + orderId,
            retryable
        );
    }

    static Map<String, Object> toBody(ProblemDetail p) {
        return Map.of(
            "type", p.type().toString(),
            "title", p.title(),
            "status", p.status(),
            "detail", p.detail(),
            "instance", p.instance(),
            "retryable", p.retryable()
        );
    }
}
