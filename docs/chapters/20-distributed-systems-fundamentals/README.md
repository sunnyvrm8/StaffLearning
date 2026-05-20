# Chapter 20: Distributed Systems Fundamentals

> **One line:** In a distributed system, **independent parts fail independently**—your job is to bound time, bound retries, shed load, and make side effects safe when the network lies.

## Why this matters in production

A **payments** team ships checkout with generous HTTP timeouts and automatic retries on every 5xx. Inventory is briefly overloaded; clients and middle tiers **retry in sync**, turning a 30% error spike into **100% overload** for ten minutes. Meanwhile, support sees **double charges** because retries hit `POST /charge` without an idempotency key—the provider applied two captures for one order. On-call traces show spans ending at different wall-clock times on two hosts; someone “fixes” ordering with `created_at` and ships a regression where refunds sort before captures.

Stakeholders feel **unpredictable latency**, **duplicate money movement**, and **incidents that worsen under recovery**—not “we need stronger consistency” yet. This chapter is the **umbrella** for those mechanics: clocks, partitions, retries, timeouts, and backpressure. It sits **after** messaging foundations ([Chapter 19: Kafka](../19-kafka-and-messaging/README.md)) and **before** formal trade-off lenses ([Chapter 21: CAP and PACELC](../21-cap-theorem-and-pacelc/README.md), [Chapter 22: Consistency Models](../22-consistency-models-and-consensus/README.md)). Microservices ([Chapter 17](../17-microservices-architecture/README.md)) multiply these problems; idempotency and sagas ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)) are the patterns that close the loop on unsafe retries.

## Core ideas

### Partial failure is the default

**Intuition:** A single JVM process fails as a unit; a distributed path fails **one hop at a time**—CPU fine, network slow, dependency down, disk full on one replica.

| Failure mode | User-visible symptom | First metric |
|--------------|----------------------|--------------|
| **Timeout** | Spinner, 504 at edge | Client vs server span gap |
| **Partition** | Split views, stale reads | Error rate per AZ / broker |
| **Slow (not down)** | p99 tail, queue growth | Thread pool / channel depth |
| **Ambiguous** | “Did payment succeed?” | Idempotency store hit rate |

There is no global `if (system.healthy())`. Design for **degradation**: fail fast inside budget, retry only when safe, shed when saturated.

See [diagrams/overview.md](./diagrams/overview.md).

### Clocks: wall time, monotonic time, and logical ordering

**Intuition:** Wall clocks **skew** (NTP, VM freeze, leap smear); monotonic clocks measure **duration** on one machine but do not compare across nodes.

- **Use wall clock** for human reports, TTL expiry, and **approximate** retention—not for “which write won” across regions.
- **Use monotonic** (`System.nanoTime`, `time.Since`) for **timeouts and deadlines** on a single host.
- **Use logical ordering** when causality matters: database transaction IDs, Kafka **partition offset**, version columns, or vector clocks at architect depth ([Chapter 22](../22-consistency-models-and-consensus/README.md)).

**Production anchor:** “Last updated wins” with `timestamp` across two data centers lost money when **clock skew** made stale inventory authoritative. Fix: **version** per key or single writer per partition.

See [diagrams/clocks-and-ordering.md](./diagrams/clocks-and-ordering.md).

### Network partitions and split brain (architect depth)

**Intuition:** A **partition** is not “the internet is down”—it is **some** nodes cannot talk to **some** others while others still can.

During partition, you choose what to sacrifice (preview of [Chapter 21](../21-cap-theorem-and-pacelc/README.md)):

- **Stop writes** to avoid split brain (CP-leaning: etcd, ZooKeeper quorum).
- **Keep serving** with possible divergence (AP-leaning: multi-master cache, async replicas).

**Split brain:** two leaders both accept writes; merge is expensive or impossible. Mitigations: **quorum**, **fencing tokens**, **lease with TTL** shorter than recovery time, **single partition owner** per key.

**Ops signal:** divergent replica lag, two nodes claiming primary, conflicting row versions after “network blip.”

### Timeouts and deadlines: one budget for the user

**Intuition:** Every hop spends the same **user-visible** SLA; children must use **remaining** time, not a fresh 30 s default.

