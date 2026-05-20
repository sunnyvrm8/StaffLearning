# Ordering, Retry, and DLQ

**Supports decision:** Define retry budget and DLQ routing so poison messages do not block the whole partition indefinitely.

```mermaid
flowchart LR
  main[[order-events]]
  consumer[Fulfillment Consumer]
  retryTopic[[order-events-retry]]
  dlq[[order-events-dlq]]
  ops[Ops Replay Tool]

  main --> consumer
  consumer -->|success| done[Commit offset]
  consumer -->|transient error| retryTopic
  retryTopic -->|delay consumer| consumer
  consumer -->|max retries or bad schema| dlq
  dlq --> ops
  ops -->|fixed replay idempotent| main
```

**Caption:** Main consumer advances lag; **DLQ** isolates poison pills. Replay requires **idempotency** and audit—never blind mass re-publish without dedup keys.
