# Chapter 19: Kafka and Messaging

> **One line:** A message broker is a **durable buffer between producers and consumers**—you buy decoupling and scale by accepting **lag, duplicates, and ordering rules** you must design for explicitly.

## Why this matters in production

A **notification platform** ships “order confirmed” email, SMS, and push after checkout. The team points every service at **Kafka** because “it’s what Netflix uses.” Producers use **random partition keys**; consumers process without **idempotency keys**. A broker restart and consumer rebalance replay 40k events—customers get triple SMS, support tickets spike, and marketing complains about spam complaints. Meanwhile **consumer lag** on `order-events` hits 2 hours during a flash sale; ops has no alert on **lag per partition**, only CPU on brokers.

Stakeholders feel **duplicate side effects**, **stale downstream state**, and **mysterious backlog**—not “we need more partitions.” Kafka (and peers like Pulsar, RabbitMQ, SQS) are **infrastructure contracts**: partition strategy, delivery semantics, DLQ policy, and observability must be decided before the first production topic—not after the first incident.

This chapter follows [Chapter 18: Event-Driven Architecture](../18-event-driven-architecture/README.md) and pairs with [Chapter 20: Distributed Systems Fundamentals](../20-distributed-systems-fundamentals/README.md), [Chapter 23: Idempotency, Sagas, and Distributed Transactions](../23-idempotency-sagas-and-distributed-transactions/README.md), and [Chapter 26: Observability](../26-observability/README.md).

## Core ideas

### Log-based messaging vs traditional queues

**Intuition:** Kafka stores an **append-only log** per partition; consumers **track offset** and replay. Classic queues **delete** after ack—good for task distribution, weaker for event sourcing and multiple independent readers.

| | Log (Kafka, Pulsar log) | Queue (SQS, RabbitMQ work queue) |
|---|-------------------------|----------------------------------|
| **Retention** | Configurable time/size; replay | Often delete-on-consume |
| **Consumers** | Many groups read same topic independently | Competing consumers share work |
| **Ordering** | Per-partition strict order | Often best-effort unless single consumer |
| **Use** | Event streams, analytics, audit, CQRS projections | Job dispatch, RPC-style work |
| **Ops signal** | Consumer lag, partition skew | Queue depth, age of oldest message |

**Production anchor:** Use Kafka when **multiple subscribers** need the same history (warehouse + analytics + search indexer) or when **high throughput** with disk-backed durability matters. Use SQS/Rabbit when **simple task queues**, short retention, and managed ops outweigh replay needs.

See [diagrams/overview.md](./diagrams/overview.md).

### Topics, partitions, and brokers

**Intuition:** A **topic** is a named stream; **partitions** are parallel shards of that stream, each an ordered log on disk.

- **Throughput:** More partitions → more parallel consumers (up to one active consumer per partition per group).
- **Ordering:** Kafka guarantees order **within a partition only**. Cross-partition order is undefined.
- **Keys:** `key=orderId` routes all events for one order to the same partition—preserves **per-entity** ordering.
- **Hot partitions:** Skewed keys (celebrity user, default `null` key) create **one partition at 100% CPU** while others idle—p99 lag on that shard.

**Sizing napkin math:** If peak ingest is **50k events/s** and each consumer handles **~500/s** after deserialization and DB writes, you need **≥100 partitions** worth of consumer capacity (often 100+ partitions with headroom). Brokers handle ~MB/s per partition in practice—validate with load test, not slides.

See [diagrams/partitions-consumer-groups.md](./diagrams/partitions-consumer-groups.md).

### Consumer groups and rebalancing

**Intuition:** A **consumer group** is one logical application; each partition is assigned to **at most one** consumer in the group at a time.

- **Scale out:** Add consumers until count = partition count; more consumers sit idle.
- **Rebalance:** Join/leave/crash triggers **partition reassignment**—during cooperative rebalance, some partitions may be **revoked** mid-processing unless you use sticky assignors and handle revocation.
- **Symptom:** Lag spikes every deploy when consumers stop, rebalance, and replay from last committed offset.

