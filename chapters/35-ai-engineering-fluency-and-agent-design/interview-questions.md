# Interview Questions: AI Engineering Fluency and Agent Design

**Bank size:** 50  
**Rationale:** Multi-concept Staff+ chapter (prompts, tools, workflows, eval, safety, ownership) per interview-bank-rubric for core AI arc depth.  
**Last updated:** 2026-05-20

---

## Foundations

## 1. In one sentence, what does “AI engineering fluency” mean on a Staff+ team?

**Answer:** It means you can **design and operate** LLM-backed features as **subsystems**—with explicit contracts, budgets, eval gates, and incident ownership—not only integrate a vendor API behind a prompt string.

---

## 2. Why is treating prompts as “just strings” a production risk?

**Answer:** Prompts are **behavior interfaces**: silent edits change refusal rates, formatting, and tool selection. Without **versioning**, tests, and rollback, you get **undeployable regressions** that appear when the provider updates weights or when two engineers merge conflicting system instructions. Strings need the same change discipline as code.

---

## 3. What is the difference between a tool and a plain retrieval call in an agent architecture?

**Answer:** **Retrieval** returns information to condition the model; **tools** execute **side effects** or privileged reads (charge card, open ticket, query PII). Tools need **schemas, RBAC, timeouts, idempotency**, and **audit** like RPC. Retrieval needs **grounding and freshness** controls. Blurring the boundary (“let the model SQL”) concentrates risk.

---

## 4. What is structured output, and when is it mandatory?

**Answer:** **Structured output** constrains the model to JSON/XML/schema-valid shapes instead of free prose for machine consumption. It is mandatory when downstream code **parses** model decisions (routing, tool args, UI payloads) or when **compliance** requires explicit fields (reason codes, citations). Free text is fine for human-facing narrative when no parser depends on it.

---

## 5. Define “task success” for a multi-step support agent.

**Answer:** **Task success** is a product-level outcome: the correct **final state** (ticket resolved, refund issued or correctly denied, customer informed) within **SLA**, without **forbidden actions**. It is stronger than single-turn helpfulness—a chain of “nice” replies that ends in a policy violation is a failure.

---

## 6. What is prompt injection in a tool-using agent, and who is the attacker?

**Answer:** **Prompt injection** is untrusted content (web page, ticket text, uploaded doc) that manipulates the model to **ignore developer instructions** or misuse tools. The attacker is often **external** (malicious customer) or **compromised content** in your corpus. Defenses combine **instruction hierarchy**, **tool allowlists**, **output filtering**, and **least-privilege** adapters—not “please don’t” in the system prompt alone.

---

## 7. Why are max-iteration caps insufficient as the only loop control?

**Answer:** Caps prevent **infinite** loops but still allow **expensive finite** oscillation (A→B→A for N steps) or **runaway retrieval** within each step. You also need **per-step budgets**, **duplicate tool call detection**, **circuit breakers** on error classes, and **escalation** to humans or cheaper models when progress stalls.

---

## 8. What belongs in a system prompt versus application code?

**Answer:** **System prompt:** stable role, tone, policy summaries, formatting rules, escalation triggers in **natural language**. **Code:** allowlists, numeric thresholds, **authz**, **schema validation**, **secrets**, **retry/idempotency**, **routing** between models, and **workflow topology** you must not let the model rewrite. If it must never be bypassed, it should not rely on prose alone.

---

## 9. Explain “model as judge” evaluation and its main failure mode.

**Answer:** A secondary model **scores** outputs against a rubric (helpfulness, policy adherence). **Failure mode:** **judge drift**—the judge correlates with the same biases as the generator, or changes when the judge model updates—producing **false confidence**. Mitigate with **human spot checks**, **adversarial cases**, and **disagreement alerts** between judges or vs. heuristics.

---

## 10. What is the difference between session memory and long-term user memory for agents?

**Answer:** **Session memory** is ephemeral per conversation—lower privacy risk, simpler. **Long-term memory** persists preferences or facts across sessions—better UX but requires **consent, correction paths, deletion**, and **access control** like any customer profile store. Staff teams document **data classes** flowing into each tier.

