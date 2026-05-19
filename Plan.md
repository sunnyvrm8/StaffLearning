# Staff Learning — Architect Preparation Plan

Curriculum for **Staff / Principal / Architect** interviews and on-the-job judgment: production trade-offs first, dual **Java + Go** examples where code applies, case studies after distributed and data foundations.

## How to use this plan

| Track | When | Goal |
|-------|------|------|
| **Core sequence** | Chapters 1→36 in order | Build concepts in dependency order |
| **Case studies** | After Ch. 20–28 (distributed + data + perf) | Integrate design under time pressure |
| **Leadership** | Parallel from Ch. 17 onward; deep dive Ch. 35 | Staff+ bar: influence, execution, hiring |
| **Interview meta** | After core + case studies; revisit Ch. 4, 27–28 | Cadence, weak spots, mock loops |

**Study rhythm (suggested):** 2 core chapters + 1 case study or leadership module + 5–10 interview questions from the topic bank.

**Skip / skim if already strong:** Ch. 4 (DS&A) if interviewing architect-only loops; Ch. 5–7 if runtime depth is proven—still sample for interview coding screens.

---

# Learning Handbook (core sequence)

Numbers are **chapter IDs** for `chapters/NN-slug/` folders. Slug = kebab-case title.

## Phase A — Engineering judgment & language depth

*Stakeholder pain: maintainability, team velocity, interview credibility.*

| # | Topic | Why this order |
|---|--------|----------------|
| 1 | SOLID and Core Engineering Principles | Frame trade-offs before patterns and scale |
| 2 | Design Patterns (GoF + enterprise: ports/adapters, strategy, circuit breaker) | Vocabulary for reviews and legacy evolution |
| 3 | Domain-Driven Design and Bounded Contexts | Align services, data ownership, and team boundaries |
| 4 | Data Structures and Complexity | Staff+ coding screens; complexity literacy for perf chapters |
| 5 | Java and Golang Deep Dive | Dual-stack fluency for examples and system reviews |
| 6 | Concurrency and Multithreading | Prerequisite for caching, messaging, and distributed timelines |
| 7 | Memory Management (JVM + Go GC, allocation, leaks) | Explains latency tails and OOM incidents |

## Phase B — Data path & interfaces

*Stakeholder pain: correctness, latency, schema evolution.*

| # | Topic | Why this order |
|---|--------|----------------|
| 8 | Networking and HTTP (DNS, TLS, HTTP/2–3, timeouts, connection pools) | Foundation for APIs, gateways, and service mesh |
| 9 | API Design (REST, gRPC, versioning, idempotency keys, pagination, errors) | Contract design before microservices split |
| 10 | Database Design and Data Modeling (SQL, NoSQL selection, normalization vs access paths) | Storage truth before cache and events |
| 11 | Indexing and Query Optimization | Read-path cost; pairs with Ch. 10 and observability |
| 12 | Caching Strategies (local, distributed, invalidation, stampede, TTL policy) | Applied after storage; before scale-out |

## Phase C — Build, ship, run

*Stakeholder pain: deploy safety, blast radius, operability.*

| # | Topic | Why this order |
|---|--------|----------------|
| 13 | Docker and CI/CD (pipelines, artifacts, rollout strategies) | Ship mechanics before orchestration |
| 14 | Kubernetes and Container Orchestration (scheduling, HPA, probes, config/secrets) | Runtime model for microservices and cloud |
| 15 | Load Balancing and Traffic Management (L4/L7, sticky sessions, health checks) | Bridges single service → fleet |
| 16 | Cloud Architecture (AWS-first + portable patterns: IAM, VPC, multi-AZ, managed services) | Concrete hosting before abstract distributed theory |

## Phase D — Distributed systems

*Stakeholder pain: consistency, duplication, partial failure.*

