// Scenario: optional fraud score enrichment on checkout (non-blocking path)
// Demonstrates: Circuit breaker — fail fast when dependency is unhealthy
// Trade-off: degraded checkout vs hard fail; tune with timeouts and metrics

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

final class CircuitBreakerFraudClient {
    enum State { CLOSED, OPEN, HALF_OPEN }
    private volatile State state = State.CLOSED;
    private final AtomicInteger failures = new AtomicInteger();
    private final int threshold = 5;
    private final FraudApi api;

    CircuitBreakerFraudClient(FraudApi api) { this.api = api; }

    Optional<Integer> score(String orderId) {
        if (state == State.OPEN) return Optional.empty();
        try {
            int s = api.fetchScore(orderId);
            failures.set(0);
            state = State.CLOSED;
            return Optional.of(s);
        } catch (Exception e) {
            if (failures.incrementAndGet() >= threshold) state = State.OPEN;
            return Optional.empty();
        }
    }

    interface FraudApi { int fetchScore(String orderId); }
}
