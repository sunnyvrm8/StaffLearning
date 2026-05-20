# Interview Questions: Concurrency and Multithreading

**Top 10** with answers — prerequisite lens for caching, messaging, and distributed timelines.  
**Last updated:** 2026-05-20

---

## Core

## 1. When does adding threads to a payment service actually reduce latency—and when does it make things worse?

**Answer:** Threads help when work is **parallelizable and mostly waiting** (I/O to PSP, fraud API, ledger DB) and you have **CPU headroom**—e.g., fan-out auth + risk checks on independent calls can cut p99 if you bound concurrency. They hurt when the bottleneck is **contention** (one hot row, one lock, one connection pool), **coordination cost** exceeds savings, or you **oversubscribe** CPUs and thrash context switches. Rule of thumb: measure **queue depth and lock wait** before scaling thread count; for CPU-bound fee calculation, more threads often **raise** p99 without raising throughput.

---

## 2. What is a data race, and how would you prove one exists in production vs in CI?

**Answer:** A **data race** is two threads accessing the same mutable state with at least one write, without a **happens-before** relationship—reads can see stale or torn values. In CI: **Java** stress tests + `jcstress` or heavy parallel tests; **Go** `go test -race`. In production: symptoms are **nondeterministic** (wrong balances, duplicate idempotency keys “sometimes”), often under load. Evidence: **metrics** (retry spikes), **thread dumps** showing many threads blocked on the same monitor, audit logs showing **impossible orderings**. Fix with proper synchronization or confinement—not “add `volatile` everywhere.”

---

## 3. Compare `synchronized` / `ReentrantLock`, `ReadWriteLock`, and lock-free structures for an in-process metrics counter.

**Answer:** **`synchronized` / `ReentrantLock`:** simplest for **low-contention** updates; fair locks cost more. **`ReadWriteLock`:** many readers, rare writers (config snapshots, feature flags)—writers still block everyone; easy to misuse if writes are frequent. **Lock-free / atomics (`AtomicLong`, `sync/atomic`):** best for **hot counters** where lock overhead dominates; you trade **complex invariants** (multi-field updates need careful design or still need a lock). For **histograms or maps**, prefer **per-shard counters** or a **single-writer** goroutine/thread consuming events—one giant locked map becomes the system bottleneck at ~10k+ updates/sec per instance.

---

## 4. Explain happens-before in terms a checkout engineer can use—not the JLS appendix.

**Answer:** **Happens-before** is the guarantee that **effects of action A are visible** to thread B before B reads shared state. You get it from: releasing/acquiring a lock, `volatile`/atomic writes, starting a thread (`start`), completing a `Future`, message handoff on a **queue/channel**, or `CountDownLatch`. Without it, another thread may see a **partially constructed** `Order` or a flag flipped before the payload write. In design reviews, ask: “**Which edge** publishes this state to other threads?”—not “is it thread-safe?” in the abstract.

---

## 5. How do you size a thread pool (or worker count) for an order service that is 80% I/O?

**Answer:** For **I/O-bound** work, pool size can exceed core count: often **tens to low hundreds** per machine if each task mostly waits on HTTP/DB—bounded by **downstream limits** (DB max connections, PSP rate limits), not `Runtime.getRuntime().availableProcessors()`. For **CPU-bound** work, stay near **cores** (or cores + small buffer) to avoid context-switch tax. Always cap with **backpressure** (bounded queue + reject/shed load) so a slow dependency does not create **unbounded runnable queues** and GC death. Measure: **pool active count, queue length, rejection rate**, and **downstream error rate** when you scale threads.

---

## 6. Design a per-user rate limiter inside one API instance: shared map, many goroutines/threads. What breaks first at 50k RPS?

**Answer:** A **single `sync.Mutex` / `synchronized` map** becomes the hotspot; **GC** pressure from boxed entries and churn hurts Go/Java alike. Mitigations: **shard** by `hash(userId) % N` with per-shard locks; **striped** locks; or **atomic token buckets per shard** with occasional reconciliation. At **multi-instance** scale, in-process limits are **wrong per user** (each node grants full quota)—move to **Redis/sidecar** with sliding window (Case Study 5: Rate Limiter). First failure mode to name in interviews: **correct on one box, 5× allowance behind a load balancer**.

---

## 7. Your service deadlocks under load—four conditions and a production debugging path.

**Answer:** **Coffman conditions:** mutual exclusion, hold-and-wait, no preemption, circular wait. Break one in design: **lock ordering** (always acquire `Account` then `Ledger`), **try-lock with timeout**, **smaller critical sections**, avoid calling **foreign code** while holding locks. In prod: capture **thread dumps** (`jcmd`, `kill -3`) during the incident; look for **cycles** in “waiting to lock.” Long-term: **lock profiling**, static analysis, and code review rules for “lock while calling HTTP.” Deadlocks are **P1** when checkout stops clearing—treat as design defect, not JVM quirk.

---

## Stretch

## 8. Why do message consumers often process **one partition at a time** but still run many consumer threads overall?

**Answer:** **Ordering** is per **partition key** (e.g., `orderId`): parallelizing inside one partition risks **reordered** `PaymentCaptured` before `OrderPlaced`. Across partitions, order is irrelevant—so **consumer concurrency = partition assignment**, not “threads per message.” More threads than partitions **idle or fight** for the same assignment model. Tie to concurrency: you get **parallelism without shared mutable order state** by partitioning; threads coordinate via **broker offsets**, not in-memory locks on business objects.

---

## 9. A hot cache key expires and 500 threads miss together—connect concurrency to cache stampede.

**Answer:** **Cache stampede:** many concurrent misses **recompute** the same expensive value (DB, pricing engine). Concurrency makes it worse because **thundering herd** saturates DB and blows p99. Fixes: **singleflight** / per-key **mutex** so one recomputes and others wait; **probabilistic early expiration**; **request coalescing**; **prefetch** on write. This is why [Chapter 12: Caching Strategies](../12-caching-strategies/README.md) assumes you understand **in-process coordination** before reaching for “more cache nodes.”

---

## 10. Two payment events show `T1` before `T2` in logs but the ledger applies them reversed—what concurrency lesson precedes distributed clocks?

**Answer:** **Wall-clock timestamps are not a total order** across machines: skew, NTP steps, and **async pipelines** mean “logged first” ≠ “causally first.” In one JVM you fix visibility with **happens-before**; across services you need **causal ordering** (vector clocks in theory; in practice **version numbers**, **Kafka offsets**, **DB transaction ids**, or **Lamport/logical clocks** on events). Interview punchline: multithreading teaches **visibility and ordering** locally; [distributed timelines](../20-distributed-systems-fundamentals/README.md) teach that **time ≠ order** globally—design idempotent, order-tolerant consumers and **monotonic versions** per aggregate.
