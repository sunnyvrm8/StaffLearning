# Interview Questions: Reliability Engineering

**Bank size:** 10  
**Rationale:** SLO/ops-focused chapter; initial 10-question bank per rubric for targeted reliability drills.  
**Last updated:** 2026-05-20

---

## Core

## 1. Define SLI, SLO, and SLA and how they relate in a production organization.

**Answer:** **SLI** (indicator): measurable proxy for user happiness—successful checkout requests / total, or queue age p99. **SLO** (objective): internal target on SLI—99.9% monthly success. **SLA** (agreement): contractual consequence if SLA missed—credits, penalties. SLIs should be **user-centric** and **cheap to measure**; SLOs drive **error budgets** and release policy. SLAs are legal/commercial—often stricter SLIs with buffer.

---

## 2. What is an error budget, and how should it change behavior when exhausted?

**Answer:** **Error budget** = 100% − SLO allowance (e.g., 0.1% monthly downtime ≈ 43 minutes). While budget remains, teams **ship features** and accept measured risk. When **exhausted or burning fast**: freeze risky launches, focus on **reliability work** (incidents, debt, load tests), tighten change windows. Budget is a **product/engineering negotiation tool**, not punishment—prevents infinite “100% uptime” that blocks velocity.

---

## 3. Pick three SLIs for a public REST checkout API and justify each.

**Answer:** (1) **Availability:** ratio of non-5xx responses excluding client 4xx—captures “could not buy.” (2) **Latency:** fraction of requests < 500 ms at p99 or SLI on `histogram_quantile`—captures slow equals broken on mobile. (3) **Correctness/freshness** (if applicable): orders acknowledged vs payment capture reconciliation lag < 5 min—catches silent money bugs metrics miss. Avoid **CPU %** as user-facing SLI; use it as diagnostic. Align windows to **monthly** SLO with weekly burn alerts.

---

## 4. Explain RTO and RPO with a regional outage example.

**Answer:** **RPO** (recovery point objective): max **data loss** acceptable—e.g., 5 min of orders if async replication lag bound. **RTO** (recovery time objective): max **downtime** to restore service—e.g., 30 min to fail over DNS and warm pools. Region loss: if RPO=0, need **sync replication** or multi-write with conflict cost; if RPO=15 min, async replicate + replay from queue may suffice. **DR drills** prove RTO—paper plans lie. Tie to business: payments often RPO≈0, analytics RPO=hours.

---

## 5. What is chaos engineering, and what prerequisite makes it safe?

**Answer:** **Controlled experiments** injecting failure (kill pod, latency, partition) in **production or prod-like** env to validate assumptions. Prerequisite: **steady-state hypothesis**, observability, **blast radius limits**, and ability to **abort**—not random breakage. Start in staging with **game days**; in prod, small blast radius during business hours with on-call aware. Goal: find **unknown dependencies**, not prove Kubernetes works.

---

## Stretch

## 6. You have 99.95% availability SLO. How much downtime per month is allowed, and how do you alert before breach?

**Answer:** **~21.6 minutes/month** (30.4-day month: (1 − 0.9995) × 43,200 min). Use **multi-window burn rates** (Google SRE): fast burn (1 h window) pages on-call; slow burn (3 d) tickets for trend. Alert on **budget consumption rate**, not single blip 500s, to reduce fatigue. Pair with **error budget policy** in team charter.

---

## 7. Compare active-passive multi-region vs active-active for a payments read API.

**Answer:** **Active-passive:** simpler **consistency** and failover story; higher **RTO** (DNS, warm standby); lower cost. **Active-active:** lower latency globally, harder **split-brain**, idempotency, and regulatory data residency. Payments **writes** often active-passive or single-primary with read replicas; **reads** may be multi-region with sticky consistency. Document **failover drill** frequency and measured RTO.

---

## 8. On-call is burning out from pages. Name three reliability improvements that are not “add more people.”

**Answer:** (1) **Alert quality:** SLO-based burns, remove symptomatic CPU alerts, require runbook links. (2) **Toil reduction:** automate rollbacks, flaky test fixes, ticket routers—free time for engineering. (3) **Failure containment:** timeouts, bulkheads, feature flags, **chaos** findings implemented. (4) **Blameless postmortems** with tracked action items—repeat incidents are process failure. Measure **pages per engineer per week** and **MTTR** trend.

---

## 9. Behavioral: describe a production incident you led. What do interviewers want beyond the timeline?

**Answer:** Strong answers: **customer impact** quantified (failed checkouts/min, revenue at risk), **decision trade-offs** under uncertainty, **communication** to stakeholders, **root cause** (systemic, not “human error”), **preventive** changes shipped (tests, SLO, runbook, architecture). Weak: heroics-only story, no follow-up metrics. Tie to **error budget** and what you stopped shipping afterward—shows Staff+ judgment ([Chapter 26](../26-observability/README.md) for observability loop).

---

## 10. Design review checklist: launching a new critical dependency (fraud scorer). What reliability artifacts do you require?

**Answer:** **SLO/SLI** for dependency latency and availability; **timeout + bulkhead** in caller; **fallback** behavior documented (fail open vs closed—fraud usually closed). **Load test** at 2× peak; **chaos** inject 500 ms latency. **Runbook**, dashboards, **on-call** rotation for vendor + wrapper. **DR:** what if vendor region down? **Error budget** impact estimated. Reject “best effort HTTP call” without circuit breaker and idempotent retry policy on side effects.
