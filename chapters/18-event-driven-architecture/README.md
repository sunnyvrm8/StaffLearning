# Chapter 18: Event-Driven Architecture

> **One line:** Event-driven architecture publishes **facts after commit** so teams integrate without synchronous chains—at the cost of **eventual consistency**, **schema evolution**, and **operational complexity** you must design for upfront.

## Why this matters in production

A payments platform “goes event-driven” by firing Kafka messages **before** the database transaction commits. A broker blip duplicates `PaymentCaptured`; fulfillment ships twice; finance cannot reconcile because the **event stream** and **ledger** disagree. Another team emits `OrderUpdated` with no version field—six consumers break on deploy day. On-call sees **consumer lag** climbing while product insists “checkout is fine.” Stakeholders feel **duplicate side effects**, **stale dashboards**, and **mystery backlog**—not “we need more topics.”

Event-driven architecture (EDA) is how bounded contexts **notify** each other after they own a state change ([Chapter 03: DDD](../03-domain-driven-design-and-bounded-contexts/README.md)), complementing sync APIs from microservices ([Chapter 17: Microservices](../17-microservices-architecture/README.md)) and durable pipes from [Chapter 19: Kafka](../19-kafka-and-messaging/README.md). It pairs with **idempotency, sagas, and outbox** ([Chapter 23: Idempotency & Sagas](../23-idempotency-sagas-and-distributed-transactions/README.md)) and API contracts ([Chapter 09: API Design](../09-api-design/README.md)). This chapter is the **design vocabulary**—events vs commands, CQRS, event sourcing overview—not broker tuning (Ch. 19).

## Core ideas

### Events vs commands vs queries

**Intuition:** A **command** expresses intent (“reserve inventory”); an **event** records a past fact (“`OrderPlaced` at 14:02 UTC”); a **query** reads state without mutating it.

| | Command | Event | Query (CQRS read) |
|---|---------|-------|-------------------|
| **Tense** | Imperative | Past tense | Present state |
| **Audience** | Usually one handler | 0..N subscribers | Read API / projection |
| **Coupling** | Sender knows target capability | Sender knows only its domain | Client knows read model |
| **Failure mode** | Retry can double side effects without idempotency | Duplicate delivery | Stale read / lag |
| **Example** | `CapturePayment` → Payment service | `PaymentCaptured` on bus | `GET /orders/{id}/summary` |

**Production anchor:** After checkout commits, publish **`OrderPlaced`** (fact). Do not broadcast **`CreateShipment`** (command) unless fulfillment is the only subscriber and you accept tighter coupling. Commands remain appropriate **inside** a context or over a **dedicated queue** with one consumer group accountable for the work.

See [diagrams/events-vs-commands.md](./diagrams/events-vs-commands.md).

### Event notification vs event-carried state transfer

| | Notification (thin event) | Event-carried state transfer (fat event) |
|---|---------------------------|------------------------------------------|
| **Payload** | IDs + type (`orderId`, `userId`) | Full snapshot needed by consumers |
| **Pros** | Small messages; publisher owns less consumer detail | Fewer sync calls; consumers work offline |
| **Cons** | Consumers call back (coupling, load) | Schema churn; PII sprawl on bus |
| **Use** | Stable aggregate IDs; consumers own enrichment | High fan-out read models; strict latency on consumer |

**Rule of thumb:** Start **thin** + documented **read APIs** for enrichment; add fields to events only when callback load or coupling hurts (order of **10⁴+** events/sec per consumer type).

### Integration styles on the context map

From DDD context mapping, EDA usually implements **Published Language** (canonical event contract) or **Conformist** (downstream accepts upstream’s events as-is). **Anti-corruption layer** translates foreign events into local commands/models—essential when legacy billing emits `BILL_ORD_V2` and checkout speaks `OrderPlaced`.

### Transactional outbox (non-negotiable for DB + bus)

**Intuition:** Never “write DB, then pray publish succeeds”—you will lose events or double-publish.

1. In one transaction: update aggregate + insert **outbox** row.
2. Worker polls outbox / CDC streams to broker.
3. Mark published after broker ack (with retry and dead-letter policy).

See [diagrams/outbox-and-idempotency.md](./diagrams/outbox-and-idempotency.md) and code: [java/TransactionalOutbox.java](./java/TransactionalOutbox.java), [go/transactional_outbox.go](./go/transactional_outbox.go).

