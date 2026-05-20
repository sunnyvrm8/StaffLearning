# Microservices — Topology Overview

**Supports decision:** whether each capability gets its own deployable, datastore, and async boundary—or stays in a modular monolith until pain justifies network cost.

```mermaid
flowchart TB
  subgraph clients [Clients]
    web[Web]
    mobile[Mobile]
    partner[Partner API]
  end

  subgraph edge [Edge]
    gw[API Gateway]
    bff[BFF Mobile]
  end

  subgraph services [Services]
    order[Order Service]
    pay[Payment Service]
    inv[Inventory Service]
    ship[Shipment Service]
  end

  subgraph data [Data per service]
    odb[(Order DB)]
    pdb[(Payment DB)]
    idb[(Inventory DB)]
  end

  bus[[Event Bus]]

  web --> gw
  mobile --> bff
  partner --> gw
  gw --> order
  bff --> order
  bff --> inv
  order --> pay
  order --> bus
  inv --> bus
  ship --> bus
  order --> odb
  pay --> pdb
  inv --> idb
```