| # | Topic | Why this order |
|---|--------|----------------|
| 17 | Microservices Architecture (decomposition, sync vs async, API gateway, mesh overview) | Structure before deep failure modes |
| 18 | Event-Driven Architecture (events vs commands, CQRS, event sourcing overview) | Complements Ch. 3 and Ch. 19 |
| 19 | Kafka and Messaging (topics, partitions, consumer groups, ordering, DLQ) | Durable async path; industry default |
| 20 | Distributed Systems Fundamentals (clocks, partitions, retries, timeouts, backpressure) | Umbrella before CAP/consistency |
| 21 | CAP Theorem and PACELC | Decision lens after seeing real topologies |
| 22 | Consistency Models and Consensus (linearizability, quorum, Raft at architect depth) | Read/write path guarantees |
| 23 | Idempotency, Sagas, and Distributed Transactions | Payment/inventory patterns; interview favorite |
| 24 | Reliability Engineering (SLOs/SLIs, error budgets, chaos, DR, RTO/RPO) | Operability contract for scale chapters |

## Phase E — Security, visibility, performance

*Stakeholder pain: breaches, blind incidents, cost of scale.*

| # | Topic | Why this order |
|---|--------|----------------|
| 25 | Security Architecture (OWASP, secrets, mTLS, supply chain, compliance overview) | Trust boundaries across Ch. 8–17 |
| 26 | Observability (metrics, logs, traces, profiling, alerting, runbooks) | Close the loop on SLOs (Ch. 24) |
| 27 | Performance Engineering (profiling, tail latency, load testing, capacity signals) | Measure before declaring scale design |
| 28 | Scalability and Capacity Planning (horizontal vs vertical, sharding, hot keys, 10x levers) | Uses all prior phases |

## Phase F — Architect differentiation

*Stakeholder pain: platform drag, cloud bill, architecture drift, AI hype vs production.*

| # | Topic | Why this order |
|---|--------|----------------|
| 29 | Platform Engineering and Internal Developer Platforms (golden paths, self-service, paved roads) | Market expectation for senior ICs leading platform |
| 30 | Cost Architecture and FinOps (unit economics, tagging, right-sizing, egress) | Executive-visible; distinguishes Staff+ |
| 31 | Architecture Governance (ADRs, fitness functions, deprecation, standards) | Sustain decisions across years |
| 32 | AI and LLM Systems in Production (inference, routing, guardrails, eval, cost/latency) | 2025–2026 interview and product reality |
| 33 | RAG and Retrieval Architecture (chunking, embeddings, vector stores, freshness) | Dominant enterprise AI integration pattern |
| 34 | Agentic Systems and MLOps for AI (tools, memory, human-in-the-loop, monitoring drift) | Beyond RAG; production agent pitfalls |

## Phase G — Leadership & interview integration

| # | Topic | Why this order |
|---|--------|----------------|
| 35 | Leadership and Influence (see Leadership section) | Staff+ loop weight increases with level |
| 36 | Mock Interviews and Preparation (see Interview Preparation) | Capstone: timed reps on weak chapters |

### Cross-cutting chapter links

- **Ch. 3 ↔ 17–18:** bounded context → service boundaries and event contracts  
- **Ch. 9 ↔ 23:** API idempotency ↔ sagas and outbox  
- **Ch. 12 ↔ 28:** cache ↔ sharding and hot-key mitigation  
- **Ch. 24 ↔ 26:** SLOs ↔ dashboards and alert quality  
- **Ch. 31 ↔ all:** ADRs capture rejected alternatives from case studies  

---

# System Design Case Studies

Practice **after Ch. 20–28**. Each case: clarify → estimate → HLD → deep dive (storage + one of cache/queue/shard) → failures → scale 10x.

## Tier 1 — Interview core (do first)

| # | Case study | Stresses |
|---|------------|----------|
| 1 | URL Shortener | Hashing, redirect latency, analytics, rate limits |
| 2 | Payment System | Idempotency, sagas, consistency, PCI boundaries |
| 3 | Chat System | Real-time, presence, fan-out, ordering |
| 4 | Notification System | Multi-channel, queues, retries, preferences |
| 5 | Rate Limiter | Token bucket, Redis, fairness, distributed counters |
| 6 | Authentication System | Sessions vs JWT, OAuth, revocation, threat model |

