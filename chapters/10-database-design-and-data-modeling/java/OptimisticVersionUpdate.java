// Scenario: inventory reservation updates a row with version check to avoid lost writes
// Demonstrates: optimistic concurrency (version column) on contested order/inventory rows
// Trade-off: retry on conflict vs pessimistic SELECT FOR UPDATE (holds locks longer)

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;

final class OptimisticVersionUpdate {
  Optional<Long> reserveStock(Connection conn, String sku, int qty, long expectedVersion)
      throws Exception {
    String sql = """
        UPDATE inventory
        SET on_hand = on_hand - ?, version = version + 1
        WHERE sku = ? AND version = ? AND on_hand >= ?
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, qty);
      ps.setString(2, sku);
      ps.setLong(3, expectedVersion);
      ps.setInt(4, qty);
      if (ps.executeUpdate() == 1) {
        return Optional.of(expectedVersion + 1);
      }
      return Optional.empty(); // caller reloads and retries or returns 409
    }
  }
}
