# Event-Driven Architecture — Topology Overview

**Supports decision:** Where to place the broker, which services publish vs subscribe, and where authoritative state still lives.

```mermaid
flowchart TB
  subgraph checkoutCtx [Checkout Context]
    orderApi[Order API]
    orderDb[(Order DB)]
    outboxWorker[Outbox Publisher]
    orderApi --> orderDb
    orderDb --> outboxWorker
  end

  subgraph bus [Event Bus]
    topicOrder[[order.events]]
  end

  subgraph consumers [Downstream Contexts]
    shipSvc[Fulfillment Consumer]
    notifySvc[Notification Consumer]
    analytics[Analytics Consumer]
    shipDb[(Shipment DB)]
    shipSvc --> shipDb
  end

  outboxWorker --> topicOrder
  topicOrder --> shipSvc
  topicOrder --> notifySvc
  topicOrder --> analytics
```

**Caption:** Checkout remains **write authority** for orders; fulfillment owns shipments. The bus carries **facts** (`OrderPlaced`); consumers are **idempotent** and own their side effects.