---

## 11. Name three SLIs you would track for a production internal copilot.

**Answer:** **Task completion rate** (or deflection where appropriate), **p95 end-to-end latency** per workflow, and **cost per successful task** (tokens + retrieval + tool calls). Add **tool error rate** and **human escalation rate** when actions are involved—optimizing only helpfulness invites silent unsafe shortcuts.

---

## 12. What is “instruction hierarchy,” and does it fully solve jailbreaks?

**Answer:** **Instruction hierarchy** is provider or pattern support that prioritizes **developer/system** rules over user content. It **reduces** casual jailbreaks but is not a complete security boundary—**tools and data access** must still enforce **least privilege** and **allowlists**. Treat hierarchy as UX alignment, not authorization.

---

## Design & Trade-offs

## 13. When would you choose a fixed workflow graph over a free-form ReAct loop?

**Answer:** Choose a **graph** when **policy order** must never be skipped (e.g., compliance check before refund proposal), when you need **testable transitions**, or when **latency variance** from unbounded search is unacceptable. Choose **ReAct** when exploration is intrinsic and you can still enforce **caps** and **tool sets** per node. Regulated or high-blast domains favor graphs.

---

## 14. Compare “fat tools” vs “small composable tools” for LLM agents.

**Answer:** **Fat tools** reduce orchestration burden on the model and can lower round trips but **hide decisions** inside opaque server logic and complicate eval. **Composable tools** increase model steps and failure modes but yield **clearer traces**, finer **RBAC**, and **unit-testable** components. Staff compromise: small public surface with **facade tools** implemented as internal compositions in code.

---

## 15. Should the model choose which model (self-routing) to call in production?

**Answer:** Only with **guardrails**: routing rules validated in code, **budgets**, and **telemetry**. Letting the model freely pick endpoints can **leak** to premium models, **bypass** safety filters, or **amplify** injection. Common pattern: **orchestrator** picks tier from **heuristics** (complexity estimate, tenant plan); model may **request** escalation, humans or rules approve.

---

## 16. Single-shot vs multi-turn agent: what is the main latency trade-off?

**Answer:** **Single-shot** with structured output minimizes **round trips** and tail latency but may **underfit** tasks needing tool feedback. **Multi-turn** improves quality on hard tasks but multiplies **serial latency** and **failure compounding**. Mitigate with **parallelizable retrieval**, **early-exit** heuristics, and **smaller** models for triage.

---

## 17. How do you decide human-in-the-loop placement without bottlenecking operations?

**Answer:** Gate **high-blast** or **irreversible** actions (large refunds, org-wide sends) and automate **low-blast** steps with **simulation previews**. Measure **queue depth and approval SLA**; if humans become the bottleneck, add **tiered limits** (auto-approve under $X with monthly caps), **risk scoring**, or **dual control** only above thresholds—not universal approval.

---

## 18. What is the trade-off of exposing SQL as a tool to the model?

**Answer:** You gain flexibility but **centralize catastrophic risk** (exfiltration, destructive queries, performance incidents). Prefer **parameterized** read-only views, **row-level security**, **query cost limits**, and **static** query templates with **slots**. Raw SQL is rarely justified outside heavily sandboxed analytics with synthetic data.

---

## 19. When is fine-tuning preferable to prompt engineering for an agent?

**Answer:** **Fine-tuning** helps when you need **consistent style**, **domain vocabulary**, or **format adherence** at scale cheaper than giant prompts, and you have **curated** training/eval data with **privacy** clearance. Avoid fine-tuning to patch **security** or **authorization** bugs—those belong in code. Also weigh **release friction** (retraining pipelines) vs prompt iteration.

---

## 20. How do streaming responses interact with tool calling UX?

**Answer:** Streaming improves **perceived latency** for narrative but complicates **tool calls** if clients render partial JSON. Production clients often **buffer** tool call fragments until valid, show **typing** states, and **cancel** in-flight requests on user abort. Orchestrators must handle **interrupted** generations without executing half-formed tool intents.