**Production:** Use **static membership** or incremental cooperative rebalance where supported; keep processing **idempotent**; commit offsets **after** side effects are durable (or use transactions/outbox—see below).

### Delivery semantics: at-most-once, at-least-once, exactly-once

| Semantic | Mechanism | Risk |
|----------|-----------|------|
| **At-most-once** | Commit offset before process | **Lost** messages on crash |
| **At-least-once** | Process then commit offset | **Duplicates** on crash/rebalance |
| **Exactly-once** (Kafka EOS) | Idempotent producer + transactional consume-process-produce | Complexity, latency, broker version constraints |

**Interview truth:** End-to-end **exactly-once side effects** (one email, one charge) almost always means **idempotent consumers** + dedup store or **outbox**, not broker magic alone.

Code: [java/IdempotentOrderConsumer.java](./java/IdempotentOrderConsumer.java), [go/idempotent_order_consumer.go](./go/idempotent_order_consumer.go).

### Ordering, duplicates, and the outbox

**Intuition:** “We use Kafka so it’s ordered” is false without a **partition key strategy**.

- **Order lifecycle** events (`Placed`, `Paid`, `Shipped`) for `order-123` must share key `order-123`.
- **Inventory** updates keyed by `sku` preserve per-SKU sequence; global inventory order is not guaranteed nor required.
- **Outbox pattern:** Service writes business row + outbox row in **one DB transaction**; relay publishes to Kafka—avoids “DB committed, message never sent” ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)).

Producer example: [java/OrderEventProducer.java](./java/OrderEventProducer.java), [go/order_event_producer.go](./go/order_event_producer.go).

### Dead-letter queues (DLQ) and poison messages

**Intuition:** After **N failures** or unrecoverable parse errors, move the message to a **DLQ topic/queue** so the main consumer advances lag without infinite retry.

| Decision | Options | Trade-off |
|----------|---------|-----------|
| **Retry** | In-process backoff vs retry topic with delay | Retry storms if bug is deterministic |
| **DLQ** | Separate topic per source | Ops must monitor DLQ depth |
| **Replay** | Tooling to fix and re-inject | Needs idempotency on replay |

**How it fails:** DLQ fills silently; team discovers 3M poison messages months later. **Alert** on DLQ rate; **runbook** for inspect → fix code/schema → replay with dry-run.

See [diagrams/ordering-dlq-retry.md](./diagrams/ordering-dlq-retry.md).

### Retention, compaction, and schema evolution

- **Retention:** Time (`retention.ms`) or size—drives **disk cost** and **replay window** for new consumers.
- **Compaction:** Keeps **latest record per key**—useful for **changelog** topics (`user-profile-updates`), not for every event stream.
- **Schema:** Avro/Protobuf + **Schema Registry** enforces compatible evolution; breaking changes need **new topic** or dual-write migration.

**Symptom:** Consumer deserializer fails after deploy—**incompatible schema** change without `BACKWARD` compatibility. Fix: forward-compatible fields, separate `v2` topic, or feature-flagged readers.

### Kafka vs alternatives (architect lens)

| Need | Often choose |
|------|----------------|
| High-volume event bus, replay, stream processing | Kafka, Pulsar |
| AWS-native, minimal ops, short jobs | SQS (+ SNS fan-out) |
| Complex routing, per-message TTL, classic AMQP | RabbitMQ |
| Global replication, geo | Pulsar, Kafka MirrorMaker/Cluster Linking |
| Strict ordering + low volume | Single partition (limits scale) or redesign |

**Avoid:** Kafka for **request/response** or **tiny** message volumes where ops cost dominates. **Avoid:** Random partition keys when **per-entity order** matters.

### Operability: what to measure

- **Consumer lag** (`records-lag-max`) per group, topic, partition—SLO: e.g. p95 lag &lt; 60 s for fulfillment.
- **Under-replicated partitions**, **offline replicas**, ISR shrink—broker health.
- **Produce/consume rate**, **request latency**, **disk usage**.
- **Rebalance rate** and **failed rebalance** events.
- **DLQ depth** and **retry topic** age.

