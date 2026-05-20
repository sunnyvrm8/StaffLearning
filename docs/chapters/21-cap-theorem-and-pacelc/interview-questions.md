# Interview Questions: CAP Theorem and PACELC

**Bank size:** 10  
**Rationale:** Narrow decision-lens chapter (CAP/PACELC); rubric default for focused subtopics.  
**Last updated:** 2026-05-20

---

## Core

## 1. State the CAP theorem in terms a production team can act on—not as a triangle poster.

**Answer:** During a **network partition**, a replicated system cannot simultaneously guarantee **linearizable consistency** (every read sees the latest agreed write) and **full availability** (every request to a non-failed node gets a successful response). In practice you choose **how to behave under partition**: stop writes to avoid divergence (CP), or accept reads/writes that may be stale or conflicted (AP), then repair. CAP is about **partition**, not “pick two of three forever”—when the network is healthy, you can have both consistency and availability for that window.

---

## 2. What is PACELC, and why do architects prefer it over CAP alone in design reviews?

**Answer:** **PACELC** extends CAP: **if Partition** then trade Availability vs Consistency; **Else** (normal operation) trade **Latency** vs Consistency. Most user-visible pain is **Else**: strong quorum reads on every request add 1–3 RTTs (~1–5 ms LAN, 20–80 ms cross-region). Example: Dynamo-style AP under partition, but you still choose whether the happy path is **R=1** (fast, stale) or **quorum** (slower, fresher). Interview strength: name **latency budget** before naming “we’re AP.”

---

## 3. Classify these as tending CP or AP under partition: etcd/ZooKeeper, Cassandra, single-region Postgres with synchronous replica, multi-master CRDT chat.

**Answer:** **etcd/ZooKeeper:** CP—quorum required; minority partition loses writes (unavailable rather than diverge). **Cassandra (default tunable):** AP—often still serves with `ONE`/`LOCAL_ONE`; conflicts resolved later (LWW, timestamps—risky). **Sync Postgres replica:** CP for writes on primary; async replica is **not** linearizable for reads routed there. **CRDT chat:** AP—availability and merge semantics over strict global order. Always add **“under what client consistency level?”**—Cassandra can be stricter if you pay latency.

---

## 4. When is “CA” (consistent and available) a valid claim?

**Answer:** **CA** is realistic on a **single node** or **single failure domain** where you do not face **network partitions between replicas**—e.g., one Postgres primary, or a monolith with one DB. The moment you have **multi-region or multi-AZ async replication** with split-brain risk, you are in CAP/PACELC territory. Marketing “CA database” usually means “consistent + available **until** we partition”—document the boundary.

---

## 5. A catalog service must stay readable during an AZ network split. Product accepts stale prices for 30 minutes. What CAP/PACELC choice do you document?

**Answer:** **Partition:** favor **availability** on reads—serve from local replicas or cached projections; queue or block **price writes** that cannot reach quorum if financial risk is high. **Else (steady state):** accept **eventual consistency** with bounded staleness (TTL, version in API) to keep p99 read latency under ~50–100 ms vs cross-region quorum on every SKU. Measure **staleness age** and **conflict rate**; define rollback if capture price ≠ displayed price exceeds SLO.

---

## Stretch

## 6. Your payment ledger must never double-settle. During partition, should the ledger be CP or AP?

**Answer:** **CP on the write path:** no commit without quorum/leader acknowledgment; minority partition **rejects** writes rather than accepting divergent balances. Reads for “available balance” may use **carefully defined** staleness (read replica) if product and regulators allow, but **settlement writes** are CP. Pair with **idempotency keys** ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)) for retries, not AP writes. Incident pattern: AP ledger + LWW → lost withdrawal.

---

## 7. Compare “strong consistency everywhere” vs “eventual consistency with compensations” for an order–inventory system at 5k orders/sec.

**Answer:** **Strong everywhere** (2PC or single DB): simpler mental model, but **latency** and **lock contention** on hot SKUs; one partition can stall checkout globally. **Eventual + compensations:** higher throughput, independent services, but **saga/outbox**, idempotent consumers, and ops playbooks for oversell. At 5k/sec, flash sales often force **reservation + async confirm**; strong cross-service 2PC rarely scales without a **single write shard** for inventory. Numbers: 2PC across 3 regions can add 50–150 ms per checkout hop.

---

## 8. How would you explain to a PM why “just make it strongly consistent” increases checkout latency?

**Answer:** Strong consistency across regions means **waiting for enough replicas** to agree before responding—extra network round trips per write/read. If checkout calls inventory, pricing, and tax serially with quorum reads, **200 ms × 3** blows a 500 ms mobile SLA. Options: **parallel** calls, **local quorum** in one region, **read-your-writes** session stickiness, or **async** non-critical paths. Tie to **PACELC Else:** consistency costs latency even without partition.

---

## 9. During an incident, half of Redis cluster nodes are isolated. Clients still write to both sides. What CAP mistake likely happened?

**Answer:** The system behaved **AP without conflict handling**—split brain with **last-write-wins** or no fencing. Symptom: counters, locks, or session state diverge; heals into wrong totals. Fix: **quorum writes**, **fencing tokens**, or **stop minority** writes (CP). Runbook: detect **split-brain** via cluster health, fail over to single primary, reconcile from durable source of truth. Preview: consensus and leader election ([Chapter 22](../22-consistency-models-and-consensus/README.md)).

---

## 10. Design review: multi-region active-active product catalog. List two CAP/PACELC decisions you require in the ADR.

**Answer:** (1) **Under partition:** can EU and US both accept merchandising updates? If yes, what **conflict resolution** (version, domain owner, manual merge) and max divergence time? (2) **Else:** per-read **consistency level**—is `LOCAL_QUORUM` required for price, or is 60 s CDN/cache staleness OK? Add **metrics:** replication lag, conflict count, p99 read by region. Reject “active-active” without **write routing** or **CRDT/version** story—otherwise you inherit AP pain without documenting it.
