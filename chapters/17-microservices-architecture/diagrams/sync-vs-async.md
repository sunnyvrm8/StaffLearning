# Microservices — Sync Chain vs Event Integration

**Supports decision:** when checkout may block on inventory (sync) vs when fulfillment should react to `OrderPlaced` (async) without lengthening the user-facing path.

```mermaid
sequenceDiagram
  participant C as Client
  participant O as OrderService
  participant I as InventoryService
  participant B as EventBus
  participant S as ShipmentService

  Note over C,S: Sync path — user waits
  C->>O: POST /orders
  O->>I: reserve stock
  alt in stock
    I-->>O: reserved
    O-->>C: 201 created
  else timeout
    I-->>O: timeout
    O-->>C: 503 retryable
  end

  Note over C,S: Async path — user does not wait
  O->>B: OrderPlaced
  B->>S: consume event
  S->>S: create shipment
```
