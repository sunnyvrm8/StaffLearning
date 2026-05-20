# Interview Questions: Microservices Architecture

**Bank size:** 25  
**Rationale:** Medium handbook chapter (decomposition, sync/async, gateway, mesh overview) per interview-bank-rubric; user requested top 25.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What is a microservice in one sentence, and what is it not?

**Answer:** A **microservice** is a **deployable unit** that owns a narrow business capability, its **data writes**, and its **operational lifecycle** (deploy, scale, on-call). It is **not** “every table is a service,” not “HTTP between classes,” and not a license to skip **contracts, tracing, and idempotency**. The goal is **independent change** under real ownership—not the smallest possible process count.

---

## 2. How is a bounded context different from a microservice, and why does the distinction matter in interviews?

**Answer:** A **bounded context** is a **model and language** boundary; a **microservice** is a **deployment** boundary. One context may run as a module in a monolith; one service may wrongly span two contexts. Interview strength: extract services where **ownership, scale, compliance, or failure isolation** justify network cost—after the context map exists ([Chapter 03](../03-domain-driven-design-and-bounded-contexts/README.md)), not because the diagram has boxes.

---

## 3. Name four legitimate reasons to split a service out of a monolith.

**Answer:** (1) **Independent deploy cadence**—payments team cannot wait on catalog releases. (2) **Scale profile**—search indexing needs 10× CPU vs checkout API. (3) **Blast radius**—fraud ML outage must not take order capture. (4) **Regulatory/compliance scope**—PCI-isolated payment service with narrower audit surface. Bad reason: “microservices are modern.” Each split should have a **metric** (deploy frequency, incident scope, cost).

---

## 4. What does “database per service” mean in practice, and what is the usual exception?

**Answer:** Each service is the **only writer** of its tables/schema; other teams read via **API or events**, not shared JDBC connections. **Exceptions:** read **replicas** or **projections** built from published events (CQRS), never ad-hoc cross-schema joins in production. “Temporary” shared DB during migration needs a **dated exit** plan—otherwise you ship a distributed monolith with network overhead.

---

## 5. Explain Conway’s law and how it should influence service boundaries.

**Answer:** **Conway’s law:** systems mirror communication structures. If checkout and catalog are one service but two VPs own them, you get political merges and frozen releases. Align **service ownership** with who changes policy and data together; use **facilitating structures** (platform, guilds) for cross-cutting standards—not one mega-team owning “the platform and all products.” Misalignment shows up as **nobody owns migrations** and incident ping-pong.

---

## 6. What is a distributed monolith?

**Answer:** A set of “microservices” that **must deploy together**, share a database or fat shared library, and integrate primarily via **long synchronous chains** without stable contracts. Users pay **network latency and partial failure** without gaining independent deployability. Smell: “we have 20 services but one release train.” Remedies: collapse wrong splits, add **async** boundaries, contract tests, and enforce data ownership.

---

## 7. Compare synchronous HTTP/gRPC integration vs asynchronous events for notifying fulfillment after checkout.

**Answer:** **Sync:** Order calls Shipment `POST /shipments` before returning 201—simple mental model, but user latency includes shipment service health; failures block checkout. **Async:** Order publishes `OrderPlaced`; shipment consumes—checkout stays fast, but **eventual consistency**, idempotent consumers, and **outbox** required. Production: **sync reserve/authorize** on critical path; **async** pick/pack/email. Numbers: if checkout SLA is 500 ms p99, a 4-hop sync chain at 120 ms each leaves no margin.

---

## 8. What is the difference between an API gateway and a BFF?

**Answer:** **API Gateway** handles **north-south cross-cutting**: TLS, authentication, rate limiting, routing, WAF, sometimes API keys—**client-agnostic**. **BFF (Backend for Frontend)** shapes responses for **one client class** (mobile vs partner): aggregation, field subsetting, device-specific caching. Gateway should stay thin; BFF may call multiple domain services. Anti-pattern: all business rules in gateway—undeployable god component.

---

## Application

## 9. A mobile order-detail page needs order, user profile, and shipment status. Where should aggregation live?

**Answer:** In a **mobile BFF** (or dedicated read API), not in the client calling three public services—avoids chatty clients and duplicated auth. BFF runs **parallel** fetches with **per-dependency timeouts**; degrade non-critical shipment if slow ([code examples](./java/BffOrderDetailsAggregation.java)). Domain services stay **normalized**; BFF owns presentation. Do not push aggregation into API gateway unless you accept gateway release coupling to mobile UX.

