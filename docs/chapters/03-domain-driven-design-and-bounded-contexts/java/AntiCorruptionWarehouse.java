// Scenario: legacy WMS returns opaque codes; fulfillment uses PickList status
// Demonstrates: Anti-corruption layer on WarehousePort
// Trade-off: mapping table maintenance vs leaking legacy enums into domain

record LegacyPickResponse(String whCode, String pickId) {}

enum PickStatus { PENDING, READY_TO_SHIP, UNKNOWN }

record PickListView(String pickId, PickStatus status) {}

interface WarehousePort {
    PickListView fetchPick(String pickId);
}

final class WarehouseAntiCorruption implements WarehousePort {
  private final LegacyWmsClient legacy;

  WarehouseAntiCorruption(LegacyWmsClient legacy) { this.legacy = legacy; }

  public PickListView fetchPick(String pickId) {
    LegacyPickResponse raw = legacy.getPick(pickId);
    PickStatus status = switch (raw.whCode()) {
      case "LINE_OK", "WH_REQ_7" -> PickStatus.READY_TO_SHIP;
      case "PICKING" -> PickStatus.PENDING;
      default -> PickStatus.UNKNOWN;
    };
    return new PickListView(raw.pickId(), status);
  }
}

final class LegacyWmsClient {
  LegacyPickResponse getPick(String id) { return new LegacyPickResponse("LINE_OK", id); }
}
