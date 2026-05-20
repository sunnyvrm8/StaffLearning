// Scenario: fulfillment handles OrderPlaced with at-least-once delivery from Kafka/SQS
// Demonstrates: idempotency via processed-event ledger before side effects
// Trade-off: extra storage vs duplicate shipments on redelivery

import java.sql.Connection;
import java.util.Optional;

public class IdempotentEventConsumer {

    public void onOrderPlaced(Connection conn, String eventId, String orderId) throws Exception {
        if (alreadyProcessed(conn, eventId)) {
            return;
        }
        conn.setAutoCommit(false);
        try {
            markProcessed(conn, eventId);
            try (var ps = conn.prepareStatement(
                    "INSERT INTO shipments (order_id, status) VALUES (?, 'PENDING')")) {
                ps.setString(1, orderId);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    private boolean alreadyProcessed(Connection conn, String eventId) throws Exception {
        try (var ps = conn.prepareStatement("SELECT 1 FROM processed_events WHERE event_id = ?")) {
            ps.setString(1, eventId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void markProcessed(Connection conn, String eventId) throws Exception {
        try (var ps = conn.prepareStatement("INSERT INTO processed_events (event_id) VALUES (?)")) {
            ps.setString(1, eventId);
            ps.executeUpdate();
        }
    }
}
