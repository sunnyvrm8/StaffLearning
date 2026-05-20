# Interview Questions: Event-Driven Architecture

**Bank size:** 25  
**Rationale:** Medium handbook chapter (events vs commands, CQRS, outbox, sourcing overview) per interview-bank-rubric; user requested top 25.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What is event-driven architecture in one sentence, and what problem does it solve?

**Answer:** **EDA** integrates systems by publishing **facts after state changes** so subscribers react **without the publisher knowing who listens**. It solves **temporal coupling** and **fan-out** (one checkout moment triggers fulfillment, email, search, analytics) better than long synchronous chains. It does **not** remove consistency work—you trade immediate cross-service truth for **eventual consistency**, idempotency, and schema discipline.

---

## 2. How is an event different from a command?

**Answer:** An **event** is past tense—a fact (`PaymentCaptured`, `OrderPlaced`) with **0..N** interested subscribers. A **command** is imperative intent (`ReserveInventory`) aimed at **one** handler accountable for side effects. Cross-context integration after commit should usually be **events**; directed work inside or to a single service can be **commands** on a dedicated queue/API. Mixing them on a shared “events” topic (`CreateShipment` broadcast) recreates tight coupling and unclear ownership.

---

## 3. What is the transactional outbox pattern and why is “write DB, then publish to Kafka” unsafe?

**Answer:** **Outbox:** in **one DB transaction**, update the aggregate and insert an **outbox row**; a separate worker publishes to the broker and marks rows published after ack. **Unsafe dual-write:** if publish succeeds and DB rolls back, downstream acts on **ghost** events; if DB commits and publish fails, you **lose** notifications. At ~1k orders/sec, even a 0.1% publish failure without outbox is thousands of broken orders per hour. Measure **outbox age p99** and unpublished row count.

---

## 4. Why do most message brokers only guarantee at-least-once delivery, and what must consumers do?

**Answer:** Brokers retry on network failure, consumer crash before ack, or **rebalance** redelivery—true end-to-end exactly-once across DB + bus + email is rare and expensive. Consumers must be **idempotent**: `processed_events` table keyed by `event_id`, natural unique keys (`order_id` on shipments), or version checks. “Exactly-once” in vendor docs usually means **idempotent semantics** with dedup storage—not magic. Symptom of gaps: **duplicate emails** or duplicate rows after deploys.

---

## 5. What is CQRS, and when is it worth the operational cost?

**Answer:** **CQRS** separates **write models** (enforce invariants, OLTP) from **read models** (denormalized lists, search, dashboards) kept aligned **eventually** via events or projections. **Worth it** when read shapes diverge (marketplace order history, admin grids, feeds) and join-heavy OLTP cannot meet latency at target QPS. **Not worth it** for a single team, one CRUD UI, no measured read pain—you add **projection bugs**, lag SLAs, and replay tooling for little gain. Product must accept **seconds** of read staleness or show “updating…” UX.

---

## 6. What is event sourcing, and how does it differ from “CRUD + outbox events”?

**Answer:** **Event sourcing** stores the **event log** as source of truth; current state is **replay** (often snapshots + tail). **CRUD + outbox** stores **current row** as truth and emits notifications. Sourcing gives **audit and replay** natively but adds snapshot strategy, schema versioning on the log, and hard **GDPR delete** problems. Most ecommerce is **event-driven without full sourcing**; sourcing fits trading, regulated audit, or products where **rebuild from history** is core—not slide-deck default.

---

## 7. Compare thin (notification) events vs fat (event-carried state transfer) payloads.

**Answer:** **Thin:** `orderId`, `type`—small, publisher owns less consumer detail; consumers **call back** or read APIs (coupling, load at 10⁴+ events/sec per type). **Fat:** full snapshot in event—fewer sync calls, better offline consumers; **schema churn** and PII on the bus. Start thin with documented enrichment APIs; add fields when measured callback pain or fan-out cost justifies it. Never put **PAN/SSN** on a topic “for convenience.”

---

## 8. What is choreography vs orchestration in distributed flows?

**Answer:** **Choreography:** each service reacts to events—loose coupling, harder global visibility (trace + correlation ids required). **Orchestration:** a **saga coordinator** drives steps and compensations—clear state machine, coordinator is a **dependency** and failure domain. Use choreography for **notify, analytics, parallel reactions**; orchestration when **payment → inventory → ship** needs compensations and explicit timeouts ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)).

---

## Application

## 9. Checkout commits an order in PostgreSQL. Fulfillment must create a shipment. Walk through the recommended integration.