---

## 10. Checkout currently does `Order → Inventory → Payment → Tax` synchronously. p99 went from 400 ms to 2.1 s after the split. What do you investigate first?

**Answer:** **Distributed tracing** span breakdown per hop—often default **30 s client timeouts**, serial calls that were parallel in-process, or missing connection pools. Fix: **parallelize** independent calls, set **deadline budgets** (gateway 2 s → BFF 1.5 s → each callee 300 ms), cache read-only tax rules, move tax/fraud async if product allows. Measure **fan-out depth**; target ≤2 sync hops on write path. Do not add a cache before fixing serial waste.

---

## 11. How do you propagate timeouts across three microservices on a gRPC/HTTP chain?

**Answer:** Pass a **single deadline** from edge: `deadline = now + 800ms` in `context` / gRPC metadata; each hop uses **remaining time minus margin** (50–100 ms), never a fresh 30 s. On expiry, return **504/503 retryable** with correlation id. Document budget table in ADR. Java: `HttpRequest.timeout(remaining)`; Go: `context.WithDeadline`. Without propagation, each layer thinks it has full SLA—tail latency explodes.

---

## 12. When would you introduce a service mesh, and when would you defer it?

**Answer:** **Introduce** when you have **many** east-west services, need uniform **mTLS**, canary traffic splits, and consistent telemetry without N custom HTTP clients—**and** platform can operate Istio/Linkerd/Envoy. **Defer** for &lt;10 services, strong shared libraries already handle retries/traces, or team lacks mesh on-call—**mesh does not fix** bad decomposition. Alternative first: API gateway + good client middleware + centralized observability ([Chapter 26](../26-observability/README.md)).

---

## 13. Two teams want to query the same `orders` table for reporting and checkout. What do you recommend?

**Answer:** **Checkout Order service** remains sole writer; reporting gets **events** (`OrderPlaced`, `OrderUpdated`) into a warehouse or **read replica/projection** with lag documented (minutes acceptable for BI, not for inventory). Block direct OLTP queries from reporting jobs—they cause lock contention and hidden coupling. If migration is urgent, **read replica** with SLA “≤5 min lag” and no writes from reporting.

---

## 14. How do you version internal service-to-service APIs during a breaking proto change?

**Answer:** Run **dual stack**: `OrderService v1` and `v2` packages or separate RPCs; consumers migrate with feature flags. CI **breaking-change detection** on protos/OpenAPI. Maintain **two deployables** or one binary serving both until traffic &lt;1%. Never “big bang” 14 consumers on deploy night. Pair with **contract tests** from consumer repos against published artifacts.

---

## 15. What headers/metadata should cross every internal call for operability?

**Answer:** **`X-Request-Id` / trace context** (W3C `traceparent`), **deadline** or timeout hint, **tenant/user** for authz, optional **idempotency key** on writes. Map to gRPC metadata consistently. Logs and metrics must include **service.name**, **dependency**, **status**. Without these, a 503 at the gateway is undebuggable across six pods.

---

## Design & Trade-offs

## 16. Monolith vs microservices for a 30-engineer startup shipping weekly—what do you argue in a design review?

**Answer:** **Modular monolith** with clear packages per context, contract tests at module seams, single deploy until **pain is measured** (deploy queue, scaling one subsystem, compliance audit). Microservices add **network, distributed debugging, and data consistency** tax—~30 engineers often lose velocity. If extracting, pick **one** high-pain boundary (e.g., payments) with full ops kit, not 12 services at once.

---

## 17. A principal proposes “shared kernel” Order DTO jar used by all services. What are the trade-offs?

**Answer:** **Pros:** consistent shapes, faster bootstrapping. **Cons:** **deploy coupling**—every DTO change retriggers all services; contexts **conform** to one model, eroding bounded language; versioning becomes social, not technical. Prefer **published contracts** (proto/OpenAPI) with codegen and compatibility rules; share only tiny types (`Money`, `TenantId`) if needed. Red flag in interview: shared entity JAR across checkout and fulfillment.

---

## 18. Design a high-level architecture for marketplace checkout with ~5k orders/min peak and three client apps.

