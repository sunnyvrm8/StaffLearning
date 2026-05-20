# Interview Questions: Distributed Systems Fundamentals

**Bank size:** 25  
**Rationale:** Medium handbook chapter (clocks, partitions, retries, timeouts, backpressure) per interview-bank-rubric; user requested top 25.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. What changes when you move from a monolith to multiple processes on the critical path?

**Answer:** Failures become **partial and independent**: inventory can timeout while order CPU is idle; there is no shared transaction across the wire. Latency adds **serialization + network RTT** per hop (often 1–5 ms LAN, 20–80 ms cross-region). You must design **timeouts, idempotency, and observability per hop**—not one process-wide try/catch. Interview strength: name **partial failure** before naming Kafka or Kubernetes.

---

## 2. Why should you not use wall-clock timestamps to order events across two data centers?

**Answer:** Clocks **skew** (NTP steps, VM pause, leap smear)—node B can appear “earlier” than A while causally later. Wall time is fine for **TTL and human dashboards**, not for conflict resolution. Prefer **monotonic clocks** for local deadlines, and **versions, sequence numbers, or partition offsets** for cross-node ordering ([Chapter 22](../22-consistency-models-and-consensus/README.md)). Incident pattern: “last write wins” refunds beat captures after skew.

---

## 3. What is the difference between monotonic time and wall-clock time for service code?

**Answer:** **Monotonic** (`nanoTime`, `time.Since`) measures elapsed duration on **one machine**—immune to NTP jumps; use for **timeouts and SLAs**. **Wall clock** (`Instant`, `time.Now`) is calendar time—use for logging correlation and expiry, not cross-host ordering. Never compute “remaining budget” with wall clock across async boundaries without a propagated deadline.

---

## 4. Define a network partition in one paragraph. How is it different from “the service is down”?

**Answer:** A **partition** is a split in reachability: some nodes can talk, others cannot, or clients see only a subset of replicas. The system may still serve traffic **in both halves**—risking **divergent state** (split brain). “Service down” is total unavailability. Partition forces a **trade-off**: stop writes (quorum), serve stale reads, or queue—preview of CAP ([Chapter 21](../21-cap-theorem-and-pacelc/README.md)). Symptom: asymmetric errors—AZ-a users OK, AZ-b 503.

---

## 5. What is a “gray failure” and why do retries make it worse?

**Answer:** **Gray failure:** dependency returns success or intermittent errors but **latency degrades**—throughput collapses without a clean “down” alert. Retries **multiply load** on the already sick dependency, creating a **metastable** outage that persists after the root cause heals. Detect with **latency SLOs**, saturation metrics, and compare p50 vs p99 per callee—not only HTTP 5xx rate.

---

## 6. Explain at-least-once vs at-most-once delivery in plain language.

**Answer:** **At-most-once:** send once, no retry—may **lose** messages, never duplicate (UDP-style fire-and-forget). **At-least-once:** retry until ack—may **duplicate**, rarely lose. Most production systems are at-least-once on the wire; **exactly-once effect** on business state requires **idempotent handlers + dedup store** ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)). Saying “we use Kafka exactly-once” without describing the consumer idempotency is incomplete.

---

## 7. What does “fail fast” mean in a distributed call chain?

**Answer:** Stop spending **remaining deadline** on work the user will not wait for—return 503/504 with correlation id instead of blocking 30 s per hop. Propagate cancellation so downstream releases locks and threads. Fail fast **reduces** queue buildup and retry amplification; it does not mean “return error on first blip” without classifying retryable vs fatal.

---

## 8. What is backpressure?

**Answer:** A signal that **downstream is saturated** so upstream must **slow, queue with bounds, or reject**—instead of accepting unbounded work until OOM. Mechanisms: bounded semaphores, 429/503 with `Retry-After`, Kafka consumer pause, adaptive concurrency limits. Without it, flash traffic (~5–10× steady RPS) converts to **memory and thread exhaustion** in minutes.

---

## Application

## 9. A payment call times out after 400 ms. The client retries three times and the user is charged twice. What went wrong?