## Tier 2 — Staff+ depth (do second)

| # | Case study | Stresses |
|---|------------|----------|
| 7 | Search Engine | Inverted index, ranking, crawl pipeline, freshness |
| 8 | Video Streaming | CDN, adaptive bitrate, upload pipeline, cost |
| 9 | Distributed Cache | Eviction, consistency, cluster membership, hot keys |
| 10 | Feed / Timeline (social) | Fan-out on write vs read, ranking, celebrity problem |
| 11 | E-Commerce Inventory and Orders | Reservations, oversell, flash sales, CQRS |
| 12 | Multi-Tenant SaaS | Isolation models, noisy neighbor, per-tenant SLAs |

## Tier 3 — Market extensions (pick by target company)

| # | Case study | Stresses |
|---|------------|----------|
| 13 | AI Recommendation System | Features, offline/online serving, cold start, eval |
| 14 | Real-Time Analytics / Metrics Pipeline | Ingestion, windows, exactly-once, query vs serve path |
| 15 | Ride-Hailing / Geospatial Matching | Geo indexes, supply/demand, surge, consistency |
| 16 | Collaborative Document Editor | OT/CRDT, presence, conflict resolution, sync |
| 17 | Ad Bidding / Real-Time Auction | Latency budget, fraud, budgeting, event volume |

---

# Leadership

- Technical Leadership and Technical Strategy  
- Roadmapping and Prioritization (outcomes vs output)  
- Mentorship and Growing Senior Engineers  
- Stakeholder Management (product, exec, legal, security)  
- Engineering Culture and Psychological Safety  
- Conflict Resolution and Crucial Conversations  
- Decision Making (one-way vs two-way doors, reversibility)  
- Execution and Delivery Accountability  
- Hiring and Interviewing (bar raising, loops, debriefs)  
- Cross-Team Alignment and Working Groups  
- RFCs and Written Communication  
- Incident Command, Postmortems, and Blameless Learning  
- Build vs Buy and Vendor Evaluation  
- Managing Technical Debt and Risk Trade-offs  

---

# Principal Engineer Thinking Framework

Use as **lens** while doing case studies and governance chapters—not a separate read order.

- How to Think Like a Principal Engineer (depth vs breadth, time horizons)  
- How to Answer System Design Questions (clarify, numbers, trade-offs, ops)  
- How to Lead Architecture Discussions (pre-read, options, decision record)  
- How to Influence Without Authority (alliances, pilots, metrics)  
- How to Take Ownership (end-to-end outcomes, escalation judgment)  
- How to Run Architecture Reviews (risks, NFRs, operability checklist)  
- How to Assess and Communicate Risk (security, compliance, migration)  
- How to Negotiate Technical Debt vs Feature Pressure  

---

# Interview Preparation

| Area | Focus |
|------|--------|
| Staff Engineer Interview Strategy | Scope, cross-team impact, system + coding mix |
| Principal Engineer Interview Strategy | Strategy, org design, multi-quarter bets |
| Behavioral Questions | STAR with metrics; technical judgment stories |
| Architecture Interview Questions | Deep dives on your real systems; failure stories |
| Coding Interview Approach | Ch. 4 patterns; communicate trade-offs; Java + Go |
| System Design Approach | Tie to case study tiers; PACELC and idempotency |
| Executive Communication | One-pagers, status without jargon, decision asks |
| Career Narrative and Portfolio | Themes: scale, reliability, cost, leadership |

---

*Handbook artifacts: `chapters/NN-slug/`, `case-studies/NN-slug/`, `leadership/slug/`, `interview-prep/slug/` per `.cursor/skills/handbook-topic-content/SKILL.md`.*
