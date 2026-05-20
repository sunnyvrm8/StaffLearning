# Chapter 35: AI Engineering Fluency and Agent Design

> **One line:** Staff-level AI work is not “call an API”—it is **owning contracts** (prompts, tools, eval, safety, cost) so probabilistic components behave like **reliable subsystems** inside products that still owe users SLAs and auditors a straight story.

## Why this matters in production

A **support copilot** proposes refunds and account changes. The team ships a chat box on top of a general-purpose model with **ad-hoc prompts** in code and “tools” that are thin wrappers around internal REST. A model update **hallucinates** a policy exception, a duplicate tool proposal **double-refunds** during a retry storm, and legal discovers **no audit trail** tying model version to advice. Meanwhile **p95 latency** hits 18 s because each turn fans out to six uncached retrieval calls and unbounded ReAct loops, and **monthly spend** is 4× budget with nobody owning unit economics per resolved ticket.

Stakeholders feel **unpredictable behavior**, **unbounded cost**, and **unexplained incidents**—the same pains as any distributed system, except the core component is **non-deterministic** and vendor-controlled. Fluency means you can **design agent surfaces** (workflow boundaries, tool schemas, human gates, eval gates) so the product stays governable. This chapter completes the arc from [Chapter 32: AI and LLM Systems in Production](../32-ai-and-llm-systems-in-production/README.md) through [Chapter 33: RAG and Retrieval Architecture](../33-rag-and-retrieval-architecture/README.md) and [Chapter 34: Agentic Systems and MLOps for AI](../34-agentic-systems-and-mlops-for-ai/README.md) with **hands-on ownership**: how prompts and tools become versioned interfaces, how workflows fail, and what you document in design review.

## Core ideas

### Fluency vs “using ChatGPT”

**Fluency** is the ability to move across **problem framing**, **data and tool contracts**, **evaluation**, and **operability** without treating the model as magic. In production, that maps to: you can explain **why** a workflow is single-shot vs multi-step, **where** state lives, **what** happens on timeout, and **how** you detect regression when the provider ships a silent behavior change.

| | Fluency | Consumer API usage |
|---|---------|---------------------|
| **When** | Productized agents, regulated advice, cost-sensitive scale | Prototypes, internal drafts |
| **Risk** | Tool side effects, data leaks via context, runaway loops | Wrong tone, slow drafts |
| **Ops signal** | Task success rate, $/task, tool error taxonomy, judge drift | Subjective “quality” |

### Prompts and system messages as versioned contracts

Treat **system prompts** and **developer messages** like **public interfaces**: breaking changes need migration, compatibility windows, and rollback. Pair prompts with **structured outputs** (JSON schema, tool declarations) so downstream code validates rather than parses free text. **Dynamic prompt assembly** from many snippets is convenient and dangerous—order, duplication, and hidden token growth create “works in dev” failures at scale.

**Production anchor:** A **billing dispute assistant** loads policy snippets by jurisdiction; a bad merge duplicates contradictory rules and the model confidently picks the wrong one. Fix: **single-responsibility prompt modules**, explicit precedence (“if conflict, escalate”), and **golden tests** that assert required citations or refusal behavior.

### Tool design: schemas, least privilege, and blast radius

Tools are **RPC endpoints the model may invoke**. Design them with **narrow verbs**, **explicit input schemas**, **timeouts**, **idempotency** for mutating calls, and **RBAC** enforced in the adapter—not in prompt prose. High-blast tools (refunds, mass email, infra changes) belong behind **human approval**, **dual control**, or **simulation/dry-run** paths.

Compare **fat tools** (`doEverythingForCustomer`) vs **composable tools** (`lookupOrder`, `proposeRefund` that returns a draft for approval). Fat tools reduce orchestration complexity for the model but **concentrate risk** and complicate testing. Composable tools increase **orchestrator** logic but yield clearer **eval cases** and safer partial failure.

Code: [java/AgentToolCall.java](./java/AgentToolCall.java), [go/agent_tool_call.go](./go/agent_tool_call.go) — bounded mutating call with deterministic idempotency material.

See [diagrams/tool-invocation-sequence.md](./diagrams/tool-invocation-sequence.md).

### Workflow patterns: ReAct, graphs, and where the model decides

**ReAct-style** loops (reason → act → observe) are easy to prototype and hard to cap: you need **max iterations**, **per-step budgets**, **stop conditions**, and **circuit breakers** when tools error or contradict. **Graph / state-machine** workflows fix **allowed transitions** (e.g., always `retrieve` before `answer`; never skip `compliance_check` for EU users) and let the model **fill slots** within nodes rather than invent topology each turn.

| Pattern | Strength | Failure mode |
|---------|----------|----------------|
| **Single-shot + structured output** | Predictable latency/cost | Underfits complex multi-doc reasoning |
| **Bounded loop** | Flexible tool use | Runaway retries, oscillation between tools |
| **Explicit graph** | Enforce policy order, testability | Rigid; more upfront design |
| **Human-in-the-loop** | High-trust actions | Queue latency, operator toil |

See [diagrams/overview.md](./diagrams/overview.md).