---

## 21. RAG vs “long context only” for an enterprise agent: compare on cost and correctness.

**Answer:** **Long context** is simpler mentally but can **blow token budgets** and dilute attention across noise—cost scales ~linearly with tokens. **RAG** targets relevant chunks with **citations** and fresher updates via index pipelines but adds **retrieval failure modes** (wrong chunk, stale index). Hybrid: **retrieve** top-k, then **compress** into a bounded digest for the model.

---

## 22. What is a shadow deployment for prompts, and what signal validates it?

**Answer:** **Shadow** runs a candidate prompt/model on duplicate traffic **without** affecting users, logging **diffs** in decisions, tool choices, or judge scores. Validate with **offline parity** on golden sets first, then shadow on **5–10%** live traffic comparing **task proxies** (escalation rate, tool errors). Promote when **no regression** on safety and cost SLIs.

---

## Workflow & Human-in-the-loop

## 23. Sketch a state machine for “refund request” that keeps compliance invariants.

**Answer:** States like `Intake → VerifyIdentity → FetchOrder → PolicyCheck → (AutoDeny|ProposeRefund) → (HumanApprove|AutoApproveUnderLimit) → ExecuteRefund → ConfirmCustomer`. **Invariant:** no `ExecuteRefund` without `PolicyCheck` passed; **human** only on edges exceeding thresholds. Persist **state** in your store, not only chat history, so retries are safe.

---

## 24. How do you prevent the model from skipping “ask human” after a tool failure?

**Answer:** Encode **transitions in code**: on `tool_error` class `TRANSIENT`, retry with backoff; on `PERMANENT`, route to **human** or **deny** path regardless of model preference. The model proposes; the **orchestrator** enforces **legal transitions**. Log when model text disagrees with orchestrator action—useful for prompt fixes.

---

## 25. What operational metrics indicate human-in-the-loop design is mis-sized?

**Answer:** **Rising approval queue age**, **growing % of sessions escalated**, or **flat automation** despite product growth—signals thresholds are too conservative or tools too weak. Conversely, **spike in reversals/chargebacks** suggests automation too aggressive. Track **approval SLA** like any dependency with an error budget.

---

## 26. How should agents handle user corrections (“actually my order ID is …”)?

**Answer:** Treat corrections as **authoritative user input**, re-validate against backend, and **update structured session state**—not only append to transcript. **Audit** the correction event. Avoid blindly re-running dangerous tools; **re-enter** the workflow from the appropriate node (often `FetchOrder`).

---

## 27. Describe “plan-then-execute” vs interleaved tool loops for operator assistants.

**Answer:** **Plan-then-execute** asks the model to emit a **checklisted plan** for human approval, then runs tools deterministically—good for **maintenance windows** with blast radius. **Interleaved** adapts faster to surprises but is harder to cap. Staff choice often mixes: **plan** for mutators, **interleave** for read-only investigation.

---

## 28. What is the role of a runbook when a provider has a partial outage mid-workflow?

**Answer:** Runbooks specify **degraded modes**: switch to **backup model**, disable specific tools, force **human handoff**, or pause **automations**. Include **customer messaging** templates and **replay** policy for queued tool intents. Agents amplify outage visibility—SLOs should include **provider health** as a dependency ([Chapter 24: Reliability Engineering](../24-reliability-engineering/README.md)).

---

## Coding

## 29. How do you generate idempotency keys for model-initiated tool calls?

**Answer:** Keys should be **deterministic per logical intent** from stable inputs (`tenantId`, `toolName`, canonical args, `workflowRunId`, `stepId`) so **retries** do not double-charge. Avoid trusting model-supplied random UUIDs without binding to server-side state. Persist keys in a **dedup store** for mutators with side effects.

---

## 30. What should a tool JSON Schema include beyond field types?

**Answer:** **Constraints**: `enum` for reason codes, `pattern`/`maxLength` for IDs, `minimum`/`maximum` for amounts, required fields, and **examples** for doc generation. Consider **additionalProperties: false** to block injection of unexpected fields. Pair schema validation with **business rules** in code for cross-field constraints schemas express poorly.

