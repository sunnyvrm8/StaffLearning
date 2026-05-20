// Scenario: inventory reservation with outbox publishing
// Demonstrates: reserve stock and publish a domain event
// Trade-off: synchronous reservation versus event-driven downstream propagation

package case_studies.ecommerce;

public class InventoryReservationService {
    private final InventoryService inventoryService;
    private final OutboxPublisher outbox;

    public InventoryReservationService(InventoryService inventoryService, OutboxPublisher outbox) {
        this.inventoryService = inventoryService;
        this.outbox = outbox;
    }

    public void reserveOrder(Order order) {
        boolean success = inventoryService.reserve(order.getItems());
        if (!success) {
            throw new OutOfStockException();
        }
        outbox.publish(new OrderReservedEvent(order.getOrderId()));
    }
}