**How it fails:** Publishing **inside** the HTTP request thread—timeouts roll back orders but message already flew. **Symptom:** ghost shipments or missing notifications. **Metric:** outbox **age p99**, unpublished row count.

### At-least-once delivery and idempotent consumers

Brokers (Kafka, SQS, Rabbit) typically provide **at-least-once** delivery. Consumers must be **idempotent**: processed-event ledger, natural keys (`orderId` unique on shipments), or compare-and-set on version.

Code: [java/IdempotentEventConsumer.java](./java/IdempotentEventConsumer.java), [go/idempotent_event_consumer.go](./go/idempotent_event_consumer.go).

**How it fails:** “We’ll use exactly-once” without end-to-end proof—often means **idempotent** with extra cost. **Symptom:** duplicate rows, duplicate emails. **Debug:** compare `event_id` in logs with `processed_events` table; check consumer **rebalance** storms.

### Event schema evolution

Treat events like **public APIs**:

- **Version** in envelope (`schemaVersion`, `type` suffix `OrderPlaced.v2`).
- **Compatible changes:** add optional fields; never repurpose semantics.
- **Breaking changes:** new topic or new type; dual-write period; consumers upgrade before publisher drops old fields.
- **Registry:** Confluent/Apicurio for Avro/Protobuf—CI checks compatibility.

**How it fails:** Deploy producer before consumers → deserialization errors → **poison messages** in DLQ. **Ops:** canary publish rate; consumer lag alert per **subscription**.

### CQRS (Command Query Responsibility Segregation)

**Intuition:** **Different models** for writes (enforce invariants) and reads (fast lists, search, dashboards)—kept consistent **eventually** by projecting from the write side or event stream.

| | Traditional single model | CQRS |
|---|--------------------------|------|
| **Writes** | Same tables as reads | Optimized OLTP / aggregate |
| **Reads** | Join heavy | Denormalized read DB, cache, ES |
| **Consistency** | Immediate | Lag SLA (e.g., 1–3 s p99) |
| **When** | Simple CRUD, one UI | Read shape diverges (feeds, admin grids) |
| **Cost** | Lower moving parts | Projection bugs, replay tooling |

**Production anchor:** Marketplace **order history** page reads from `order_summary_by_user` fed by `OrderPlaced` / `OrderShipped` events; **place order** still hits authoritative `orders` table. Product must label “status may take a few seconds to update.”

See [diagrams/cqrs-projection.md](./diagrams/cqrs-projection.md).

**Avoid CQRS when:** one team, one UI, no read/write shape split— you add lag and projection on-call without benefit.

### Event sourcing (overview)

**Intuition:** Store **sequence of events** as source of truth; current state = **replay** (or snapshots + replay).

| | CRUD + outbox events | Event sourcing |
|---|----------------------|----------------|
| **Source of truth** | Current row state | Event log |
| **Audit** | Separate audit table | Built-in |
| **Replay** | Re-publish from outbox/CDC | Native—rebuild projections |
| **Complexity** | Moderate | High—snapshots, versioning, GDPR deletes |
| **When** | Most ecommerce, notifications | Trading, collaborative editors, regulated audit |

**Production note:** Event sourcing is **not** required for EDA. Many systems are **event-driven** (notify after commit) without sourcing every aggregate. Adopt when **temporal queries**, **mandatory audit**, or **replay-driven products** justify storage and team skill. Practice the overlap with reservations and CQRS in **Case Study 11 (E-Commerce Inventory and Orders)** in [`Plan.md`](../../Plan.md) when you reach Tier 2 case studies.

### Choreography vs orchestration

| | Choreography (EDA default) | Orchestration (saga coordinator) |
|---|----------------------------|----------------------------------|
| **Flow** | Each service reacts to events | Central process tells participants |
| **Visibility** | Harder—distributed trace | Clear state machine |
| **Coupling** | Looser | Coordinator is dependency |
| **Use** | Notify, analytics, parallel reactions | Multi-step payments with compensations |

Long-running **payment + inventory + shipping** with compensations often needs **orchestrated saga** or **process manager** ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)), while **downstream notifications** stay choreographed.

### Ordering, causality, and partitions

- **Per-aggregate ordering:** partition key = `orderId` so `OrderPlaced` precedes `OrderCancelled` for same order.
- **Global ordering:** expensive; rarely needed.
- **Causality:** propagate `correlationId` / `causationId` in envelope for traces ([Chapter 26: Observability](../26-observability/README.md)).

