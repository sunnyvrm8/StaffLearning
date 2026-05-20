// Scenario: payment webhook may retry; insert ledger row once per provider event id
// Demonstrates: idempotent insert via UNIQUE(provider, event_id) and catch duplicate
// Trade-off: DB uniqueness vs application-only dedup (race under concurrent retries)

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

final class UniqueConstraintIdempotentInsert {
  enum InsertOutcome { INSERTED, DUPLICATE }

  InsertOutcome recordWebhook(Connection conn, String provider, String eventId, String payload)
      throws SQLException {
    String sql = "INSERT INTO payment_events (provider, event_id, payload) VALUES (?, ?, ?)";
    try (var ps = conn.prepareStatement(sql)) {
      ps.setString(1, provider);
      ps.setString(2, eventId);
      ps.setString(3, payload);
      ps.executeUpdate();
      return InsertOutcome.INSERTED;
    } catch (SQLIntegrityConstraintViolationException e) {
      return InsertOutcome.DUPLICATE; // safe to ACK webhook
    }
  }
}
