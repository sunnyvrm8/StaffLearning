# Interview Questions: Idempotency, Sagas, and Distributed Transactions

**Bank size:** 10  
**Rationale:** Pattern-focused chapter (payments/inventory); rubric 10 for initial bank—high interview density per question.  
**Last updated:** 2026-05-20

---

## Core

## 1. Why do distributed systems “default to at-least-once,” and what must every write handler assume?

**Answer:** TCP, HTTP timeouts, broker redelivery, and client retries mean **duplicates are normal**—not edge cases. Handlers must be **idempotent** or **deduplicated**: same business key → same outcome, no double charge or double ship. At-least-once on the wire + idempotent consumer ≈ **exactly-once effect** on state. Measure **duplicate delivery rate** (often 0.1–1% under retry storms) and **dedup cache hit ratio**.

---

## 2. How does an Idempotency-Key work for `POST /payments`?

**Answer:** Client sends stable key (UUID) in header; server **records key → result** in a store with TTL (24–72 h typical). First request executes charge and persists mapping; retries return **same HTTP status and body** without re-calling PSP. Requirements: **unique constraint** on key, atomic insert-or-return, and PSP support for idempotent provider requests where available. Without it, timeout + retry → **double capture**—classic incident ([Chapter 20](../20-distributed-systems-fundamentals/README.md)).

---

## 3. Compare saga **orchestration** vs **choreography** for order → payment → inventory → shipment.

**Answer:** **Orchestration:** central coordinator issues commands and tracks state—clear visibility, easier compensations, risk of orchestrator as bottleneck/SPOF. **Choreography:** each service reacts to events—loose coupling, harder to trace “where is order 7?” without good tracing. Production hybrid: **orchestrate** money and inventory **reserve**; **choreograph** notifications and analytics. Both need **idempotent** steps and **correlation IDs**.

---

## 4. What is a compensating transaction, and how is it different from rolling back a database transaction?

**Answer:** **DB rollback** undoes within one ACID boundary. **Compensating transaction** is a **new forward business action**: `RefundPayment`, `ReleaseInventory`—may fail, be partial, or require human ops. Sagas model **long-running** workflows without holding locks across services. Design compensations as **idempotent** and **semantic** (“cancel shipment if not picked”) not literal undo of bytes.

---

## 5. When would you still use two-phase commit (2PC), and when should you reject it?

**Answer:** **Use 2PC** rarely: colocated resources (XA within one cluster), some internal batch jobs, legacy mainframe integration with clear DBA ownership. **Reject** for high-throughput microservices across regions: **blocking**, **coordinator failure** stalls participants, **latency** multiplies. Prefer **saga + outbox**, **single write owner** per aggregate, or **event sourcing** with deterministic projections. If interview pushes 2PC for checkout at 10k TPS, explain **lock duration** and **partition** behavior.

---

## Stretch

## 6. Explain the transactional outbox pattern and how it pairs with idempotent consumers.

**Answer:** In one **local DB transaction**: update business row + insert row into **outbox** table. Relay process publishes to Kafka/SNS and marks sent. Consumers process with **dedup key** (event id). Avoids “DB committed, message never sent” or dual-write races. Trade-off: **seconds** of publish latency, relay ops, and outbox cleanup. Links event-driven architecture ([Chapter 18](../18-event-driven-architecture/README.md)) to reliable cross-service workflows.

---

## 7. Walk through a happy-path and one failure for: reserve inventory → charge card → create shipment (saga).

**Answer:** **Happy:** Reserve (pending) → Charge (captured) → Ship (created); mark saga complete. **Failure after charge, shipment fails:** compensate **refund** + **release inventory**; saga state `COMPENSATING`. Risks: refund timeout leaves **orphan capture**—retry compensations with idempotency, alert on stuck states > N minutes. **Failure before charge:** release reservation only. Store saga state in **durable table**; never infer from logs alone.

---

## 8. What is TCC (Try-Confirm-Cancel), and when is it preferable to “naive saga + compensate”?

**Answer:** **Try:** reserve resources (soft hold). **Confirm:** commit. **Cancel:** release hold. Gives **business-level** two-phase without locking foreign DBs—common in payments/wallets. Prefer when downstream supports **explicit reserve** APIs and holds expire (TTL). Cost: **three calls** per step, complex client libraries, must handle **hung Try** (expiry sweeper). Naive compensate is simpler when only **final** states matter and reserves are not modeled.

---

## 9. Debug: Kafka shows `OrderPlaced` processed 47 times; warehouse shipped once. What checks?

**Answer:** (1) Consumer **idempotency store**—is `eventId` unique constraint working? (2) **Commit offset** before vs after processing—at-most-once loss vs at-least-once duplicate. (3) **Rebalance** storm reprocessing old offsets. (4) Producer **duplicate publish** without idempotent producer. Fix: dedup table, process-then-commit or transactional consumer, monitor **consumer lag** and **duplicate skip rate**. One ship for 47 delivers means handler dedup worked once—find why 46 weren’t skipped in metrics.

---

## 10. System design: checkout must not oversell during flash sale; payment partner is async webhook. Outline consistency and idempotency choices.

**Answer:** **Inventory:** partition by SKU, **optimistic reservation** with TTL (30–120 s), single writer per shard or row-level version; release on timeout. **Payment:** `Idempotency-Key` on create; webhook handler keyed by `provider_event_id` dedup. **Saga state** documents `RESERVED → PAID → CONFIRMED`; async webhook advances state idempotently. **Avoid** cross-service 2PC. Numbers: 50k RPS browse, 5k RPS checkout attempts—size reservation store and dedup cache (Redis/DB) for **peak keys × TTL**. SLO: 0 oversell tickets; reconcile nightly with warehouse.
