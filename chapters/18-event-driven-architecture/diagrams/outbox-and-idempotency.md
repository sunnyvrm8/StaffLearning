# Outbox + At-Least-Once Delivery

**Supports decision:** How to avoid dual-write (DB commit + broker publish) and how consumers survive duplicates.

```mermaid
sequenceDiagram
  participant API as Order API
  participant DB as Order DB
  participant W as Outbox Worker
  participant K as Event Bus
  participant C as Fulfillment Consumer

  API->>DB: BEGIN; INSERT order; INSERT outbox row; COMMIT
  W->>DB: Poll unpublished outbox
  W->>K: Publish OrderPlaced (id=evt-9f2)
  W->>DB: Mark outbox published
  K->>C: deliver (maybe twice)
  C->>C: IF processed(evt-9f2) SKIP ELSE handle
```

**Caption:** **Outbox** makes publish consistent with commit; **idempotent consumers** make at-least-once delivery safe. Without both, expect duplicate shipments or lost events.
