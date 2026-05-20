# Anti-Corruption Layer — Legacy to Domain

**Supports decision:** Where translation lives so fulfillment domain never imports `LegacyPickResponse` types.

```mermaid
flowchart LR
  subgraph fulfillment [Fulfillment Context]
    domain[PickList Aggregate]
    acl[WarehouseAntiCorruption]
    port[WarehousePort]
    domain --> port
    port --> acl
  end

  subgraph infra [Infrastructure]
    client[LegacyWmsClient]
    acl --> client
  end

  client -->|WH_REQ_7, LINE_OK| acl
  acl -->|ReadyToShip, PickListId| domain
```

**Invariant:** Only the ACL package may import legacy DTOs or SDK types. Domain speaks `PickList`, `ShipmentStatus`, and domain errors—not vendor codes.
