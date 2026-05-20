// Scenario: fulfillment consumes OrderPlaced from Kafka (at-least-once delivery)
// Demonstrates: dedup by eventId before side effect; commit after durable write
// Trade-off: DB unique constraint vs Redis dedup — DB survives broker replay

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotentOrderConsumer {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    public void onRecord(String eventId, String orderId, Runnable reserveInventory) {
        if (!processed.add(eventId)) {
            return; // duplicate from rebalance or retry
        }
        try {
            reserveInventory.run();
        } catch (RuntimeException e) {
            processed.remove(eventId);
            throw e;
        }
    }
}
