# DDD — Contexts and Data Ownership

**Supports decision:** Where to draw boundaries before splitting services or databases—who owns writes for `Order`, `Shipment`, and `Invoice`.

```mermaid
flowchart TB
  subgraph checkout [Checkout Context - owns Cart and PlaceOrder]
    cartDB[(cart_db)]
    Cart[Cart Aggregate]
    Cart --> cartDB
  end

  subgraph fulfillment [Fulfillment Context - owns Shipment]
    shipDB[(fulfillment_db)]
    Shipment[Shipment Aggregate]
    Shipment --> shipDB
  end

  subgraph billing [Billing Context - owns LedgerEntry]
    billDB[(billing_db)]
    Ledger[Ledger Aggregate]
    Ledger --> billDB
  end

  checkout -->|OrderPlaced idempotent event| fulfillment
  checkout -->|InvoiceRequested| billing
```

**Reading the diagram:** Each context has **its own store** in the target state. Integration is **events or APIs**, not shared mutable rows. Shared databases are a **transitional** smell—document the strangler exit.
