# Interview Questions: Caching Strategies

**Bank size:** 10  
**Rationale:** Applied data-path chapter (local/distributed cache, invalidation); rubric 10 for focused drill banks.  
**Last updated:** 2026-05-20

---

## Core

## 1. Describe cache-aside (lazy loading). Who writes the cache and when?

**Answer:** Application **reads** cache first; on miss, loads from DB, **populates** cache, returns. Writes typically **update DB then invalidate or update cache**—the app owns consistency logic. Pain solved: shield hot reads (product catalog, session profile) from DB at **10k–100k RPS** with sub-ms cache RTT vs 1–5 ms DB. Failure mode: stale data after write if invalidation is forgotten—document TTL as **safety net**, not primary consistency.

---

## 2. Compare write-through vs write-behind (write-back) for an inventory counter.

**Answer:** **Write-through:** every write hits cache and DB synchronously—stronger consistency, higher write latency (~2× RTT). **Write-behind:** write to cache, async flush to DB—lower latency, risk **loss on crash** and ordering bugs. Inventory during flash sales: prefer **write-through or DB-authoritative** with cache invalidation; write-behind only if you accept reconciliation and have durable queue. Numbers: 1 ms cache + 3 ms DB vs batching 100 updates—measure oversell rate, not average latency alone.

---

## 3. What is a cache stampede and how do you mitigate it on a hot key?

**Answer:** Many clients miss the same key simultaneously and **all hit the DB**—spike from 1k to 50k QPS on one query. Mitigations: **single-flight** (one loader, others wait), **probabilistic early expiration** (jitter refresh before TTL), **request coalescing** at the edge, or **pre-warm** on deploy. Production signal: DB CPU correlates with cache TTL expiry pattern. For celebrity product keys, combine with **local L1** + short TTL L2 ([Chapter 28](../28-scalability-and-capacity-planning/README.md) hot keys).

---

## 4. When is a local (in-process) cache appropriate vs a distributed cache (Redis)?

**Answer:** **Local:** ultra-hot read-only or versioned config, microsecond access, tolerate **per-node staleness** (each pod has its own view). **Distributed:** shared state across pods (rate limits, feature flags, session not sticky), explicit memory budget, eviction policies. Avoid local cache for **user-specific writes** unless you invalidate on every change or use short TTL (~seconds). Trade-off: local reduces network but breaks **uniform invalidation**—use event bus or version header.

---

## 5. How do TTL and explicit invalidation work together in production?

**Answer:** **Invalidation** on write paths keeps correctness for known keys; **TTL** bounds staleness when invalidation misses (bug, async pipeline lag). Choose TTL from **business tolerance**: catalog 5–60 min with version in API; auth/session seconds–minutes. Too-short TTL → stampede; too-long → support tickets. Measure **cache age at read** and **staleness incidents**; alert on invalidation failure rate.

---

## 6. You cache `GET /product/{id}` in Redis. A price update must be visible within 2 seconds globally. What pattern?

**Answer:** On price update: **write DB**, then **delete** cache key (or set with new version), optionally **pub/sub** to other regions for local L1 purge. Reads use **cache-aside** with 2 s max staleness → TTL ≤ 2 s *and* invalidation on write. For multi-region, add **version field** in response so clients detect stale; consider **read-your-writes** via routing to primary or short-lived “don’t cache” header after update. Do not rely on “eventual” without a number.

---

## Stretch

## 7. Cache hit ratio dropped from 95% to 60% after a marketing push. What do you check?

**Answer:** (1) **Key cardinality explosion**—new SKUs not in warm set. (2) **TTL too aggressive** or memory eviction (`maxmemory` policy `allkeys-lru`). (3) **Shard hot spot**—one Redis node saturated. (4) **Serialization size**—values bloated, network bound. (5) **Bypass header** or auth change causing misses. Fix: pre-warm top N keys, increase memory, **consistent hashing** cluster, or separate cache for campaign traffic. Cross-link indexing if miss storm hits DB ([Chapter 11](../11-indexing-and-query-optimization/interview-questions.md)).

---

## 8. Is Redis a source of truth for shopping cart data?

**Answer:** Usually **no** for durable cart/checkout—Redis is fast but **lossy** under failover unless AOF/RDB and replication tuned; memory cost scales with carts. Pattern: Redis as **ephemeral session** with periodic flush or **DB + cache-aside**; use Redis persistence only when business accepts RPO. Payment and inventory truth stay in **OLTP DB** with idempotent APIs ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)).

---

## 9. Design a two-tier cache (Caffeine + Redis) for a search autocomplete API at ~20k QPS.

**Answer:** **L1** per pod: 10–50k entries, 1–5 s TTL, bounds memory. **L2** Redis: shared prefix keys, 30–120 s TTL, **single-flight** on miss to search backend (~50–200 ms). **Invalidation:** rare for suggestions; version prefix on index rebuild. **Failure:** degrade to L1-only or origin with circuit breaker—never infinite Redis retry. p99 target: L1 hit <1 ms, L2 <2 ms, origin budget capped. Document **thundering herd** test in load suite ([Chapter 27](../27-performance-engineering/README.md)).

---

## 10. How does caching interact with consistency when you add read replicas?

**Answer:** Cache may hold data **newer** than a lagging replica (write invalidated cache, read repopulates from stale replica) or **older** if invalidation lags. Mitigations: **read-your-writes** from primary after mutation, **version stamps** in cache value, or **sticky routing** to primary for session. PACELC “else” trade-off: faster cache hit vs linearizable read ([Chapter 21](../21-cap-theorem-and-pacelc/interview-questions.md)). Incident: “user paid but UI shows unpaid”—trace cache key, replica lag, invalidation order.
