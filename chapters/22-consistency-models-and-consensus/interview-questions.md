# Interview Questions: Consistency Models and Consensus

**Bank size:** 10  
**Rationale:** Focused consistency/consensus lens; rubric default 10 for decision-oriented subtopics before full chapter expansion.  
**Last updated:** 2026-05-20

---

## Core

## 1. What is linearizability, and when is it worth paying for on a read path?

**Answer:** **Linearizability:** every operation appears to take effect **atomically** at some point between its start and end; all clients agree on a single global order—reads never return “before” a completed write. Worth it for **coordination primitives**: leader election, distributed locks, inventory debit that must not oversell, fraud dedup counters. Often **not** worth it for **product catalog**, social feeds, or analytics—eventual or bounded staleness suffices. Cost: quorum RTTs, leader bottleneck; cross-region linearizable reads can add **20–80 ms** per hop.

---

## 2. How is serializability different from linearizability?

**Answer:** **Serializability** (transactions): concurrent transactions behave as some **serial order**—classic ACID isolation. **Linearizability** (replicated registers): per-object real-time order visible to all clients. A single-node Postgres `SERIALIZABLE` is not automatically linearizable across **async replicas**. In interviews: “DB transaction isolation” vs “replicated service read consistency.” You can have serializable writes on a leader and **stale** reads from a follower.

---

## 3. Define read-your-writes, monotonic reads, and eventual consistency in one paragraph each.

**Answer:** **Read-your-writes:** after a user writes, their subsequent reads see that write (session stickiness or version check)—critical after profile update. **Monotonic reads:** a client never sees **older** data after seeing newer (no time travel)—often via sticky routing to same replica or monotonic token. **Eventual consistency:** if writes stop, replicas **converge**; no bound on staleness during churn—fine for search indexes, risky for balance without version checks. Document which guarantees your **API contract** exposes.

---

## 4. Explain quorum reads and writes (R, W, N) and the “R + W > N” rule of thumb.

**Answer:** **N** replicas; **W** acks for write; **R** acks for read. If **R + W > N**, read and write quorums **overlap**, so a read likely sees latest write (tunable staleness). Example: N=3, W=2, R=2—tolerates one failure, 2 RTTs. Lower R (e.g., R=1) cuts latency but risks **stale** reads—PACELC latency trade ([Chapter 21](../21-cap-theorem-and-pacelc/README.md)). Hot keys at W=3 become **write bottlenecks**.

---

## 5. At architect depth: what problem does Raft solve, and what do you *not* need to implement yourself?

**Answer:** **Raft** elects a **leader**, replicates a **ordered log** to followers, and commits entries after majority ack—provides **strong consistency** for metadata: config, locks, shard mapping, feature flags. You do **not** rebuild Raft for application order data at 50k QPS—use **Postgres, Dynamo, Kafka**, or managed consensus. Know: leader failure → **election timeout** (~hundreds of ms unavailability), **only leader serves writes**, followers can lag. Ops: monitor **commit index lag**, avoid large log entries blocking pipeline.

---

## Stretch

## 6. When would you choose an external consensus service (etcd) vs database leader election vs “no consensus”?

**Answer:** **etcd/consul:** small, strongly consistent **metadata**, frequent membership changes, coordination. **DB leader election** (advisory lock): acceptable when election rate is low and team owns DB ops. **No consensus:** stateless workers, idempotent consumers, CRDT/event sourcing with defined merge—scale-out without single leader. Red flag: every microservice embedding Raft for business entities—operational nightmare.

---

## 7. A user sees “insufficient funds” then a successful debit for the same transfer. Which consistency guarantee broke?

**Answer:** Likely **read-your-writes** or **monotonic reads** failure: read hit a **stale replica** or load balancer switched to lagging follower after write on leader. Debug: compare **read replica lag**, routing policy, and whether API returns **version/etag**. Fix: route session to leader or primary, **sync read after write** for financial reads, or return `409` with current balance. Not always “CAP”—often **routing** misconfiguration.

---

## 8. Design: global counter for “tickets remaining” at 20k purchases/sec. Strong consistency vs sharded counters vs reservation queue?

**Answer:** **Single linearizable counter:** correct but **hot key**—one leader, ~low thousands updates/sec practical ceiling. **Sharded counters:** partition by show ID or bucket; sum on read—eventual total unless careful. **Reservation queue:** async **hold** with TTL, confirm from inventory service—matches checkout patterns ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)). Production flash sale: **pre-allocated shards** + local decrement + periodic reconcile; expose “approx remaining” if needed.

---

## 9. What is a fencing token, and how does it prevent stale leaders from corrupting storage?

**Answer:** Monotonic **token** issued by consensus layer; storage rejects writes with **token < highest seen**. Scenario: old leader, delayed by GC, writes after new leader elected—without fencing, double allocation. Pair locks in etcd/ZooKeeper with **versioned keys** or DB **compare-and-set**. Incident without fencing: two schedulers assign same job; duplicate charges.

---

## 10. Your team proposes “we’ll use gossip for inventory consistency.” What questions do you ask?

**Answer:** (1) **Convergence bound** under continuous writes—gossip is **eventual**; is oversell acceptable for how long? (2) **Conflict resolution** when two nodes decrement same SKU—vector clocks, CRDT, or authoritative shard owner? (3) **Failure modes:** partition → divergent counts; how **reconcile** with warehouse truth? (4) **Read path:** does checkout call local gossip state or central quorum? Prefer **owned shard per SKU range** + events over pure epidemic gossip for money-adjacent inventory.