**Answer:** **Order service** commits `orders` + **outbox** row `OrderPlaced` in one transaction ([TransactionalOutbox](./java/TransactionalOutbox.java)). Worker publishes to `order.events` with partition key `orderId`. **Fulfillment consumer** idempotently inserts shipment (`UNIQUE(order_id)` or processed-event ledger). Do **not** HTTP-call fulfillment inside the checkout request unless product requires synchronous ship label—keeps checkout p99 independent of warehouse latency. SLA: fulfillment may lag **seconds to minutes**; product copy reflects that.

---

## 10. A consumer processes `OrderPlaced` twice and creates two shipments. How do you fix it and prevent recurrence?

**Answer:** **Fix:** dedupe data (merge/cancel duplicate shipments), add **unique constraint** on `order_id`, backfill `processed_events` if missing. **Prevent:** idempotent handler checking `event_id` before side effects; store **business key** idempotency, not only Kafka offset (offsets reset on rebalance). **Ops:** alert on `duplicate_shipment_rate`; trace logs should show same `event_id` twice. Root cause often “we assumed exactly-once from Kafka.”

---

## 11. After deploy, consumer lag on `order.events` jumps from 200 ms to 45 minutes. Checkout API is healthy. What do you check first?

**Answer:** **Per-partition lag** (hot key?), **consumer group rebalance** storm, **deserialization errors** → DLQ, slow handler (DB lock, N+1 API calls per event), **scaled-down consumers**, or **poison message** retry loop. Compare **publish rate** vs **consume rate** since deploy. If schema change: producer ahead of consumers. Short-term: scale consumers, skip/shed non-critical subscribers per playbook; long-term: fix handler, partition strategy, and compatibility CI.

---

## 12. Product demands “order status updates instantly everywhere” after place order. Can pure EDA satisfy that?

**Answer:** **Pure async** cannot guarantee **instant** cross-context read-your-writes without **sync read** of authoritative service or **client-side** wait on a known projection. Options: (1) return **201** with order state from write DB; (2) **poll** read API with short timeout; (3) **WebSocket/SSE** fed by projection with sub-second lag SLA; (4) **sync call** only where legally/UX-required. Interview strength: name **lag SLA** (e.g., p99 projection ≤2 s) and degrade gracefully—do not promise zero lag on six independent databases.

---

## 13. How should partition keys be chosen for `OrderPlaced` and `OrderCancelled` for the same order?

**Answer:** Use **`orderId`** (aggregate id) as partition key so all lifecycle events for one order stay **ordered** on one partition. **Avoid** `storeId` on Black Friday for all order events—that creates a **hot partition** and one consumer drowning while others idle. Global total order across partitions is unnecessary; **per-aggregate** ordering is the usual requirement. Analytics-only topics may use salted keys at cost of order.

---

## 14. You need a customer order-history page with filters and sort. The write model is normalized OLTP. What pattern fits?

**Answer:** **CQRS projection:** maintain `order_summary_by_user` (or search index) updated by `OrderPlaced`, `OrderShipped`, `OrderCancelled` consumers ([diagrams/cqrs-projection.md](./diagrams/cqrs-projection.md)). Read API hits projection; **write path** unchanged. Document **staleness** (e.g., 1–3 s p99). Rebuild path: replay from event log or snapshot + incremental. Do not run heavy joins on checkout OLTP at 500 QPS read traffic.

---

## 15. How do you evolve an event schema when adding a required field?

**Answer:** **Avoid** required fields on v1 topics without version bump. Preferred: **new event type** or `schemaVersion: 2`, **dual-publish** period, consumers upgrade first, then producers stop v1. With registry (Avro/Protobuf): **backward-compatible** changes only (add optional fields). Breaking change needs **new topic** or consumer isolation. CI compatibility checks on PR. Deploy order: **consumers before producers** or feature-flagged publishers—else DLQ flood.

---

## Design & Trade-offs

## 16. When would you choose synchronous HTTP between Order and Inventory vs `OrderPlaced` events?

**Answer:** **Sync HTTP/gRPC** when checkout **must know now** that stock is reserved before charging (user waits; failure blocks sale)—with **idempotent reserve** and tight timeouts (≤150–300 ms budget). **Events** when fulfillment, email, and search can lag; buffer flash-sale spikes (10× write burst, warehouse consumes at 2× sustainable). Hybrid is normal: **sync reserve + async ship**. Numbers: 4 sync hops × 120 ms = 480 ms before payment—often unacceptable at 500 ms p99 SLA.

---

## 17. Compare event-driven integration vs a nightly batch ETL from the orders table.

**Answer:** **EDA:** near-real-time (seconds–minutes), decoupled deploys, multiple subscribers, operational cost (lag, DLQ, schema). **Batch ETL:** simpler ops, hours lag, good for **BI/warehouse** where freshness is daily. Anti-pattern: **dual truth**—events and batch disagree because reporting still scrapes OLTP. Pick EDA for **operational reactions** (ship, notify); batch for **analytics** unless product needs sub-minute dashboards—then projection or CDC to warehouse.

