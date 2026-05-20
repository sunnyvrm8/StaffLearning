// Scenario: mobile BFF loads order page from Order, User, Shipment services in parallel
// Demonstrates: per-call timeout, partial degradation (missing shipment vs hard fail)
// Trade-off: fan-out p99 vs sequential; document which fields are optional

import java.util.Map;
import java.util.concurrent.*;

final class BffOrderDetailsAggregation {
  interface OrderApi { Map<String, Object> get(String id); }
  interface UserApi { Map<String, Object> get(String userId); }
  interface ShipmentApi { Map<String, Object> track(String orderId); }

  static Map<String, Object> orderPage(
      String orderId, String userId,
      OrderApi orders, UserApi users, ShipmentApi shipments) throws Exception {

    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var orderF = pool.submit(() -> orders.get(orderId));
      var userF = pool.submit(() -> users.get(userId));
      var shipF = pool.submit(() -> shipments.track(orderId));
      Map<String, Object> order = orderF.get(300, TimeUnit.MILLISECONDS);
      Map<String, Object> user = userF.get(300, TimeUnit.MILLISECONDS);
      Map<String, Object> shipment;
      try {
        shipment = shipF.get(200, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        shipment = Map.of("status", "pending");
      }
      return Map.of("order", order, "user", user, "shipment", shipment);
    }
  }
}
