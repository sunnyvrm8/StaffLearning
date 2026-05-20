# Partitions and Consumer Groups

**Supports decision:** Size partitions and consumer instances so parallelism matches throughput without breaking per-key ordering.

```mermaid
flowchart TB
  subgraph topic [Topic order-events - 4 partitions]
    p0[P0: orders A-M]
    p1[P1: orders N-Z]
    p2[P2: orders ...]
    p3[P3: orders ...]
  end

  subgraph groupA [Consumer Group fulfillment-v2]
    c1[Consumer 1]
    c2[Consumer 2]
    c3[Consumer 3]
  end

  p0 --> c1
  p1 --> c1
  p2 --> c2
  p3 --> c3

  note1[Max active consumers equals partition count]
```

```mermaid
sequenceDiagram
  participant P as Producer
  participant K as Partition P2
  participant C as Consumer
  P->>K: record key=order-99 offset=1042
  C->>K: poll batch
  C->>C: process idempotent
  C->>K: commit offset 1042
  Note over C,K: Crash before commit replays 1042 at-least-once
```

**Caption:** Partition key `orderId` hashes to one partition—**order-99** events stay ordered. Commit **after** durable side effect to avoid silent loss; expect **replay** on failure.
