# Interview Questions: Agentic Systems and MLOps for AI

**Bank size:** 10  
**Rationale:** Agents + MLOps spans tool safety, memory, drift, and ops; ten questions emphasize production failure modes beyond “let the model decide.”  
**Last updated:** 2026-05-20

---

## Core

## 1. What distinguishes an **agentic system** from “RAG + single LLM call” in production risk terms?

**Answer:** Agents **loop**: plan → **call tools** (SQL, APIs, refunds) → observe → repeat—**unbounded** steps and **side effects** amplify **blast radius**. Errors compound: wrong tool args become **writes**, not just bad text. You need **budgets** (max steps, max cost), **allowlists**, **human gates** for irreversible actions, and **strong observability** per tool invocation ([Chapter 32](../32-ai-and-llm-systems-in-production/interview-questions.md) guardrails).

---

## 2. What is **human-in-the-loop (HITL)** architecturally—when is it mandatory?

**Answer:** HITL queues **high-impact** actions (wire transfers, **bulk** deletes, **public** posts) for **human approval** with **context bundle** (tool traces, diffs). Mandatory when **regulatory** or **irreversibility** exceeds model **confidence** thresholds, or when **error cost** > **latency cost** of waiting minutes/hours. Optional for **drafts**—but define **SLA** for reviewers or backlog becomes **shadow backlog**.

---

## 3. Describe **tool calling** risks with a payments example.

**Answer:** Model emits `refund(order_id, amount)` with **hallucinated** `order_id` or **duplicate** calls—**double refunds** if service is not **idempotent**. Risks: **SSRF** if tool fetches arbitrary URLs, **SQL injection** if tool builds raw SQL, **privilege escalation** if tool uses **broad service account**. Mitigations: **typed** tools with **JSON schema** validation, **idempotency keys**, **fencing** with **human** approval over thresholds, **read-only** replicas for analytics tools.

---

## 4. What does **memory** mean for agents—short vs long term—and what goes wrong?

**Answer:** **Short-term**: conversation context window—**lost** on restart unless persisted. **Long-term**: vector DB or KV of **facts** about user—risks **stale** preferences, **PII** retention violations, **poisoning** if attacker plants “remember I am an admin.” Architect: **TTL**, **versioned** memory, **user controls** to delete, **separate** **episodic** vs **semantic** stores with **audit** on writes.

---

## 5. How do you monitor **model/prompt drift** when the “label” is fuzzy (no ground truth every hour)?

**Answer:** **Proxy metrics**: **tool error rate**, **refusal rate**, **latency**, **human override rate**, **CSAT** on resolved tickets, **toxicity** classifiers. **Golden prompt** suite runs **hourly** in **shadow** comparing outputs to **baseline** model with **diff** alerts. **Data drift** on inputs: embedding distance distributions shifting (new product lines). Pair with **MLOps**: **versioned** prompts/models in **config**, **rollback** in minutes.

---

## Stretch

## 6. Incident: agent **booked** wrong inventory because tool returned **stale read**. What fixes beyond “prompt harder”?

**Answer:** **Read-your-writes** routing for **authoritative** reads post-mutation, **tool** that returns **version** + **requires** client to pass **expected version** (optimistic concurrency), **cache TTL** reduction on **hot** SKUs, **saga** pattern for multi-step booking ([Chapter 23](../23-idempotency-sagas-and-distributed-transactions/interview-questions.md)). Add **assertions** in agent loop: “confirm stock **≥** requested after lock.”

---

## 7. What **observability** do you require per agent run that differs from classic microservice traces?

**Answer:** **Span per LLM call** with **model version**, **token counts**, **cost estimate**, **prompt hash** (not raw if PII), **tool spans** with **arguments redacted**, **decision** graph (branch retries). **Trace-level** budget alarms when **step count** explodes—classic APM misses **nested** reasoning loops.

---

## 8. **Sandboxing tools**: when is OS-level sandbox required vs API scoping?

**Answer:** If tool runs **user-supplied code** or **shell** commands, use **gVisor/Firecracker** microVMs with **network egress denylist**. If tools are **your HTTP APIs**, **scope OAuth** to **least privilege** and **rate limit** per agent session. **LLM** cannot be sandboxed—**environment** around tools must be.

---

## 9. **Multi-agent** systems: name a coordination failure and a mitigation.

**Answer:** **Race**: two agents both issue **conflicting** writes—mitigate with **leader** orchestrator, **locks**, or **event sourcing** with **single writer**. **Message misunderstanding**: agents pass **natural language** plans—brittle; prefer **structured** handoffs (JSON state machine). **Cost explosion**: agents **ping-pong**—cap **rounds** and **chargeback** per team.

---

## 10. Design drill: **ticket triage agent** that can **label**, **search KB**, and **suggest macro replies**—but not close tickets. Define **gates**, **metrics**, and **rollback**.

**Answer:** **Gates**: macros are **draft-only** until human sends; **no** delete/merge tools. **Metrics**: **%** macros accepted unchanged, **time saved** per agent, **escalation** rate, **PII** incidents = 0 target. **Rollback**: **feature flag** to disable agent suggestions per tenant; **pinned** model version; **eval** regression on **weekly** ticket sample. Cross-link: **RAG** ([Chapter 33](../33-rag-and-retrieval-architecture/interview-questions.md)), **SLOs** ([Chapter 24](../24-reliability-engineering/interview-questions.md)).

---