---

## 31. Write pseudocode for validating a tool call before execution.

**Answer:** `parse json → if unknown tool reject → load tool schema → validate with jsonschema → authorize principal against tool RBAC → check rate limits → if mutating ensure idempotency key present → enqueue or execute with deadline`. Return structured `tool_result` errors to the model **without** leaking internal stack traces.

---

## 32. How do you unit test an agent orchestrator without calling a live LLM?

**Answer:** **Fixture transcripts**: recorded model outputs (including tool_calls) drive the orchestrator; assert **state transitions**, **adapter invocations**, and **error handling**. Use **contract tests** on schemas and **golden files** for normalization. Reserve live model tests for **nightly** eval suites due to cost and flakiness.

---

## 33. What is a safe pattern for returning tool errors back to the model?

**Answer:** Map internal failures to **small enumerated error codes** (`RATE_LIMITED`, `NOT_FOUND`, `POLICY_DENY`) plus **minimal** context (“order not found for tenant”). Avoid echoing secrets, stack traces, or full SQL. This lets the model **recover** or escalate without widening the leak blast radius.

---

## 34. How should timeouts be chosen for external tools invoked by agents?

**Answer:** Base timeouts on **user-facing SLA** fractions: if p95 UX target is 8 s end-to-end, individual tools might get **1–3 s** with parallelization plan. Use **hedged requests** sparingly—they double load. Always return **ambiguous timeout** outcomes to orchestration, not success—**reconcile** with provider idempotent APIs.

---

## 35. What is “tool argument normalization,” and why does it matter?

**Answer:** Models emit **syntactic variants** (`"100.00"` vs `100`, extra whitespace). **Normalization** casts types, rounds currency with banker's rules, and canonicalizes enums before hashing/idempotency. Without it, **logically identical** intents generate different keys and **duplicate** operations.

---

## 36. How do you prevent SSRF when tools fetch URLs suggested by the model?

**Answer:** **Deny by default**: no arbitrary URLs. If fetches are required, use **allowlisted domains**, **strip credentials**, resolve DNS with **pinning**, block **metadata endpoints**, and run fetchers in **sandboxed** network namespaces. The model is an **untrusted planner** for network targets.

---

## 37. Describe feature flags for prompts and tools at deploy time.

**Answer:** Store **prompt version** and **tool manifest version** in config service; flags enable **percentage rollouts** and instant **rollback** without binary redeploy. Ensure **telemetry** tags include versions to correlate regressions. Pair with **immutable** artifacts for audit (“what exactly shipped at 14:32 UTC”).

---

## 38. What logging is appropriate for LLM requests in regulated environments?

**Answer:** Log **metadata** (tenant, user id hash, model id, token counts, latency, tool names, outcomes) by default; **redact** or **encrypt** prompts/responses containing PII. Provide **break-glass** access with justification for support. Align with **retention** policies and legal hold—agents make logging volume explode without sampling design.

---

## System Design

## 39. Design a high-level architecture for an internal “IT helpdesk” agent that can reset passwords and file tickets.

**Answer:** Client → **API gateway** with auth → **orchestrator service** (state machine) → **LLM router** → model provider; **tool adapters** call **IdP** (password reset with strict policies), **ticketing API**, and **knowledge search** (RAG). **Secrets** in vault; **reset** behind **step-up MFA** and **rate limits**; **no direct** model-to-IdP path. **Events** to SIEM. Expect **~10–50 QPS** internally; **p95** dominated by model + IdP; **SLO** on ticket correctness over pure latency.

---

## 40. How would you shard responsibility between a “triage” small model and a “executor” large model?

**Answer:** **Triage** classifies intent, extracts **slots**, and decides if **tools** are needed—cheap, fast. **Executor** handles complex reasoning with **richer** context. If triage mislabels, add **confidence thresholds** and **fallback** to executor or human. Monitor **triage precision** and **cost savings** vs **error injection rate**.

---

## 41. Outline data flows for an agent that reads customer PII from CRM.