**How it fails:** Hot partition key (`storeId` on Black Friday) → single consumer lag. **Mitigation:** salt keys for analytics only; keep **business** ordering on aggregate id.

### When EDA helps the organization

- **Decouple deploys:** fulfillment ships new pick logic without checkout deploy.
- **Fan-out:** one `UserRegistered` → email, CRM, analytics, search.
- **Buffer spikes:** flash sale writes orders; warehouse consumes at sustainable rate.
- **Audit narrative:** events are facts for compliance timelines.

See [diagrams/overview.md](./diagrams/overview.md).

## When to use / when to avoid

**Use when:**

- Multiple downstream contexts react to the same business moment.
- Peak write rate exceeds downstream capacity (buffer with backlog SLA).
- You need **temporal decoupling** (publisher does not know subscribers yet).
- Read models differ materially from write models (CQRS).

**Avoid when:**

- User needs **immediate** read-your-writes across contexts and product cannot show lag.
- Team lacks **consumer monitoring**, DLQ playbooks, and schema discipline.
- “Events everywhere” replaces a **2-service** sync flow—operational tax with no boundary win.
- Strong **cross-aggregate transaction** required—use saga/outbox + sync where invariants demand it, not fire-and-forget hope.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Duplicate shipments | Non-idempotent consumer | `processed_events`, unique constraints |
| Missing notifications | Lost publish (no outbox) | Outbox unpublished count, publisher errors |
| Consumer lag ↑ | Slow handler, rebalance, hot key | Per-partition lag, GC pauses, DB lock |
| DLQ flood after deploy | Schema break | Deserializer errors, `schemaVersion` mix |
| “Ghost” orders in BI | Event without commit | Compare event time vs DB `committed_at` |
| Stale order history | Projection bug / lag | Projection offset vs log end, replay job |

**Incident pattern:** Black Friday—checkout OK, **warehouse 6 h behind**. Fix: scale consumers, relieve hot partition, temporarily shed non-critical subscribers; long-term **backpressure** contract with product (degraded mode). **Do not** silently drop events.

## Architect takeaway

- **Decide:** event vs command per integration; thin vs fat payload; CQRS yes/no; sourcing only with replay/audit drivers.
- **Measure:** consumer lag p99, outbox age, DLQ rate, projection staleness, duplicate-handler rate (business KPI).
- **Document in design review:** partition key, idempotency strategy, schema compatibility policy, lag SLA for reads, poison-message playbook, PII on bus.

## Diagrams

- [Overview topology](./diagrams/overview.md)
- [Events vs commands](./diagrams/events-vs-commands.md)
- [CQRS projection](./diagrams/cqrs-projection.md)
- [Outbox and idempotency](./diagrams/outbox-and-idempotency.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Transactional outbox on place order | [TransactionalOutbox.java](./java/TransactionalOutbox.java) | [transactional_outbox.go](./go/transactional_outbox.go) |
| Idempotent `OrderPlaced` consumer | [IdempotentEventConsumer.java](./java/IdempotentEventConsumer.java) | [idempotent_event_consumer.go](./go/idempotent_event_consumer.go) |

**Production note:** Ship outbox + idempotent consumers before expanding topic fan-out; add CQRS read models when query load or join pain is measured—not at project kickoff.

## Related topics

- [Chapter 03: Domain-Driven Design](../03-domain-driven-design-and-bounded-contexts/README.md) — bounded contexts, published language, ACL
- [Chapter 09: API Design](../09-api-design/README.md) — commands over HTTP, idempotency keys on writes
- [Chapter 17: Microservices Architecture](../17-microservices-architecture/README.md) — sync vs async boundary, data ownership
- [Chapter 19: Kafka and Messaging](../19-kafka-and-messaging/README.md) — partitions, consumer groups, DLQ mechanics
- [Chapter 23: Idempotency, Sagas, and Distributed Transactions](../23-idempotency-sagas-and-distributed-transactions/README.md) — sagas, compensation, outbox depth
- [Chapter 20: Distributed Systems Fundamentals](../20-distributed-systems-fundamentals/README.md) — retries, backpressure, partial failure
- [Chapter 26: Observability](../26-observability/README.md) — correlation across publish/consume spans

## Interview preparation

See [interview-questions.md](./interview-questions.md) (25 questions — medium distributed chapter per rubric; user requested top 25).
