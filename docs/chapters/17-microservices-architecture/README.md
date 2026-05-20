# Chapter 17: Microservices Architecture

> **One line:** Microservices trade **independent deployability and failure isolation** for **network uncertainty**—only pay that tax when boundaries match real ownership and scale pain.

## Why this matters in production

A retailer splits a **modular monolith** into twelve services in one quarter because “we’re doing microservices.” Checkout now chains **Order → Inventory → Payment → Fraud → Tax** synchronously over HTTP. A 200 ms inventory blip becomes **2.8 s p99** at the gateway; one team’s deploy breaks another’s on-call. Data that lived in one transaction is **eventually consistent** without idempotency or outbox—support sees paid orders with no shipment. Stakeholders feel **slow checkout**, **cascading outages**, and **blame ping-pong**—not “we need Kubernetes.”

Microservices are an **organizational and operational** architecture: bounded contexts become deployable units ([Chapter 03: DDD](../03-domain-driven-design-and-bounded-contexts/README.md)), contracts freeze at the API layer ([Chapter 09: API Design](../09-api-design/README.md)), and partial failure becomes normal ([Chapter 20: Distributed Systems](../20-distributed-systems-fundamentals/README.md)). This chapter sits **after** cloud and traffic foundations ([Chapter 16: Cloud](../16-cloud-architecture/README.md), [Chapter 15: Load Balancing](../15-load-balancing-and-traffic-management/README.md)) and **before** event buses and deep consistency ([Chapter 18: Event-Driven](../18-event-driven-architecture/README.md), [Chapter 19: Kafka](../19-kafka-and-messaging/README.md), [Chapter 23: Idempotency & Sagas](../23-idempotency-sagas-and-distributed-transactions/README.md)).

## Core ideas

### Decomposition: what actually drives service boundaries

**Intuition:** Split where **independent change, scale, or failure** justify network hops—not where ER diagrams draw tables.

| Driver | Signal | Example split |
|--------|--------|----------------|
| **Bounded context** | Different ubiquitous language, policies | Checkout vs fulfillment vs billing |
| **Scale** | One domain dominates CPU/IO | Search index vs order API |
| **Release cadence** | Teams block each other’s deploys | Payments (regulated) vs catalog (daily) |
| **Blast radius** | Incidents should not take all revenue | Isolate card capture from recommendations |
| **Technology fit** | ML ranking vs CRUD | Recommendation service on GPU fleet |

**Conway’s law:** Service boundaries that fight team structure become **ownership vacuums**—two teams “share” one service and nobody owns schema migrations.

**Anti-pattern:** **Entity-per-table** microservices (`UserService`, `AddressService`, `PhoneService`) with chatty sync calls—distributed monolith with worse ops.

See [diagrams/overview.md](./diagrams/overview.md).

### Modular monolith vs microservices

| | Modular monolith | Microservices |
|---|------------------|---------------|
| **Deploy** | One unit (or few) | Many independent units |
| **Consistency** | In-process transactions possible | Per-service ACID; cross-service eventual |
| **Latency** | Function calls | Network + serialization |
| **When** | One product team, unclear domains, &lt;~100 engineers on one codebase | Proven context boundaries, ops maturity, clear SLO owners |
| **Risk** | Big-bang deploy, scaling whole app | Sync chains, contract drift, observability gaps |

**Production anchor:** Start with **clear modules** (packages, DB schemas per context) inside one deployable; extract the **first** service where pain is measurable—deploy conflicts on payments, inventory hot partition, or compliance audit scope—not a slide that says “target state: 40 services.”

### Data ownership: database per service (in principle)

**Intuition:** Each service **owns** its writes; others integrate via API or events—never “just query our table.”

- **Order service** owns `orders`, `order_lines`; exposes `GET /orders/{id}` and publishes `OrderPlaced`.
- **Inventory** owns stock reservations; checkout calls `POST /reserve` or consumes events—does not `JOIN` `inventory.sku` from order’s DB connection pool.
- **Shared read models** (CQRS projections) are **derived**, versioned, and rebuilt from events—not shared OLTP tables.

