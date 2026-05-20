# Interview Questions: AI and LLM Systems in Production

**Bank size:** 10  
**Rationale:** Production LLM systems mix latency, cost, and safety; ten questions span routing, eval, and failure modes without cataloging every model vendor.  
**Last updated:** 2026-05-20

---

## Core

## 1. What stakeholder pains drive a **separate inference architecture** instead of “call OpenAI from the monolith”?

**Answer:** **Cost** pain (token spend unpredictable), **latency** tails (1–10s provider spikes), **reliability** (rate limits, outages), **compliance** (data residency, logging), **safety** (PII leakage, toxic outputs). A production path adds **routing**, **caching**, **fallback models**, **budgets**, **eval harness**, and **observability**—same reasons you would not put **raw payment APIs** everywhere without a **facade**.

---

## 2. Explain **model routing**: when do you send a request to a small vs large model?

**Answer:** **Router** (rules or ML) picks model by **task complexity**, **user tier**, **latency SLO**, and **risk** (medical/legal gets stricter model + more checks). Simple FAQ → **8B** class; multi-step reasoning → **70B** or external API. Trade-off: wrong routing yields **bad answers** (user pain) or **wasted** money (finance pain). Measure **per-intent** quality and **cost**—iterate with **offline** eval + **online** satisfaction proxies.

---

## 3. What are **guardrails** in production LLM serving—beyond a regex for profanity?

**Answer:** **Input** validation (length, **injection** patterns, **tool** argument schemas), **output** validators (JSON schema, **citation** required for claims), **policy** filters (PII redaction), **refusal** templates for disallowed topics, **human review** queues for edge cases. Implement as **pipeline stages** with **telemetry** per stage latency. Failure: guardrails add **500 ms**—budget them in **p99**.

---

## 4. Compare **offline eval** vs **online** signals for LLM quality—what do you trust for a launch gate?

**Answer:** **Offline**: golden datasets, **LLM-as-judge** (careful bias), **regression** suites on **safety** prompts—fast, reproducible. **Online**: user thumbs, **task success** (did ticket resolve?), **human spot checks**—noisy but real. Launch gate: **offline** must not regress **critical** metrics; **shadow** traffic or **canary** with **online** monitoring before **100%**. Never trust **offline** alone for **subjective** tone without **human** calibration.

---

## 5. How do **token economics** change system design for a support copilot handling 1M tickets/month?

**Answer:** Estimate **prompt + completion** tokens per ticket × **price per 1M tokens**—may dominate **infra** cost. Design: **summarize** threads to **bounded** context, **retrieve** only top-k chunks ([Chapter 33](../33-rag-and-retrieval-architecture/interview-questions.md)), **cache** embeddings/responses where safe, **batch** non-urgent tasks. Order-of-magnitude: **4k-token** average at **$1/M** mixed ≈ **$4k/month** per million interactions before caching—often **under-estimated** by **10x** without retrieval discipline.

---

## Stretch

## 6. **Streaming** tokens to the UI vs **batch** completion—trade-offs for mobile clients on flaky networks?

**Answer:** Streaming improves **perceived** latency and **cancelability** mid-generation—better UX. Downside: **more complex** client state, **harder** to log final answer until done, **intermediate** tokens may leak if user navigates away. Batch simplifies **analytics** and **guardrails** (validate full output). Hybrid: stream with **server-side** accumulation for **audit** log.

---

## 7. Describe **prompt injection** at the architecture boundary—how do you mitigate, not eliminate?

**Answer:** Untrusted text (web pages, emails) becomes **instructions** if concatenated naively into system prompts—models obey **attacker** goals (exfiltrate secrets, call tools). Mitigations: **separate** instruction vs data channels where APIs allow, **tool allowlists**, **output** egress controls, **no secrets** in prompts, **RLHF** + **classifiers** as **defense in depth**. Red-team with **automated** suites. Architecture truth: treat LLM as **untrusted code executor** when tools exist ([Chapter 34](../34-agentic-systems-and-mlops-for-ai/interview-questions.md)).

---

## 8. **Multi-tenant** LLM hosting: what isolation model do you propose for Enterprise A vs B on shared GPUs?

**Answer:** **Hard**: dedicated endpoints/namespaces, **VPC** peering, **no shared** prompt logs. **Soft**: logical **tenant_id** in metadata with **RBAC** on observability—cheaper but **higher** leak risk via **logs** or **support** access. Regulated tenants often pay for **dedicated** capacity. Always **encrypt** at rest and **purge** retention per contract.

---

## 9. Incident: support bot **hallucinated** a refund policy and customers were misled. What systemic fixes do you prioritize?

**Answer:** **Ground** answers in **retrieved policy docs** with **citations**, **confidence** thresholds triggering **human handoff**, **disable** high-risk intents via **feature flag**, **post-incident** eval set expanded. **Legal/comms** workflow for **customer correction**. Metric: **policy-contradiction** rate in **offline** eval weekly.

---

## 10. Design drill: **chat assistant** with **p95 < 3s** including retrieval; **99.5%** monthly availability. Sketch the serving path and one **fallback**.

**Answer:** Path: **API gateway** → **auth** → **router** → **retrieval** (vector + keyword) → **LLM** (primary region) with **timeouts** per stage; **async** queue for **non-interactive** follow-ups. **Fallback**: if LLM **timeout**, return **cached** answer for **FAQ** intents or **degraded** template: “I cannot answer now; here are links.” Cross-link: **SLOs** ([Chapter 24](../24-reliability-engineering/interview-questions.md)), **observability** ([Chapter 26](../26-observability/interview-questions.md)).

---