**Answer:** **Ambiguous timeout:** provider may have succeeded while the response was lost; retries were **not idempotent**. Fix: **Idempotency-Key** on every attempt, server-side dedup table (24–72 h TTL), reconcile with provider GET/status API before retrying POST. Disable blind retries on non-idempotent routes. Measure duplicate capture rate and idempotency cache hit ratio.

---

## 10. How do you set timeouts for a mobile checkout that must complete in 2 s p99?

**Answer:** Start from **user budget** (2 s), subtract edge (TLS, gateway ~200–400 ms), allocate **parallel** dependency budgets (e.g., 300 ms inventory + 300 ms pricing in parallel, not serial 600 ms). Each client uses **min(local cap, parent deadline − margin)**. Document a **budget table** in an ADR; test with chaos injecting 500 ms delay on one callee. If serial chain needs 1.2 s minimum, product or architecture must change—not “increase timeout to 10 s.”

---

## 11. When should an HTTP client retry a failed request?

**Answer:** Retry on **timeouts, connect failures, 503, 429** (respect `Retry-After`) when the operation is **idempotent** or keyed. **Do not** retry most **4xx** (except 409 conflict strategies), **non-idempotent POST** without keys, or when **deadline** is exhausted. Cap attempts (3–4), use **exponential backoff + full jitter**, and ensure gateway and service are not all retrying the same call ([code](./java/RetryWithJitter.java)).

---

## 12. What is exponential backoff with full jitter and why use it?

**Answer:** Backoff doubles sleep cap per attempt (e.g., 100 ms → 200 → 400, capped at 2 s); **full jitter** picks uniform random sleep in `[0, cap]` so retries **desynchronize** after a shared outage. Without jitter, thousands of clients retry at **t+1s** together—a **retry storm** that blocks recovery. Trade-off: longer tail for individual clients vs healthier system median time to recover.

---

## 13. Your checkout service accepts 10k RPS but payment provider caps at 800 RPS. What do you do?

**Answer:** **Backpressure at checkout**: bounded in-flight payment calls (e.g., 500), queue with max wait or reject 503/429 with `Retry-After`—do not unbounded-buffer 9.2k RPS in memory. **Shape traffic**: token bucket at edge, sale queue, or async “order accepted, pay async” if product allows. **Scale** payment integration via batching/partner capacity, not only pod count. Metric: `payment_in_flight`, reject rate, provider 429s.

---

## 14. How do you propagate cancellation when a user closes the app mid-request?

**Answer:** Client aborts HTTP/2 stream; gateway and services should honor **context cancellation**—stop DB work and release connections when the parent context is done. Many stacks still run orphan work unless server checks `ctx.Done()` between stages. Trace **client disconnect** vs **server timeout** separately; alert on high orphan work rate after 504s.

---

## 15. Inventory returns 503 for 2 minutes during a deploy. Error rate returns to normal but latency stays high for 30 minutes. Explain.

**Answer:** Classic **metastable failure**: retries and queued requests kept load high after deploy finished—**retry storm** + **backlog drain**. Fix: jittered backoff, **retry budget** per client, circuit breaker to inventory, temporarily **shed** non-critical paths, drain queues. Post-incident: limit max retry attempts at gateway, add **in-flight cap** ([java/BoundedInFlight.java](./java/BoundedInFlight.java)).

---

## Design & Trade-offs

## 16. Compare “retry in the client,” “retry in the API gateway,” and “retry only in the worker.”

**Answer:** **Client retry:** needed for mobile flaky networks—risk of duplication without keys. **Gateway retry:** centralizes policy but can **amplify** all clients—dangerous on POST. **Worker/async retry:** best for **non-user-facing** side effects with idempotent consumers and DLQ. Strong design: **one primary retry layer** per operation type, documented; others fail fast or surface retryable errors. Stacking three layers × 5 retries ≈ 125× load.

---

## 17. When would you reject requests with 503 instead of queueing them?

