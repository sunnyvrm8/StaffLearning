# Interview Questions: Kafka and Messaging

**Bank size:** 25  
**Rationale:** User-requested top 25; medium handbook chapter (topics, partitions, consumer groups, ordering, DLQ) per interview-bank-rubric.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What problem does a message broker solve that direct HTTP calls between services do not?

**Answer:** A broker **decouples time and availability**: producers can write when consumers are down, absorb **traffic spikes** in a durable buffer, and fan out one event to **many independent consumers** without N synchronous calls. You trade **immediate consistency** and simple debugging for **lag, duplicates, and ordering rules**. Use when checkout must not block on email, search indexing, and warehouse—each at different speeds and failure modes.

---

## 2. How is a Kafka topic different from a traditional message queue?

**Answer:** Kafka stores an **append-only log** per partition with **retention**; consumers track **offsets** and can **replay**. Classic work queues often **delete** after ack and distribute messages among competing consumers. Kafka fits **event streams**, audit, and multiple read models; SQS/Rabbit fit **task dispatch** and simpler ops when replay is not required. Wrong choice shows up as either missing history for new consumers or over-operating Kafka for 50 msgs/min.

---

## 3. What is a partition, and what ordering guarantee does Kafka provide?

**Answer:** A **partition** is an ordered, replicated log shard within a topic. Kafka guarantees **strict order within one partition only**—not across partitions or the whole topic. **Partition key** (e.g., `orderId`) maps related events to the same partition so `Placed` always precedes `Paid` for that order. No key → round-robin → **no per-entity order**.

---

## 4. What is a consumer group, and how does it relate to parallelism?

**Answer:** A **consumer group** is one logical application instance fleet sharing work: **each partition is assigned to at most one consumer in the group** at a time. Parallelism ceiling = **partition count**. A second group reading the same topic has **independent offsets**—analytics and fulfillment both consume `order-events` without stealing messages from each other.

---

## 5. Explain at-least-once delivery and why it is the common default mental model.

**Answer:** **At-least-once:** process the message, then **commit offset**; on crash before commit, another consumer **replays** the message → **duplicate**. Kafka’s default consumer and most production pipelines assume this unless you invest in idempotent producers, transactions, and still idempotent side effects. **At-most-once** (commit before process) loses messages on crash. **Exactly-once end-to-end** to email or DB almost always requires **application dedup**, not broker settings alone.

---

## 6. What is consumer lag, and why is per-partition lag more useful than a single global number?

**Answer:** **Lag** is how far a consumer group’s committed offset trails the log end—**backlog in messages or time**. Global lag hides **hot partitions**: one key skew can leave partition 7 at 2M messages while others are current. Alert on **max lag per partition** and tie to business SLO (“ship within 5 minutes of order”). Sudden lag spikes often mean slow DB, poison message retry loops, or rebalance storms.

---

## 7. What triggers a consumer group rebalance, and what symptom appears in production?

**Answer:** Consumer **join/leave**, **session timeout** (slow processing exceeding `max.poll.interval.ms`), or partition count change triggers **partition reassignment**. Symptoms: **lag spike** during deploys, duplicate processing if offsets committed mid-batch incorrectly, and **stop-the-world** pauses with older eager rebalance strategies. Mitigate: cooperative rebalance, static membership, smaller batches, idempotent handlers, and process-then-commit discipline.

---

## Application

## 8. Checkout must publish OrderPlaced after the DB commit. How do you avoid dual-write bugs?

**Answer:** Use the **transactional outbox**: in the same DB transaction, insert the order row and an **outbox** row; a separate **relay** (or CDC like Debezium) publishes to Kafka and marks outbox sent. Never “write DB then fire-and-forget produce” without outbox—crash between steps loses events or requires scary reconciliation. Consumer side still needs **idempotency** because the bus is at-least-once.

---

## 9. You need per-order event ordering but 80k orders/minute peak. How do you partition?

**Answer:** Key by **`orderId`** so all lifecycle events for one order share a partition. Scale **partition count** and consumers until per-partition throughput fits processing budget (e.g., 500–2k msgs/s per partition depends on payload and DB). **80k/min ≈ 1.3k/s** aggregate may need tens of partitions with headroom. Watch **hot keys** (bulk corporate orders)—salting breaks order unless you redesign invariants.

---

## 10. A consumer sends email on every message and users report duplicates after a deploy. What failed?

**Answer:** Classic **at-least-once + non-idempotent side effect**: rebalance or crash replayed messages without dedup. Fix: **idempotency key** (`eventId`) in a durable store (unique index), commit offset **after** successful send (or use outbox on consumer), and alert on resend rate. Do not “fix” by switching to at-most-once without accepting **lost** notifications.

---

## 11. When should a failed message go to a DLQ instead of retrying forever on the main topic?

**Answer:** After **bounded retries** for transient errors (network, 503), route **deterministic failures** (schema mismatch, unknown SKU, validation) to a **DLQ topic** so main consumer **commits and advances lag**. DLQ depth must be **alerted** and replayed via tooling with audit. Infinite retry on poison messages blocks partition progress and masks code bugs as “lag.”

---

## 12. How do you design replay from a DLQ after fixing a bug?

**Answer:** Replay tool reads DLQ, optionally transforms payload, publishes to main topic or processes directly with **same idempotency keys**. Dry-run count, rate limit, and **monitor duplicate side effects**. Require **change ticket** linking bug fix version. Never replay 3M messages at full produce rate without backpressure—throttle and watch consumer lag and downstream DB.

---

## 13. What does `enable.auto.commit=true` imply for correctness?