**Answer:** **Clients** → **API Gateway** (auth, rate limit) + **mobile BFF** / **web BFF** → **Order** (authoritative cart/submit), **Inventory** (reserve/release), **Payment** (PCI scope), **Pricing/Tax** as needed. **Sync:** reserve + pay auth on critical path (&lt;500 ms p99 budget). **Async:** `OrderPlaced` → fulfillment, email, search index via **Kafka** + outbox. **DB per service**; idempotency keys on submit ([Chapter 09](../09-api-design/README.md)). **Observability:** traces across gateway→BFF→services. **Scale 10×:** shard orders by `merchant_id`, cache catalog reads, don’t sync-call recommendations on hot path.

---

## 19. Compare “smart endpoints, dumb pipes” vs “smart middleware” (ESB/mesh-heavy) for a greenfield platform.

**Answer:** **Smart endpoints:** business logic in services; pipes are simple HTTP/events—teams own failure modes explicitly. **Smart middleware:** centralized routing, transformation, orchestration—faster initial wiring, risk of **opaque logic** and single failure domain. Modern bias: **dumb pipes** (HTTP/gRPC, Kafka) + **optional mesh** for policy, not business rules in brokers. ESB nostalgia often recreates the monolith in XML.

---

## 20. When is a modular monolith strictly better than 15 microservices for an e-commerce platform?

**Answer:** When **domain boundaries are still moving**, one primary product team owns the surface, ops maturity for **many SLOs** is low, and measured pain does not justify network/consistency cost. Modular monolith gives **refactor speed**, transactional invariants inside a context, and simpler debugging. Extract when **deploy frequency**, **incident blast**, or **scale** block a specific module—use strangler pattern, not big-bang.

---

## 21. How do sagas relate to microservices, and when would you avoid a orchestrated saga?

**Answer:** **Sagas** coordinate **multi-service writes** with compensating steps (reserve inventory → charge → confirm; on charge failure, release stock). Use **orchestration** when flow is complex and one owner needs visibility; **choreography** via events when steps are loosely coupled. Avoid heavy orchestration for **simple two-step** flows—outbox + single consumer may suffice ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)). Never default to **2PC** across services without latency and ops discussion.

---

## Stretch

## 22. During a partial outage, Inventory returns 503 and clients retry aggressively. What architectural controls limit blast radius?

**Answer:** **Rate limits** at gateway, **retry budgets** with jitter (mesh or client policy), **circuit breakers** on Order→Inventory, **bulkheads** so inventory threads don’t exhaust Order’s pool. Return **503 with Retry-After**; ensure **idempotent reserve**. Consider **cached soft stock** or queue checkout for async confirmation only if product accepts. Alert on **retry amplification** metric (outbound calls &gt; inbound).

---

## 23. You inherit 40 services and no service catalog. What do you do in the first 30 days as staff engineer?

**Answer:** Build a **dependency map** from traces and deploy data—not interviews alone. Identify **top 5 critical paths** (checkout, login), **highest fan-out**, and **shared DB** violations. Establish **golden signals** per tier-1 service, **contract repo**, and **freeze** on new sync hops without ADR. Pick **one** strangler to collapse or one extraction that fixes deploy pain—demonstrate value before “architecture transformation” slides.

---

## 24. Behavioral: Tell me about a microservices migration that went wrong and what you changed.

**Answer:** (STAR) Example: split order and inventory without **idempotent reserve**; flash sale retries **double-reserved** stock. **Action:** rolled back deploy, added **idempotency key** on reserve API, moved confirmation to **outbox event**, added trace dashboards on reserve path. **Result:** duplicate reservations dropped to zero; p99 checkout improved 40% by parallelizing tax. **Lesson:** split **after** contract and failure semantics, not after diagram approval—document timeout budgets in ADR for exec visibility.

---

## 25. A executive asks for “microservices by Q4” to reduce time-to-market. How do you respond?

**Answer:** Reframe to **outcomes**: deploy frequency, lead time, incident MTTR. Show data—if monolith deploy is weekly because of **testing/coordination**, splitting without CI/contract investment **slows** delivery. Propose **modular monolith + platform golden path** first; pilot **one** extraction with before/after metrics. Offer **decision record** with rejected big-bang 40-service plan. Staff+ credibility: align architecture to **measured pain**, not trend vocabulary.