### Memory: session, user, and org—minimize PII and maximize control

**Session memory** (last N turns) is safest by default. **Long-term user memory** improves UX and creates **retention, correction, and deletion** obligations—tie to product privacy model. **Org memory** (SOPs, runbooks) should be **grounded** with citations and freshness timestamps. Anything stored cross-session needs **TTL**, **access control**, and **audit** like any customer data store ([Chapter 25: Security Architecture](../25-security-architecture/README.md) trust boundaries).

### Evaluation for workflows—not only single-turn accuracy

Workflow eval needs **task-level success**, **tool correctness** (right tool, valid args, no redundant calls), **latency and cost per task**, and **safety** rubrics (refusal when appropriate). Combine **golden traces** (replay), **synthetic adversarial** cases (jailbreak + tool exfiltration attempts), and **small human spot checks** on judge drift if you use model-as-judge.

See [diagrams/eval-for-workflows.md](./diagrams/eval-for-workflows.md). Link to observability for online SLOs: [Chapter 26: Observability](../26-observability/README.md).

### Literacy → ownership on a Staff+ team

Fluency becomes **ownership** when you publish **SLIs** (task success, escalation rate), **cost per successful task**, **runbooks** for provider outages, and **ADRs** for rejected agent designs ([Chapter 31: Architecture Governance](../31-architecture-governance/README.md)). You align product, legal, and security on **what the agent may never do** versus what it may propose.

## When to use / when to avoid

**Use when:** customer-facing or operator-facing workflows need **natural language** plus **actions** on real systems; quality is “good enough” only with **explicit gates**; you can fund **eval + observability** alongside model bills.

**Avoid when:** a deterministic rules engine or form-based UI solves the job with less risk; **no owner** for tool RBAC and incident response; **no logging** of prompts/responses with privacy review—shipping an agent to absorb uncertainty you still owe users to eliminate.

## How it fails

- **Duplicate side effects:** model replays tool calls after timeouts—no idempotency keys on mutators ([Chapter 9: API Design](../09-api-design/README.md), [Chapter 23: Idempotency, Sagas, and Distributed Transactions](../23-idempotency-sagas-and-distributed-transactions/README.md)).
- **Prompt injection exfiltrates tools:** untrusted document content instructs the model to call `exportCustomerDatabase`—missing **tool allowlists** and **output constraints** per role.
- **Silent regression:** provider updates change refusal rates; no **offline suite** or **canary**—support metrics move over a week before anyone notices.
- **Cost blowout:** retrieval pulls 50 chunks × expensive model each turn—no cache, no rerank cap, no escalation to cheaper model for triage.
- **Human queue gridlock:** every refund awaits approval during Black Friday—**SLA** for human step not modeled; automation tier missing.

**Debugging hooks:** per-step trace IDs, tool latency histogram, token usage per stage, judge score distributions, rate of `tool_error` codes, comparison of **shadow** vs **prod** prompt behavior.

## Architect takeaway

- **Decide:** workflow topology (graph vs loop), tool surface area, memory tiers, human gates, model routing policy, eval gates on release.
- **Measure:** task success, time-to-resolution, $/successful task, tool error taxonomy, escalation rate, safety incidents, p95 end-user latency.
- **Document in design review:** trust boundaries and data classes in context; tool matrix with RBAC; failure modes for ambiguous tool outcomes; rollback plan for prompts/tools; cross-links to security and reliability chapters.

## Diagrams

- [Control plane overview](./diagrams/overview.md)
- [Tool invocation sequence](./diagrams/tool-invocation-sequence.md)
- [Eval for workflows](./diagrams/eval-for-workflows.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| Refund tool with idempotency + timeout | [java/AgentToolCall.java](./java/AgentToolCall.java) | [go/agent_tool_call.go](./go/agent_tool_call.go) |

**Production note:** Keep **orchestration** in typed code; let the model choose among **declared** tools with **validated** arguments. Never expose raw SQL or arbitrary HTTP from tool adapters without sandbox and policy.

## Related topics

- [Chapter 32: AI and LLM Systems in Production](../32-ai-and-llm-systems-in-production/README.md) — inference, routing, guardrails, cost/latency baselines  
- [Chapter 33: RAG and Retrieval Architecture](../33-rag-and-retrieval-architecture/README.md) — chunking, embeddings, freshness for grounded answers  
- [Chapter 34: Agentic Systems and MLOps for AI](../34-agentic-systems-and-mlops-for-ai/README.md) — agents in production, drift, monitoring loops  
- [Chapter 9: API Design](../09-api-design/README.md) — idempotency keys, errors, versioning discipline for tools-as-APIs  
- [Chapter 25: Security Architecture](../25-security-architecture/README.md) — secrets, mTLS to providers, abuse cases  
- [Chapter 26: Observability](../26-observability/README.md) — traces across model and tool spans  
- [Chapter 31: Architecture Governance](../31-architecture-governance/README.md) — ADRs for model and prompt changes  

## Interview preparation

See [interview-questions.md](./interview-questions.md) (50 questions—multi-concept Staff+ fluency arc per interview-bank-rubric).