**Answer:** Authenticated user session → orchestrator attaches **scoped token** with **least privilege** to CRM adapter → **field-level** filtering removes unnecessary PII before model context → **log redaction** on traces. Optional **tokenized** references instead of raw fields. **Cache** embeddings of docs only if policy allows; prefer **ephemeral** retrieval.

---

## 42. How do you scale an agent platform to many internal teams safely?

**Answer:** Provide **golden paths**: shared **orchestrator library**, **tool registry** with review, **central eval harness**, **quota/rate limits**, and **policy templates**. Teams register tools via **PR review** with security checklist. **Isolate** prompts per team but **standardize** observability and **incident** routing—prevents 20 snowflake agents with no shared controls.

---

## 43. What capacity signals matter for an agent serving 1k concurrent sessions?

**Answer:** **GPU/TPM quotas** from provider, **orchestrator CPU** for validation/serialization, **retrieval QPS** and index hot shards, **tool dependency** rate limits (CRM, payments), and **egress** costs. Watch **queue depth** on orchestrator worker pools—LLM latency backs up the whole fleet. Plan **10×** with **degradation modes**, not linear scale assumptions.

---

## 44. How would you add multilingual support without duplicating every tool?

**Answer:** Keep **tools language-agnostic** (IDs, amounts); localize **user-facing** templates in the presentation layer. Prompts may include **language instructions**, but **grounding sources** must exist per locale or **translate** with human-reviewed glossaries for regulated terms. Evaluate **per-locale** golden sets—BLEU is insufficient; measure **task success**.

---

## 45. Describe an architecture for offline batch processing of tickets with an agent.

**Answer:** **Queue** of ticket IDs → **worker** pulls context from stores → orchestrator runs **bounded** workflow → writes **structured outcomes** to DB and **links** evidence. **Idempotent** consumers; **dead-letter** for poison tickets. **Rate limit** model calls to respect quotas; **checkpoint** per ticket for resume. Humans **sample** audits on a percentage.

---

## 46. How do you integrate A/B tests ethically for customer-facing agents?

**Answer:** **Disclose** when required by law/policy; avoid **dark** behavioral experiments on vulnerable flows (medical, financial) without oversight. Use **power analysis** for metrics (task success, harm rates), **pre-register** stop rules, and **auto-rollback** on guardrail breaches. Prefer **internal** or **opt-in** cohorts first.

---

## Debugging & Ops

## 47. Users report “the agent became rude overnight.” What do you check first?

**Answer:** **Prompt/version drift** and **model version** changes, **retrieval corpus** updates (poisoned docs), **temperature** or **sampling** config flips, and **jailbreak** attempts altering few-shot examples in dynamic prompts. Compare **traces** tagged by release id; replay **golden** conversations offline against prior artifact.

---

## 48. Tool error rate spiked but LLM latency is flat. What categories of root cause do you investigate?

**Answer:** **Downstream dependency** degradation (CRM 503s), **auth token** expiry misconfiguration, **schema mismatch** after deploy, **quota** throttling, **network partitions**, or **data-driven** validation failures (new SKU format). Break down errors by **tool name** and **error code**—aggregate latency hides adapter failures.

---

## 49. What is a “compensating action” pattern when a tool succeeds after the user aborted?

**Answer:** Users can cancel while a **refund** is in flight; orchestrator must **reconcile** with payments API using **idempotent** queries, **void** if still pending, or **confirm** if completed and **notify** honestly. Treat as distributed transaction UX: **status** endpoint and **clear** messaging beat silent double execution.

---

## Staff+

## 50. How do you run an incident retrospective for an agent that issued wrong financial advice?

**Answer:** Timeline: **model/tool versions**, **prompt hash**, **retrieval documents** with hashes, **human overrides**, and **customer impact** quantified. Identify **missing guardrail** vs **bad retrieval** vs **policy ambiguity**. Actions: **eval additions**, **graph constraint**, **tool narrowing**, **communications** to affected users, and **governance** update (ADR). Blameless focus on **system holes**, not the model as moral agent.

---