**Answer:** The client may commit offsets on a **timer**, not strictly after your handler finishes—risk of **lost messages** (commit before process) or **duplicates** depending on timing, and surprises on rebalance. Production services usually **manual commit** after durable side effects (or use libraries with clear semantics). If you auto-commit, handlers must still be **idempotent** and you must understand commit interval vs processing time.

---

## 14. A new analytics team wants a copy of all order events from day one. What Kafka feature helps?

**Answer:** **Retention** (time/size) on the topic plus a **new consumer group** starting at `earliest` offset (or restore from object storage if retention expired). For very long history, consider **tiered storage** or export to the data lake. Contract: topic retention must exceed **onboarding window** or they only get partial history—document in topic SLA.

---

## Design & Trade-offs

## 15. Kafka vs Amazon SQS for a notification worker pool doing 2k jobs/s with no replay requirement.

**Answer:** **SQS** wins on **ops simplicity**, per-message visibility timeout, and AWS integration at moderate scale—no broker cluster to run. **Kafka** wins if multiple subscribers need the **same stream**, replay, or very high sustained throughput with log retention. At **2k/s** either can work; choose on **fan-out, replay, team skills, and cost model** (SQS per-request vs Kafka cluster + MSK). Missing replay need → avoid Kafka tax.

---

## 16. Can you get global total ordering of all events on a topic?

**Answer:** Only with **one partition**—which caps throughput to roughly **one consumer’s** processing rate and one broker leader’s write path. Global order is usually wrong requirement; prefer **per-aggregate order** (per order, per account) via keys. If you truly need global sequence, question whether you need a **database sequence** or single-writer service instead.

---

## 17. What is log compaction, and when would you enable it?

**Answer:** **Compaction** retains the **latest record per key**, tombstoning older versions—changelog semantics for **KTables** / `user-settings` keyed by `userId`. Not for immutable event histories like `OrderPlaced` streams where every event matters. Wrong use loses audit trail. Ops: compaction lag and disk still matter.

---

## 18. How do idempotent producers and Kafka transactions help, and what do they not fix?

**Answer:** **Idempotent producer** (PID + sequence) avoids duplicate **writes to the log** on retry. **Transactions** allow consume-transform-produce atomically within Kafka. They do **not** make **email sent once** or **DB updated once** without consumer idempotency or external transaction patterns. EOS Kafka + non-idempotent `charge()` still double-charges.

---

## 19. You plan to increase partitions from 24 to 96 on a keyed topic. What risks do you call out in review?

**Answer:** **New keys** only land on new partitions—existing data does not reshuffle; **ordering per key is preserved** for new produces. Consumers rebalance; lag spikes. Downstream jobs assuming `partition == shard` break. Cannot decrease partitions easily. If changing keying strategy, treat as **new topic + migration**. Load-test rebalance and verify **consumer instance count** justifies 96.

---

## 20. How do Avro/Schema Registry and compatibility modes prevent production outages?

**Answer:** Schemas registered with **BACKWARD** (new consumers read old data) or **FORWARD** rules block deploys that remove required fields or change types incompatibly. Consumers deserialize with **schema id** in wire format. Failure mode: producer deploys new schema, old consumers **crash loop**—CI contract tests and dual-schema consumption window required. Breaking change → **new topic** `order-events-v2`.

---

## Stretch

## 21. Consumer lag is fine globally but partition 3 is at 4 hours. What do you check?

**Answer:** **Hot partition** from skewed key, slow messages on that shard, or stuck consumer not assigned evenly. Metrics: `records-lag` per partition, produce rate per partition, consumer **assignment map**. Compare message keys on P3 vs others. Remedies: salt redesign (careful with ordering), scale processing for that handler path, fix poison message on P3 only, add partitions only with correct keying strategy for **new** traffic.

---

## 22. During a flash sale, produce rate doubles and fulfillment lag exceeds SLO. Name three levers.

**Answer:** (1) **Scale consumers** up to partition count and optimize handler (batch DB writes, async steps). (2) **Add partitions** only with key plan for new capacity—not retroactive fix for skew. (3) **Degrade** non-critical consumers or shed load (pause analytics group). (4) **Cache/idempotent prefetch** inventory. Measure: end-to-end time from `OrderPlaced` timestamp to shipment created—not just broker CPU.

---

## 23. How would you explain the outbox pattern to a skeptical PM worried about “more moving parts”?

**Answer:** Without outbox, either we **lose events** when the server crashes after charging the card, or we **duplicate** warehouse work—both hit revenue and support costs. Outbox is one extra table written in the **same transaction** we already trust for payments; relay is boring infrastructure. Alternative is nightly reconciliation jobs that **anger customers** before we find gaps. Moving parts are visible; dual-write bugs are silent until Black Friday.

---

## 24. A team wants to use Kafka as a request-response bus with 50 ms SLA. What do you push back on?

**Answer:** Kafka is optimized for **durable streaming**, not RPC: polling latency, broker round-trips, and lack of per-request reply semantics without **correlation id + reply topic** pattern (complex, still not true RPC). For 50 ms **read-your-writes**, use **HTTP/gRPC** or queue with request-reply (RabbitMQ) if async. If they insist, quantify p99 with **correlation topics**, DLQ, and ops cost—usually a design smell for sync user journeys.

---

## 25. What belongs in a topic contract ADR for `order-events`?

**Answer:** **Owners**, schema (Avro/JSON), **partition key**, retention (e.g., 14 days), max payload size, **PII classification**, allowed consumers, **delivery semantics** (at-least-once), **idempotency key field**, DLQ topic name, max acceptable **lag SLO**, compatibility mode, and **replay procedure**. Include rejected alternatives (e.g., SNS-only) and migration plan for v2. Sign-off from security if cardholder data could leak in headers.