---

## 18. Design event flows for a notification system that must send email, SMS, and push on `UserRegistered` (~2k registrations/min peak).

**Answer:** **Auth service** commits user + outbox `UserRegistered` (thin: `userId`, `locale`). **Topic** `user.events`, partition by `userId`. **Three consumer groups** (email, SMS, push) so one slow channel does not block others—separate **lag alerts**. Each handler **idempotent** (`userId` + channel). **Preferences** service may filter before send. **DLQ** per channel with retry policy; PII only in encrypted payloads. **Scale 10×:** scale consumers per group; rate-limit external providers; consider **priority queue** for OTP vs marketing.

---

## 19. Your team proposes “event sourcing everything” for a standard B2C cart. What do you push back on?

**Answer:** Push back on **cost without drivers**: snapshot/replay tooling, team skill, GDPR erasure on immutable logs, query complexity for support tools. B2C cart rarely needs **time-travel audit** beyond `orders` + outbox + audit table. Offer **CRUD + outbox + selective CQRS** for order history; revisit sourcing only for **dispute/audit** subdomain with exec sign-off. Principal answer: match pattern to **measured** audit/replay requirement, not architecture fashion.

---

## 20. How does the outbox relate to the API idempotency key on `POST /orders`?

**Answer:** **Idempotency key** (HTTP) stops **duplicate orders** from client retries on the **write API**—same key returns same `orderId` without double insert. **Outbox** ensures **exactly one publish intent per committed order** to the bus. Both are needed: idempotency without outbox can commit once but publish twice if publisher retries wrong; outbox without API idempotency can still double-commit on client retry if server treats each POST as new. Document key retention (24–72 h) and outbox dedup by `event_id`.

---

## Stretch

## 21. DLQ depth for `OrderPlaced` consumers tripled after a producer deploy. Walk through diagnosis.

**Answer:** Sample DLQ messages: **deserialization** (schemaVersion mismatch), **validation** (new required field null), **downstream DB** constraint. Compare producer **deploy time** to DLQ spike; check **schema registry** compatibility CI. Roll forward fix: **consumer** deploy tolerant reader, or **producer** rollback / dual-write. Replay DLQ with rate limit after fix. **Prevent:** consumer-first deploy, canary producer traffic, contract tests in CI.

---

## 22. “We use Kafka exactly-once.” A finance reconciliation job still finds duplicate `PaymentCaptured` rows. Explain.

**Answer:** Broker **exactly-once** (transactions) covers **broker boundaries**, not **your DB + side effects** unless you use specialized consume-transform-produce with idempotent producer and still handle **external systems** (Stripe, email). Finance duplicates usually mean **consumer not idempotent** or **at-least-once + retry** from application layer. Fix: **ledger unique** on `payment_id`, idempotent consumer, reconcile job keyed on provider id. Interview: never claim E2E exactly-once without naming **every** side-effect store.

---

## 23. Behavioral: Describe an incident where async messaging caused customer-visible harm and what you changed.

**Answer:** (STAR) Example: published `OrderPlaced` **before** DB commit on timeout race; warehouse shipped **unpaid** orders. **Action:** implemented **transactional outbox**, halted consumer, reconciled against `orders.status=PAID`, added **metric** `events_without_matching_order`. **Result:** zero ghost shipments in 30 days; outbox age p99 &lt;2 s. **Lesson:** EDA requires **same rigor as payments**—facts must match committed truth; async is not “fire and forget.”

---

## 24. A downstream team wants to subscribe to your `OrderPlaced` topic for a one-off marketing experiment. What governance do you apply?

**Answer:** Treat topic as **published language**: register consumer, review **PII** in payload, agree **lag SLA** and **schema change** notification. Prefer **thin events** + their ACL if they need enriched PII. New consumer group should not share **processing logic** with fulfillment—isolate failure domains. Time-box experiment with **offboarding** date; avoid **fat events** shaped for one subscriber that become permanent contract debt.

---

## 25. Principal scope: Your org has 12 topics and no schema registry. Executives want “real-time everything.” What is your 90-day plan?

**Answer:** **Month 1:** inventory topics/consumers, **tier-1 flows** (checkout → fulfillment), add **outbox** where missing, correlation ids, lag/DLQ dashboards. **Month 2:** **schema registry** + CI compatibility, naming standards (`domain.events`), idempotency audit on tier-1 consumers. **Month 3:** **CQRS** only on measured read pain; reject new topics without **owner** and retention policy. Communicate: “real-time” = **defined lag SLAs**, not zero latency—batch remains for BI. Success metrics: duplicate side-effect rate, outbox age, tier-1 consumer lag p99—not topic count.
