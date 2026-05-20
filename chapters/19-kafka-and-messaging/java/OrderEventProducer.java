// Scenario: checkout publishes OrderPlaced with partition key = orderId
// Demonstrates: per-order ordering on one partition; same key → same partition
// Trade-off: hot celebrity orderId can skew one partition — monitor per-partition lag

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class OrderEventProducer {

    public record ProduceRequest(String topic, String orderId, byte[] payload) {}

    /** Maps to Kafka ProducerRecord(topic, key, value) in real clients. */
    public ProduceRequest orderPlaced(String orderId, byte[] json) {
        Objects.requireNonNull(orderId, "orderId");
        return new ProduceRequest("order-events", orderId, json);
    }

    public byte[] keyBytes(String orderId) {
        return orderId.getBytes(StandardCharsets.UTF_8);
    }
}