| Layer | Typical mobile checkout read | Notes |
|-------|------------------------------|-------|
| Client | 2–3 s total | Abandon UI |
| Gateway | 2 s | TLS + auth + route |
| Service | 1.5 s | Business logic |
| Each dependency | 200–400 ms | Parallel where possible |

**Cancellation:** propagate `context` / gRPC deadline so work stops when the caller left—saves CPU and reduces lock hold time ([Chapter 17](../17-microservices-architecture/README.md) east-west examples).

**How it fails:** Each service sets 60 s timeout; user sees 2 s failure at edge while **orphan work** continues downstream, filling pools and holding inventory locks.

### Retries: necessary, dangerous, never default-on POST

**Intuition:** Retries turn **transient** faults into recovery—or **amplify** load when everyone retries together.

**Safe retry checklist:**

1. **Idempotent** operation or **idempotency key** stored server-side ([Chapter 09: API Design](../09-api-design/README.md)).
2. **Bounded** attempts (e.g., 3–4) inside parent **deadline**.
3. **Exponential backoff + full jitter** so retries desynchronize ([diagrams/retry-storms.md](./diagrams/retry-storms.md)).
4. **Retry only idempotent HTTP methods** or documented 503/429; never blind retry on 400/409.
5. **Coordinate** client, gateway, and service retries—three layers × 5 retries = 125× load.

| Strategy | When | Risk |
|----------|------|------|
| **Immediate retry** | Single client, rare blip | Thundering herd |
| **Backoff + jitter** | 5xx, timeout | Added tail latency |
| **Retry-after header** | Rate limit 429 | Ignored → ban |
| **No retry; queue** | Non-idempotent side effect | Complexity |

Code: [java/RetryWithJitter.java](./java/RetryWithJitter.java), [go/retry_with_jitter.go](./go/retry_with_jitter.go).

**Ambiguous timeout:** TCP succeeded at provider, response lost—you must **reconcile** (GET by idempotency key, ledger query), not retry blindly.

### Backpressure: protect the system you have

**Intuition:** When arrival rate &gt; service rate, **unbounded queues** grow until memory or threads die; **backpressure** signals upstream to slow or fail fast.

Mechanisms:

- **Bounded worker pool / semaphore** — reject with 503 when full ([java/BoundedInFlight.java](./java/BoundedInFlight.java)).
- **Bounded channel** (Go) — same semantics ([go/bounded_in_flight.go](./go/bounded_in_flight.go)).
- **Queue with max length + DLQ** — async path ([Chapter 19](../19-kafka-and-messaging/README.md)).
- **Adaptive concurrency** (e.g., limit in-flight per dependency based on latency).
- **Load shedding** — drop optional work (recommendations) before checkout.

**Production anchor:** Flash sale at **~8k RPS**; checkout without in-flight cap accepted **50k goroutines** waiting on payment—GC pause drove **cascading failure**. Cap at 500 in-flight, 429 with `Retry-After`, p99 recovered.

See [diagrams/backpressure.md](./diagrams/backpressure.md).

### At-least-once, at-most-once, exactly-once (delivery semantics)

**Intuition:** Networks duplicate; consumers crash mid-process—you pick **semantics**, not magic.

| Semantics | Meaning | Typical building blocks |
|-----------|---------|-------------------------|
| **At-most-once** | May lose, never duplicate | Fire-and-forget, no retry |
| **At-least-once** | May duplicate, rarely lose | Retry + ack after process |
| **Exactly-once effect** | User sees once | Idempotent writes + dedup store + transactional outbox |

“Exactly-once” in marketing usually means **exactly-once processing effect** on business state, not one physical message on the wire.

### Failure detection and gray failures

**Intuition:** **Gray failure**—dependency returns 200 but slowly—drops throughput without tripping “down” alerts.

- Use **latency SLOs**, not just availability.
- **Synthetic probes** plus **real traffic** golden signals ([Chapter 24: Reliability](../24-reliability-engineering/README.md)).
- **Bulkheads** isolate one slow callee ([Chapter 02: Circuit breaker](../02-design-patterns/README.md)).

## When to use / when to avoid

**Use when:**

