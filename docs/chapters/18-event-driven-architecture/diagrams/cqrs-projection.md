# CQRS — Command and Query Paths

**Supports decision:** When to split write models from read models and how projections stay eventually consistent.

```mermaid
flowchart TB
  subgraph writeSide [Write Side]
    cmd[PlaceOrder Command]
    agg[Order Aggregate]
    writeDb[(Orders OLTP)]
    cmd --> agg --> writeDb
    writeDb --> outbox[Outbox / Event Log]
  end

  subgraph readSide [Read Side]
    proj[Projection Worker]
    readDb[(OrderSummary Read DB)]
    outbox --> proj
    proj --> readDb
    readApi[Order History API]
    readApi --> readDb
  end

  clientWrite[Client Checkout] --> cmd
  clientRead[Client Order History] --> readApi
```

**Caption:** Writes enforce **invariants** on the OLTP model; reads hit a **denormalized** store optimized for list/search. Lag between write and read is an explicit **SLA** (e.g., &lt;2 s p99), not a surprise.
