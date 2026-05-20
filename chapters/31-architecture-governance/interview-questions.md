# Interview Questions: Architecture Governance

**Bank size:** 10  
**Rationale:** Governance is process-heavy; ten questions tie ADRs, fitness functions, and deprecation to velocity and risk without HR-policy depth.  
**Last updated:** 2026-05-20

---

## Core

## 1. What is an **ADR** (Architecture Decision Record) good for—and what is it explicitly not?

**Answer:** ADRs capture **context, decision, consequences** for **significant** forks: “gRPC vs REST for mobile sync,” “active-active vs primary-secondary.” They onboard **future teams** and prevent **groundhog debates**. They are **not** a dump of every meeting—avoid ADR spam for **trivial** choices. Good ADRs list **rejected alternatives** with **why**—that is what principal reviews mine during incidents.

---

## 2. Define **architecture fitness functions** with an example measurable in CI.

**Answer:** Automated checks that **score** architecture qualities: **“no synchronous call from checkout to analytics”** enforced by **static analysis** or **dependency rules** (ArchUnit, import-linter). Another: **p95 build** under **10 min**, **container** image **no critical CVEs**, **SLO** tests on **canary**. They turn “we value modularity” into **failing builds**—trade-off: **brittle** rules if overfitted to today’s graph.

---

## 3. How do you run a **standards process** that does not become a **paperwork guild**?

**Answer:** Standards are **few**, **versioned**, and **tool-backed** (linters, golden templates). **RFC** for changes with **time-boxed** comment period and **named approvers** (staff+ domain owners). **Exceptions** require **expiry** and **risk owner**. Measure **time in review** vs **incidents prevented**—if RFCs take **weeks** for low risk, **tier** the process (minor vs major).

---

## 4. What belongs in a **deprecation policy** for public APIs and internal libraries?

**Answer:** **Sunset timeline** (minimum notice, e.g., **6–12 months** for public), **migration guide**, **metrics** on remaining usage, **owner** for answering questions, **enforcement** (HTTP **Sunset** headers, CI warnings). Internal libs: **semver** discipline, **codemods** when possible. Failure mode: “deprecated” for **years** with **no** enforcement—teams never migrate.

---

## 5. How do **architecture reviews** differ from **security reviews** and **code reviews**?

**Answer:** **Arch review**: **NFRs** (scale, consistency, cost, operability), **boundaries**, **data ownership**, **failure modes**—often pre-PR. **Security review**: **threat model**, **secrets**, **authz**—can overlap but different lens. **Code review**: **correctness** and **local** quality. Running only code review yields **beautiful** PRs that **shard wrong**. Cadence: arch review at **MVP** and **before GA** scale milestones.

---

## Stretch

## 6. A team says governance **blocks innovation** because RFC took a month. How do you respond as a **Staff+** peer?

**Answer:** Separate **genuine** delay (missing approvers, unclear template) from **avoidance** of hard trade-offs. Propose **fast path** for **two-way door** decisions with **auto-approve** if no risk comments in **48h**, and **slow path** only for **data plane** changes. Publish **SLA** for review turnaround. If governance was right—**bad idea prevented**—share **postmortem** of similar past incident to rebuild trust.

---

## 7. An incident reveals the system violated a documented **standard** (no timeouts on HTTP). What governance follow-up is appropriate?

**Answer:** Treat as **systemic** gap: add **fitness function** (lint rule, service mesh default), **audit** other services, **train** on template. Blameless on author; accountability on **platform** for missing defaults. Track **%** services compliant weekly.

---

## 8. Describe a **multi-year** governance decision (e.g., cloud vendor or event backbone) and how you keep it from going stale.

**Answer:** ADR series with **review triggers**: **cost** drift, **feature** gaps, **regulatory** change, **error budget** burn on migration path. **Quarterly** **revisit** checklist with **executive** sponsor. Document **exit options** even if expensive—**option value** matters to boards.

---

## 9. How do you detect **architecture drift** in a microservice estate of 200 services?

**Answer:** **Automated inventory**: runtime versions, **dependency** graphs, **SLO** presence, **API** versioning compliance. **Scorecards** (like Backstage + Sonar rules) highlight outliers. **Synthetic** journeys catch **semantic** drift (“works in metrics, broken for users”). Prioritize **customer-critical** flows first.

---

## 10. Design review checklist: what **five bullets** do you always scan before approving a new **event-sourced** subsystem?

**Answer:** (1) **Projection rebuild** time and **storage** cost at 10x event volume. (2) **Schema evolution** (compat consumers). (3) **PII** in events and **retention**. (4) **Ordering** and **idempotency** guarantees ([Chapter 18](../18-event-driven-architecture/README.md), [Chapter 23](../23-idempotency-sagas-and-distributed-transactions/interview-questions.md)). (5) **Operational** story: **replay** drills, **monitoring** lag, **on-call** runbooks. Reject if only **happy path** slides exist.

---
