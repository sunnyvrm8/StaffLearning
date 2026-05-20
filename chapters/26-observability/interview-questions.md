# Interview Questions: Observability

**Bank size:** 10  
**Rationale:** Observability is broad (metrics/logs/traces); ten questions anchor mocks on SLO-aligned signals and incident workflows.  
**Last updated:** 2026-05-20

---

## Core

## 1. Define **observability** in production terms—not “three pillars” marketing.

**Answer:** Observability is whether you can **answer new questions** about system behavior **without shipping new code**, using **telemetry you already emit**—e.g., “which dependency blew our checkout p99 after the canary?” It requires **high-cardinality context** (trace IDs, user cohort flags—not unlimited labels on metrics) and **correlation** across signals. Monitoring asks “is the dashboard green?”; observability supports **debugging unknown-unknowns** during incidents with **hypothesis testing** via queries.

---

## 2. Contrast **RED** and **USE** metrics—when do you reach for each?

**Answer:** **RED** (Rate, Errors, Duration) fits **request-driven services**: QPS, 5xx ratio, latency histograms—directly maps to **user-perceived** SLOs for APIs. **USE** (Utilization, Saturation, Errors) fits **resources**: CPU%, disk IO queue depth, NIC drops—finds **bottlenecks** before they become user errors. Use RED on the **golden signals** dashboard per service; drill into **USE** on nodes/pools when RED degrades but logs are inconclusive. Mistake: only CPU graphs while **latency** tails come from **GC or lock contention** invisible to simple averages.

---

## 3. How do you decide **sampling** for distributed traces without losing the ability to debug incidents?

**Answer:** **Head-based** sampling (random at ingress) is simple but may drop the **one** bad trace you need. **Tail-based** sampling (observe full trace, then keep if error or slow) costs more ingest but preserves **signal**. Hybrid: 1% baseline + **100%** for canary builds or **error** paths. Budget: at 50k RPS, 100% trace export can be **$** and **CPU** prohibitive—negotiate **retention** (hours vs days) with legal. Always propagate **trace context** even when not exporting—so you can **turn up** sampling during incidents.

---

## 4. Why do **logs** become expensive faster than metrics, and how do teams control that?

**Answer:** Logs are **high volume per event** and often **unstructured**—storage and index costs scale with **text size** and **cardinality** of dynamic fields. Metrics aggregate in TSDB with **bounded cardinality** per series. Controls: **structured JSON** with **stable** keys, **dynamic level** (info vs debug), **sampling** for debug lines, **centralized** retention tiers (hot 7d, cold 30d). Anti-pattern: logging **full payloads** (PII + cost bomb). Order-of-magnitude: a chatty service can emit **GB/day** per instance at peak—finance notices before SRE does.

---

## 5. Tie **SLIs** to instrumentation: how would you define an SLI for “checkout succeeds”?

**Answer:** Pick **user-visible** outcome: **proportion of checkout attempts** that return **2xx within 800 ms** and **without** subsequent compensating refund within 5 minutes (if you can join events—or proxy with “payment intent confirmed”). Instrument at **edge** (ingress) and **downstream** (payment client) with **histogram** latency and **error** attributes. Avoid SLI defined only on **worker success** if users see timeouts—include **client-reported** timeouts if mobile app telemetry exists. Cross-link: error budgets ([Chapter 24](../24-reliability-engineering/interview-questions.md)).

---

## Stretch

## 6. **Alert fatigue**: your on-call gets 200 pages a week, mostly non-actionable. What do you change architecturally and culturally?

**Answer:** **SLO-based** alerts: page on **burn rate** consuming error budget, not every threshold twitch. **Runbooks** required before alert merges—if no action, **delete** or downgrade to ticket. **Aggregation**: one incident for correlated blast, not per pod. **Ownership**: service team tunes thresholds; platform provides **templates**. Metric: **MTTA** vs **noise ratio** (pages without incident). Failure mode: “wrap everything in P1” trains humans to ignore real fires.

---

## 7. During an incident, **distributed traces** show gaps—some spans missing between services. What are the top three causes?

**Answer:** (1) **Context propagation** broken (library not injecting `traceparent`, gRPC metadata dropped, async boundary without attach). (2) **Sampling** or **span limits** dropping segments. (3) **Clock skew** making spans look disconnected or filtered. Debugging: reproduce in staging with **forced** trace, add **integration tests** for propagation across **queues** ([Chapter 19](../19-kafka-and-messaging/README.md)) and **thread pools**.

---

## 8. When do you use **continuous profiling** (e.g., pprof, async profiler) in production, and what risks do you mitigate?

**Answer:** Profiling finds **hot paths** and **allocation churn** driving tail latency—valuable when CPU profiles from APM are too coarse. Risks: **overhead** (keep sampling low, short windows), **PII in stack** if frames include user strings, **safety** on low-latency paths (pauseless profilers). Pair profiles with **deploy markers** to catch regressions. Trade-off vs **tracing**: profiles show **where** CPU goes; traces show **which requests** suffer.

---

## 9. Explain **metric cardinality explosion** with a realistic example and fix.

**Answer:** Example: adding `user_id` as a Prometheus label on **HTTP request duration**—1M users → **unbounded** series, TSDB OOM. Fix: use **logs or traces** for per-user investigation; metrics stay **aggregated** (route, status, region). For **feature flags**, cap label values or use **exemplars** linking histograms to traces. Platform guardrail: **lint** on merge blocking high-cardinality labels.

---

## 10. You are designing **runbooks + dashboards** for a new payments service. What three views do you require before go-live?

**Answer:** (1) **Golden RED** dashboard with **SLO line** and burn-rate panels. (2) **Dependency** health (ledger, fraud scorer) with **timeouts** and **queue depth** if async. (3) **Business reconciliation** view: counts of **authorized vs captured** vs **voided** with anomaly detection. Runbook: **first actions** for timeout spike, **kill switch** for feature flag, **escalation** to payment partner status page. Cross-link: idempotency ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/interview-questions.md)).

---
