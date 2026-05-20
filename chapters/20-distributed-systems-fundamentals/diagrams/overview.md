# Distributed Systems — Partial Failure Topology

**Supports decision:** where to place timeouts, retries, and idempotency when every hop can fail independently—not only when the whole machine is down.

```mermaid
flowchart TB
  subgraph clientZone [Client]
    app[Mobile App]
  end

  subgraph edgeZone [Edge]
    gw[API Gateway]
  end

  subgraph svcZone [Services]
    order[Order API]
    inv[Inventory]
    pay[Payment Provider]
  end

  subgraph storeZone [Stores]
    odb[(Order DB)]
    cache[(Cache)]
  end

  app -->|timeout 2s| gw
  gw -->|deadline budget| order
  order --> inv
  order --> pay
  order --> odb
  order --> cache

  inv -.->|partition / timeout| order
  pay -.->|ambiguous 504| order
```