Tie alerts to **business impact** (shipment delay), not only broker CPU ([Chapter 24: Reliability](../24-reliability-engineering/README.md)).

## When to use / when to avoid

**Use when:**

- Multiple services need the **same event history** with durable retention.
- Peak traffic **spikes** must be absorbed without dropping writes (checkout → downstream).
- You need **replay** for new projections, analytics, or disaster recovery.
- Throughput is **thousands+ events/s** sustained with disk-backed durability.

**Avoid when:**

- Team lacks capacity to run **Kafka ops** (or managed equivalent budget).
- Work is **RPC-shaped** (“call inventory now”)—sync or queue may be simpler.
- **Global strict ordering** across all events is required—single partition does not scale; redesign aggregates.
- Message volume is low and **managed queue** meets SLOs at lower TCO.

## How it fails

| Symptom | Likely cause | Debug hooks |
|---------|--------------|---------------|
| Duplicate charges / emails | At-least-once + non-idempotent consumer | Trace `eventId`; check offset commit vs DB commit order |
| Events out of order | Wrong/missing partition key; multiple partitions | Log partition+offset; verify key on producer |
| One partition lag extreme | Hot key skew | Per-partition lag metrics; key distribution histogram |
| Lag after deploy | Rebalance + slow processing | Rebalance logs; reduce `max.poll.interval` violations |
| Consumer stuck | Poison message infinite retry | DLQ rate; stack traces on deserialize |
| Disk full on broker | Retention too long / compaction off | Broker disk alerts; topic size by partition |

**Incident pattern:** “We increased partitions from 12 to 120” without changing **key space**—ordering per key breaks if key hashing changes; plan **new topic** + migration for partition count changes that affect routing semantics.

## Architect takeaway

- **Decide:** Partition key (entity id), retention, consumer group boundaries, delivery semantic (assume at-least-once + idempotency), DLQ/replay policy, schema compatibility rules.
- **Measure:** Lag per partition, DLQ depth, produce error rate, rebalance frequency, end-to-end latency from produce timestamp to consumer completion.
- **Document in design review:** Who owns the topic contract; max acceptable lag; idempotency key; what happens on replay; PII in payloads; upgrade path for schema v2.

## Diagrams

- [Overview — log-based bus](./diagrams/overview.md)
- [Partitions and consumer groups](./diagrams/partitions-consumer-groups.md)
- [Ordering, retry, and DLQ](./diagrams/ordering-dlq-retry.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Idempotent consumer (dedup by event id) | [java/IdempotentOrderConsumer.java](./java/IdempotentOrderConsumer.java) | [go/idempotent_order_consumer.go](./go/idempotent_order_consumer.go) |
| Producer with partition key for per-order ordering | [java/OrderEventProducer.java](./java/OrderEventProducer.java) | [go/order_event_producer.go](./go/order_event_producer.go) |

**Production note:** Ship idempotent handlers and explicit partition keys before scaling consumers; add DLQ and lag alerts in the same release as the first paying customer on the topic.

## Related topics

- [Chapter 18: Event-Driven Architecture](../18-event-driven-architecture/README.md) — events vs commands, CQRS, outbox introduction
- [Chapter 17: Microservices Architecture](../17-microservices-architecture/README.md) — async integration and failure isolation
- [Chapter 20: Distributed Systems Fundamentals](../20-distributed-systems-fundamentals/README.md) — retries, backpressure, partial failure
- [Chapter 23: Idempotency, Sagas, and Distributed Transactions](../23-idempotency-sagas-and-distributed-transactions/README.md) — outbox, sagas, payment safety
- [Chapter 26: Observability](../26-observability/README.md) — lag and trace correlation across consume path

## Interview preparation

See [interview-questions.md](./interview-questions.md) (25 questions — user-requested bank; medium chapter depth per rubric).