**How it fails:** “Temporary” shared database becomes permanent; any team’s migration breaks everyone. **Symptom:** mysterious cross-service deadlocks and “we can’t extract billing because 14 reports use `orders.status`.”

### Sync vs async integration

| | Synchronous (HTTP/gRPC) | Asynchronous (events/queue) |
|---|-------------------------|-----------------------------|
| **User waits?** | Often yes on critical path | No for downstream side effects |
| **Coupling** | Temporal—caller needs callee up | Looser—buffer in broker |
| **Consistency** | Easier to reason per request | Eventual; needs idempotency |
| **Failure** | Timeouts propagate; retry storms | Backlog, poison messages, lag |
| **Use** | Read-your-writes, reserve stock before confirm | Notify warehouse, send email, analytics |

**Rule of thumb:** Keep the **checkout critical path** short—sync only what the user needs to see **now** (price, stock reservation, payment auth). Push **shipment creation, loyalty points, search index** to async with outbox ([Chapter 18](../18-event-driven-architecture/README.md)).

See [diagrams/sync-vs-async.md](./diagrams/sync-vs-async.md).

### API gateway vs BFF vs core services

| Layer | Role | Owns |
|-------|------|--------|
| **API Gateway** | North-south: TLS termination, auth, rate limits, routing, WAF | Cross-cutting policy; thin |
| **BFF (Backend for Frontend)** | Shapes APIs per client (mobile vs web); aggregation | Client-specific composition, caching |
| **Domain services** | Business rules, authoritative state | Aggregates, invariants |

**Intuition:** Gateway is **airport security**; BFF is **tour guide** for one passenger type; services are **departments** with authority.

**Avoid:** Fat gateway with business logic—becomes undeployable shared monolith. **Avoid:** One BFF per microservice—use domain APIs and compose in BFF only where client needs differ.

### Service mesh (overview)

**Intuition:** Move **east-west** concerns—mTLS, retries, timeouts, traffic split, telemetry—to a **sidecar** so app code stays business-focused.

**Use when:** Many polyglot services, uniform mTLS policy, canary by percentage, L7 metrics without rewriting every HTTP client.

**Avoid when:** &lt;10 services, strong client libraries already propagate traces and deadlines, team cannot operate Envoy/Istio/Linkerd on-call. **Mesh does not fix** wrong boundaries or missing idempotency—it amplifies traffic you already send.

See [diagrams/gateway-bff-mesh.md](./diagrams/gateway-bff-mesh.md).

### The distributed monolith

**Symptoms:**

- Deploy **all** services together “or things break.”
- **Synchronous chains** &gt;3 hops on hot path without caching.
- **Shared database** or shared library DTOs as integration.
- **No** contract tests; breaking changes discovered in prod.
- **Circular** dependencies (A→B→C→A).

**Remedies:** Introduce events for non-critical paths, strangler-fig extraction, **freeze contracts** (OpenAPI/proto CI), collapse wrongly split services, invest in tracing across hops ([Chapter 26: Observability](../26-observability/README.md)).

### Timeouts, deadlines, and bulkheads

**Intuition:** Every hop eats the user’s **timeout budget**—child calls must use **remaining** time, not a fresh 30 s default.

- Gateway: 2 s total for mobile checkout read.
- BFF: 300 ms per downstream; degrade optional data (shipment tracking) on timeout.
- East-west: propagate `context` / gRPC deadline; **fail fast** when budget exhausted.

**Bulkhead:** Separate thread pools or connection limits per dependency so one slow fraud service does not exhaust all workers ([Chapter 02: Circuit breaker](../02-design-patterns/README.md)).

Code: [java/BffOrderDetailsAggregation.java](./java/BffOrderDetailsAggregation.java), [java/EastWestDeadlineClient.java](./java/EastWestDeadlineClient.java).

### Team topology and platform

**Intuition:** Microservices assume **you can deploy, monitor, and secure** each service—usually with platform support ([Chapter 14: Kubernetes](../14-kubernetes-and-container-orchestration/README.md), [Chapter 29: Platform Engineering](../29-platform-engineering-and-internal-developer-platforms/README.md)).