**Answer:** When **queue wait** would exceed user SLA anyway, or unbounded queue risks **OOM** (flash sale, payment cap). 503 + `Retry-After` tells clients to back off—preserves **partial availability** for accepted requests. Prefer **controlled rejection** over accepting everything and failing all at once. Product must handle “try again” UX for checkout—not ideal, but honest under overload.

---

## 18. How does backpressure differ from rate limiting?

**Answer:** **Rate limiting** caps **arrival rate** (tokens per second per key/IP)—fairness and abuse prevention. **Backpressure** reacts to **current saturation** (thread pool full, channel full, DB latency high)—“I cannot take more work **right now**.” Use both: rate limit at edge for fairness; backpressure inside service based on **downstream health** (adaptive concurrency). A steady 1k RPS can still overwhelm if each request triggers 50 DB queries.

---

## 19. You must choose: single-region strong ordering vs multi-region availability during partition. What questions do you ask?

**Answer:** Clarify **RPO/RTO**, regulatory residency, read vs write traffic, and whether **conflicts are mergeable**. Payments often need **single leader per account** or quorum (CP-lean) for writes; catalog may tolerate stale reads (AP-lean). Ask: “Can we **stop writes** in a minority partition?” and “What is the **reconciliation** playbook?” Link to PACELC in [Chapter 21](../21-cap-theorem-and-pacelc/README.md)—not a slogan, a product decision.

---

## 20. How do timeouts interact with idempotency keys on a payment API?

**Answer:** Timeout means **unknown outcome**—retry with the **same** idempotency key so provider returns original result (201/200) instead of a second charge. Key TTL must exceed **max client retry window** (e.g., 24 h). Server stores key → response mapping. Without keys, retry after timeout is a **business logic bug**, not “network flakiness.”

---

## Stretch

## 21. What is a fencing token and when do you need one?

**Answer:** A **monotonic token** (from lock service or DB) passed with writes so a **delayed old primary** cannot commit after a new primary took over—prevents split-brain corruption. Needed when using **leases/leader election** with async replication lag (shared storage, some DB failover modes). If you only have stateless HTTP APIs with single-writer per key, versioning may suffice; fencing is core in storage and queue design interviews.

---

## 22. Design a retry policy for a saga step that calls an external tax API (read-only) vs a capture-payment step (write).

**Answer:** **Tax (read):** safe to retry GET/cached POST with short backoff; cache result in saga state; deadline 300 ms, 2–3 retries with jitter. **Capture (write):** **idempotency key** per saga instance; on timeout, **reconcile** via provider status before retry; max 2 attempts inside saga timeout; on persistent ambiguity, **compensate** or park in manual review queue—never infinite retry. Persist saga state before calling capture ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/README.md)).

---

## 23. During an incident, on-call disables retries globally and error rate drops but customer complaints rise. Why?

**Answer:** Disabling retries reduced **load amplification** but increased **failed checkouts** on transient blips—users see hard failures instead of delayed success. Better: keep retries with **stricter caps, jitter, idempotency**, and fix downstream. Measure **success within SLA** not just 5xx rate. Communicate product impact before global retry kill switches.

---

## 24. How would you load-test backpressure before a flash sale (order of magnitude)?

**Answer:** Stepped test: 1× → 2× → 5× expected peak RPS (~e.g., 2k → 10k) with **synthetic checkout**; watch `in_flight`, GC, p99, payment 429/503, and **reject rate**. Inject slow payment (500 ms) and verify **stable reject** without OOM. Success: bounded memory, predictable 503 rate, recovery within minutes after spike—not unbounded queue growth. Run from **same region** as prod first, then cross-region tail.

---

## 25. Tell me about a time you prevented a cascading outage. What signals did you use?

**Answer:** (Behavioral, architect slant.) Strong story: identified **retry storm** or **thread pool exhaustion** via trace depth + retry metric; shipped **deadline propagation**, **jitter**, or **in-flight cap**; quantified **error budget saved** or p99 drop (e.g., 4 s → 800 ms). Weak: “we scaled pods.” Tie to **cross-team** change (gateway + service + client). Lesson: **stability under recovery** matters as much as peak QPS—document retry ownership in design review ([Chapter 24](../24-reliability-engineering/README.md)).
