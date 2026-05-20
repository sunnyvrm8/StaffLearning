# Interview Questions: Performance Engineering

**Bank size:** 10  
**Rationale:** Performance is a wide discipline; ten questions focus on tail latency, measurement discipline, and capacity signals before scale-out debates.  
**Last updated:** 2026-05-20

---

## Core

## 1. Why does **p99 latency** matter more than average latency for a user-facing API—and when is p99 misleading?

**Answer:** Users hit **slow paths** stochastically; averages hide **1%** of requests at 2s while mean is 50 ms—those users churn or retry (amplifying load). **SLOs** are often written on **p95/p99**. p99 is misleading if traffic is **low volume** (noisy), or if you **clip** timeouts—p99 looks good while **timeouts** (logged as client errors) dominate user pain. Also misleading when **one huge customer** dominates—segment by **tenant** or **region**.

---

## 2. List the most common **tail latency** contributors in a JVM/Go microservice behind a load balancer.

**Answer:** **GC pauses** (JVM young/old gen pressure), **stop-the-world** events, **lock contention** on hot caches, **thread pool exhaustion** (queueing delays), **slow downstream** (DB, HTTP without deadlines), **TLS handshake** storms on cold connections, **CPU throttling** in containers (missing limits/requests), **disk** contention on shared nodes. Order-of-magnitude: one **missing connection pool** cap can add **100 ms+** queue wait at 5k RPS.

---

## 3. Describe a **load test** you trust for a release candidate—what you measure and what you explicitly do not claim.

**Answer:** **Workload model** based on production **shape** (read/write mix, payload sizes), **ramp** gradually, **soak** 30–60 min to catch leaks. Measure **latency percentiles**, **error rates**, **resource saturation** (CPU, GC, pool waits), and **business metrics** (checkout success). Do **not** claim “handles 10x” unless you model **10x** of the **correct** dimension (often **writes** or **fan-out**, not just RPS). Synthetic tests miss **cache warmth** and **skew**—complement with **shadow** traffic or **canary** with real mix.

---

## 4. How do you read a **flame graph** to decide whether to optimize code vs scale hardware?

**Answer:** Wide **plateaus** at your frames = CPU time in your logic—candidate for algorithmic fixes. Wide stacks in **runtime** (`malloc`, regex, JSON) suggest **allocation** or parsing cost—pool buffers, cheaper codec, streaming. If flame shows **idle/wait** in syscalls (`read`, `epoll_wait`) you may be **I/O bound**—more CPU won’t help; fix **queries**, **batching**, or **parallelism**. Mistake: optimizing a function that is **0.5%** of samples while **DB** is 70%.

---

## 5. What knobs matter most for **HTTP client connection pools** under bursty traffic?

**Answer:** **Max connections per route/host**, **max pending acquire timeout**, **idle eviction**, **keep-alive** alignment with LB idle timeouts. Too small → **queueing** and tail latency; too large → **file descriptor** pressure and **thundering herd** on downstream. Set **deadlines per request** so stuck connections release. In Go/Java, tune **transport** max idle and **TTL** to match **NLB** ~350s defaults—mismatch causes **RST** surprises.

---

## Stretch

## 6. After a deploy, **p99 doubles** for 10 minutes then recovers. Name three plausible causes and how you’d confirm.

**Answer:** (1) **Cold JVM** (JIT, class loading)—confirm with **JIT logs** and compare to **warmup** canary. (2) **Cache cold** after rollout—confirm **cache hit ratio** drop. (3) **Autoscaling lag**—fewer replicas during deploy—confirm **replica count** vs RPS. (4) **Schema migration** causing **plan** change—confirm **DB** slow query log. Use **deploy markers** on dashboards and **diff** traces pre/post.

---

## 7. Tell a **“we optimized the wrong thing”** story framed as metrics you should have checked first.

**Answer:** Team rewrote JSON marshaling saving **5% CPU**, while checkout p99 was dominated by **N+1 queries** adding **150 ms**—real win was **batch fetch**. Lesson: start with **latency breakdown** (tracing spans) and **DB time** share, not micro-benchmarks. **Cost of delay**: two sprints on serialization while **SLO burn** continued.

---

## 8. What **capacity signals** tell you to scale **before** an outage—not after?

**Answer:** **Sustained** high **CPU** (>70% for business hours) with **rising** latency, **growing queue depths**, **GC time** trending up, **connection pool** wait time, **error budget** burn on **latency** not errors. Leading indicators beat **CPU average** alone—watch **p99 CPU** throttling events in K8s. Order-of-magnitude: aim for **30–50%** headroom at peak for **surprise** traffic (marketing push).

---

## 9. Compare **vertical scaling** (bigger instance) vs **horizontal scaling** for a **single-threaded** CPU-bound worker.

**Answer:** Vertical scaling helps until you hit **single-core** ceiling—bigger box doesn’t split one **hot thread**. Horizontal adds **parallel workers** but needs **partitioned** work (shards, partitions) to avoid **duplicate** processing. Trade-off: horizontal needs **idempotent** consumers and **rebalance** story ([Chapter 28](../28-scalability-and-capacity-planning/interview-questions.md)). For **single hot key**, neither fixes logic—you need **batching** or **algorithm** change.

---

## 10. Design drill: search API at **500 RPS** steady, **5k RPS** bursts during news events; p99 must stay **<120 ms**. What performance architecture decisions do you document?

**Answer:** **Caching** layer with **TTL** and **stale-while-revalidate** for popular queries ([Chapter 12](../12-caching-strategies/interview-questions.md)); **read replicas** or **search index** separate from write path; **rate limit** per API key to protect **shared** index; **timeouts + bulkhead** so slow tenants cannot exhaust pools. Load test **5k** with **realistic** query skew (top 1% queries = 50% traffic). Define **degradation mode**: drop **non-essential** facets under pressure.

---