- More than one process or region on the critical path (including “monolith + cache + DB + SaaS”).
- Writes have **money or inventory** impact—idempotency and reconciliation are non-negotiable.
- Peak traffic is **spiky**—backpressure and shedding are designed, not accidental.
- You can trace **end-to-end deadline** consumption per hop ([Chapter 26: Observability](../26-observability/README.md)).

**Avoid when (for now):**

- Single-process CLI with local SQLite—still use timeouts, but skip distributed saga machinery.
- Retrying **without** idempotency “until we add keys next sprint.”
- Wall-clock **last-write-wins** across regions without version vectors or single-writer partitions.
- Unbounded thread pools “because Kubernetes will scale us”—scale hits **dependencies** first.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Outage deepens during recovery | Retry storm, no jitter | Retry rate vs error rate overlay |
| Duplicate charges | Retry on non-idempotent POST | Idempotency key presence, provider dashboard |
| p99 ↑, CPU OK | Gray failure, pool exhaustion | Per-dependency latency, queue depth |
| “Impossible” event order | Clock skew, no version | NTP drift, `version` conflicts |
| OOM under load | Unbounded queue / goroutines | In-flight gauge, heap, `GOMAXPROCS` |
| Stuck inventory locks | Orphan work after client timeout | Long-running spans after 504 |

**Incident patterns:** **Metastable failure**—system stable at high load but cannot return to low load after spike without manual drain. **Cascading timeout**—one slow DB holds 500 threads, all endpoints 503.

**Debugging hooks:** Trace **deadline exceeded** tags; metric **retry_attempt** histogram; **in_flight** gauge per endpoint; compare **client clock** vs **server `Date`** only for skew alarms, not ordering.

## Architect takeaway

- **Decide:** Per-integration timeout budget table; retry policy (who retries, how many, jitter); idempotency for every mutating API; backpressure limit and shedding order; partition behavior (stop vs degrade).
- **Measure:** End-to-end success within deadline; retry amplification factor; duplicate side-effect rate; queue depth; % requests rejected by backpressure; ambiguous-timeout reconciliation time.
- **Document in design review:** Failure model diagram per critical path; “no retry” list; idempotency key lifetime; behavior under partition; load test proving cap under 2× peak.

## Diagrams

- [Partial failure topology](./diagrams/overview.md)
- [Retry storm sequence](./diagrams/retry-storms.md)
- [Backpressure gate](./diagrams/backpressure.md)
- [Clocks and ordering](./diagrams/clocks-and-ordering.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Payment charge with backoff, jitter, idempotency key | [java/RetryWithJitter.java](./java/RetryWithJitter.java) | [go/retry_with_jitter.go](./go/retry_with_jitter.go) |
| Limit concurrent checkout work (fail fast when saturated) | [java/BoundedInFlight.java](./java/BoundedInFlight.java) | [go/bounded_in_flight.go](./go/bounded_in_flight.go) |

**Production note:** Ship **idempotency keys and deadline propagation** on the first external integration—not after the first duplicate-charge incident. Pair retries with **jitter** and a **global attempt budget**; pair scale-out with **in-flight caps** so HPA does not amplify overload on payment and ledger systems.

## Related topics

- [Chapter 08: Networking and HTTP](../08-networking-and-http/README.md) — TCP, TLS, connection pools, timeout layers.
- [Chapter 09: API Design](../09-api-design/README.md) — idempotency keys, error codes, retry-after.
- [Chapter 17: Microservices Architecture](../17-microservices-architecture/README.md) — multi-hop deadlines and sync chains.
- [Chapter 19: Kafka and Messaging](../19-kafka-and-messaging/README.md) — async buffering, consumer lag, ordering per partition.
- [Chapter 21: CAP Theorem and PACELC](../21-cap-theorem-and-pacelc/README.md) — partition trade-offs formalized.
- [Chapter 23: Idempotency, Sagas](../23-idempotency-sagas-and-distributed-transactions/README.md) — safe cross-service writes.
- [Chapter 24: Reliability Engineering](../24-reliability-engineering/README.md) — SLOs, error budgets, chaos.

## Interview preparation

See [interview-questions.md](./interview-questions.md) (25 questions — medium distributed fundamentals chapter per rubric; user requested top 25).
