// Scenario: checkout commits Order + outbox row; separate worker publishes to bus
// Demonstrates: transactional outbox — no dual-write to DB and broker in one request
// Trade-off: seconds of publish lag vs lost events on crash-after-commit

import java.sql.Connection;
import java.util.UUID;

public class TransactionalOutbox {

    public void placeOrder(Connection conn, String orderId, String userId) throws Exception {
        conn.setAutoCommit(false);
        try {
            try (var ps = conn.prepareStatement(
                    "INSERT INTO orders (id, user_id, status) VALUES (?, ?, 'PLACED')")) {
                ps.setString(1, orderId);
                ps.setString(2, userId);
                ps.executeUpdate();
            }
            String eventId = UUID.randomUUID().toString();
            try (var ps = conn.prepareStatement(
                    "INSERT INTO outbox (event_id, topic, payload, published) VALUES (?, ?, ?, false)")) {
                ps.setString(1, eventId);
                ps.setString(2, "order.events");
                ps.setString(3, "{\"type\":\"OrderPlaced\",\"orderId\":\"" + orderId + "\"}");
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }
}