- **Stream-aligned teams** own a context end-to-end (API + data + runbooks).
- **Platform** provides golden paths: CI, observability, secrets, service templates—not every team hand-rolling mesh config.

## When to use / when to avoid

**Use when:**

- Bounded contexts are stable; contracts exist ([Chapter 09](../09-api-design/README.md)).
- Independent scale, compliance, or deploy cadence is proven—not hypothetical.
- Ops can run **many** SLOs, dashboards, and on-call rotations.
- You have a story for **cross-service writes** (sagas, outbox, idempotency).

**Avoid when:**

- Greenfield with one team—**modular monolith** ships faster.
- “Resume-driven” split before domain discovery.
- No tracing/metrics—debugging three-hop failures blind.
- Data model is one big ball of mud—fix modeling first ([Chapter 10: Database Design](../10-database-design-and-data-modeling/README.md)).

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| p99 spikes after split | Sync fan-out, missing deadlines | Trace span depth, per-hop latency |
| Duplicate side effects | Retries without idempotency | Idempotency hit rate, consumer lag |
| “Works on my machine” integrations | Undocumented contracts | Contract test failures in CI |
| One team blocks all deploys | Shared library / DB | Coupling graph, DORA deploy frequency |
| Mystery 500 at gateway | Deep chain, no bulkhead | Thread pool saturation, error budget burn |

**Incident patterns:** Retry storm after inventory 503 doubles load; **thundering herd** on cold BFF after cache flush; **version skew** when only half the fleet speaks new proto field.

**Debugging hooks:** Trace `request_id` from gateway through BFF to each service; metric **dependency health** (error rate, timeout rate per callee); **deploy markers** on dashboards during canaries.

## Architect takeaway

- **Decide:** Monolith vs extract; sync vs async per integration; gateway/BFF split; mesh only if ops ready; DB-per-service enforcement.
- **Measure:** End-to-end latency budget; deploy independence per team; cross-service error rate; event consumer lag; % of traffic on deprecated API versions.
- **Document in design review:** Ownership map (context → service → on-call); timeout budget table; data authority; saga/outbox for each cross-write; rejection of “shared DB for now.”

## Diagrams

- [Topology overview](./diagrams/overview.md)
- [Sync vs async integration](./diagrams/sync-vs-async.md)
- [Gateway, BFF, and mesh](./diagrams/gateway-bff-mesh.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| BFF parallel aggregation with degradation | [java/BffOrderDetailsAggregation.java](./java/BffOrderDetailsAggregation.java) | [go/bff_order_details.go](./go/bff_order_details.go) |
| East-west call with deadline budget | [java/EastWestDeadlineClient.java](./java/EastWestDeadlineClient.java) | [go/east_west_deadline.go](./go/east_west_deadline.go) |

**Production note:** Ship **deadline propagation** and **structured tracing** on the first extracted service—retrofitting through six hops is painful. Treat optional BFF fields (recommendations, shipment ETA) as **degradable** with explicit product rules, not accidental timeouts.

## Related topics

- [Chapter 03: Domain-Driven Design](../03-domain-driven-design-and-bounded-contexts/README.md) — bounded contexts become service boundaries.
- [Chapter 09: API Design](../09-api-design/README.md) — contracts before split; idempotency at the edge.
- [Chapter 15: Load Balancing](../15-load-balancing-and-traffic-management/README.md) — routing and health checks in front of fleets.
- [Chapter 18: Event-Driven Architecture](../18-event-driven-architecture/README.md) — async integration after sync pain appears.
- [Chapter 23: Idempotency, Sagas](../23-idempotency-sagas-and-distributed-transactions/README.md) — cross-service writes without 2PC fantasy.
- [Chapter 24: Reliability Engineering](../24-reliability-engineering/README.md) — SLOs and error budgets per service.

## Interview preparation

See [interview-questions.md](./interview-questions.md) (25 questions — medium distributed chapter per rubric; user requested top 25).
