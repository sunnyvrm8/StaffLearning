// Scenario: reserve stock via modern WMS or legacy warehouse API
// Demonstrates: Port in domain + Adapter in infrastructure (hexagonal)
// Trade-off: mapping layer vs leaking vendor DTOs into OrderService

interface InventoryPort {
    boolean reserve(String sku, int qty);
}

final class OrderService {
    private final InventoryPort inventory;
    OrderService(InventoryPort inventory) { this.inventory = inventory; }
    boolean place(String sku, int qty) { return inventory.reserve(sku, qty); }
}

final class LegacyWarehouseAdapter implements InventoryPort {
    public boolean reserve(String sku, int qty) {
        // translate to legacy SOAP/JSON and map errors to boolean or domain error
        return true;
    }
}
