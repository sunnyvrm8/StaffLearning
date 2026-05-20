# Interview Questions: Scalability and Capacity Planning

**Bank size:** 10  
**Rationale:** Capacity planning pairs with many prior chapters; ten questions stress shard keys, hot paths, and 10x levers without a full 50-bank napkin-math course.  
**Last updated:** 2026-05-20

---

## Core

## 1. When is **horizontal scaling** the wrong default answer for a bottleneck?

**Answer:** When the system is **stateful with strong consistency** on a **single hot shard**, or **single leader** (e.g., one Postgres primary)—adding app servers **amplifies** writes to the same place and **increases** lock contention. Also wrong when **cost per unit** explodes (license per node) or **operational** complexity exceeds benefit for **<10x** headroom. Fix the **data path** first: **partition**, **async**, **cache**, or **serialize** hot updates through a **dedicated** queue.

---

## 2. How do you choose a **partition key** for sharded orders—one strong approach and one failure mode?

**Answer:** Strong: **shard by `customer_id`** for OLTP workloads where queries are “my orders”—colocates related rows, avoids scatter-gather on every page load. Failure mode: **power seller** with **1M orders/day** becomes **hot shard**—p99 for that tenant dominates. Mitigations: **sub-sharding** within tenant, **write-through** queue for ingestion, or **separate** high-volume tier with **dedicated** resources. Always model **skew**: top **1%** tenants often drive **50%+** writes.

---

## 3. Explain **hot key** problems in a distributed cache cluster serving a flash sale SKU.

**Answer:** All reads/writes for **one key** land on **one partition** owner—CPU/network saturate, **latency spikes**, **timeouts** cascade. **Counters** (`INCR`) on one key are especially bad. Mitigations: **application-side sharding** of logical key into `sku:000..sku:999` with **random read** or **merge** on client, **local** caching with **TTL**, **pre-warm**, **request coalescing** (single-flight), **CDN** for read-mostly. Trade-off: split counters complicate **exact** inventory—often pair with **authoritative DB** reservation ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/interview-questions.md)).

---

## 4. How do **read replicas** scale traffic—and what consistency caveats do you document for product?

**Answer:** Replicas scale **read QPS** offloading primary—good for dashboards, search suggestions. Caveats: **replication lag** (100 ms–minutes on overload) means **stale reads**—users may not see their **just-written** row unless **read-after-write** routes to primary or **session stickiness** with **causal** routing. For **financial balances**, default reads to **primary** or use **version** in API responses. Measure **lag** as an SLI with alert on **p95 lag >** product threshold.

---

## 5. Walk through **order-of-magnitude** capacity for a notification fan-out: 10M users, 1 campaign/day, average 100 devices each—what breaks first?

**Answer:** Naive fan-out: **10M × 100 = 1B** device messages if push to all—**FCM/APNs quotas** and **internal** queue depth break first, not your CPU. Realistic: **segmented** sends + **rate limits** + **batch topics** in the messaging tier ([Chapter 19](../19-kafka-and-messaging/README.md)). Storage: assuming **1 KB** dedup state per active device = **10 GB** order— manageable; **egress** to providers dominates cost. Architect takeaway: **fan-out on write** vs **read** path choice drives **10x** cost.

---

## Stretch

## 6. A relational DB hits **80% CPU** at peak; adding **read replicas** did not reduce primary CPU enough. Why?

**Answer:** Primary still serves **all writes** and possibly **read-your-writes** traffic; replicas only help **read-only** queries routed away. If workload is **write-heavy** or **ORMs** still hit primary for **locking** queries, replicas won’t help. Also **replication** itself consumes primary **IO**. Next steps: **query tuning**, **partitioning**, **archive cold rows**, **async** denormalized projections ([Chapter 18](../18-event-driven-architecture/README.md)).

---

## 7. **Multi-tenant SaaS**: one tenant runs a report that saturates shared workers. What architectural patterns prevent **noisy neighbor**?

**Answer:** **Hard isolation**: dedicated **queue/pool** for enterprise tier; **fair scheduling** (token bucket per tenant); **query cost limits** and **statement timeouts**; **resource quotas** in K8s per namespace. **Soft isolation**: **priority classes** with **preemption** risks support backlash. Measure **per-tenant** p99 and **chargeback** signals. Failure: “**unlimited**” SQL in **shared** warehouse—one tenant scans **PB** logical bytes.

---

## 8. Tie **cache stampede** to capacity: what happens when TTLs align at midnight—and the fix?

**Answer:** Many instances **miss** cache simultaneously, **thundering herd** to DB/origin—**QPS spike 10–50x**, DB **connection exhaustion**. Fixes: **probabilistic early expiration**, **single-flight** lock per key, **stale-while-revalidate**, **jittered TTLs**, **prefetch** jobs. Cross-link: [Chapter 12](../12-caching-strategies/interview-questions.md). This is a **capacity incident** disguised as a cache bug.

---

## 9. Name three **10x growth levers** you’d list in an ADR for a growing e-commerce checkout.

**Answer:** (1) **Shard** order writes by region or customer segment. (2) **Async** payment capture with **idempotent** webhooks. (3) **CDN + edge** for static and **read models**. Alternatives rejected: **global 2PC** (latency), **bigger boxes only** (ceiling). Each lever names **metric** to watch (write QPS per shard, **lag**, **error budget**).

---

## 10. **Global ID** generation at **100k IDs/sec** across regions—compare approaches and failure modes.

**Answer:** **UUID v4**: trivial, no coordination; **index bloat** and **non-time-ordered** in B-trees (insert hotspots mitigated by **ULID**). **DB sequence**: **single point** and **region coupling**. **Snowflake-style**: **clock skew** causes duplicates or gaps—need **NTP discipline** and **epoch** handling. **Lease ranges** per region: operational complexity, rare **exhaustion** if mis-sized. Pick based on **sortability**, **collision** tolerance, and **join** performance in analytics.

---
