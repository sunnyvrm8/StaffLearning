# AI Engineering Fluency — Agent Control Plane Overview

**Supports decision:** Where to place guardrails, state, and human approval so agent workflows stay observable, reversible, and within latency/cost budgets.

```mermaid
flowchart TB
  subgraph clients [Clients]
    ui[Web or API Client]
  end

  subgraph control [Control Plane]
    orch[Orchestrator or Workflow Engine]
    pol[Policy and RBAC]
    appr[Human Approval Queue]
    audit[Audit Log and Traces]
  end

  subgraph model [Model Plane]
    router[Router or Gateway]
    llm[LLM Provider]
  end

  subgraph tools [Tool Plane]
    treg[Tool Registry with Schemas]
    sand[Sandboxed Adapters]
    ext[External APIs Payments CRM Search]
  end

  subgraph data [Data Plane]
    vec[(Vector Store Optional)]
    kv[(Session State Idempotency)]
    bus[[Events for Replay]]
  end

  ui --> orch
  orch --> pol
  pol -->|allow or deny| router
  router --> llm
  llm -->|proposed tool calls| treg
  treg --> sand
  sand --> ext
  orch --> appr
  appr -->|resume or reject| orch
  orch --> audit
  orch --> kv
  sand --> bus
  orch --> vec
```

**Caption:** The **orchestrator** owns step boundaries, budgets, and retries; the **LLM** proposes actions; **tools** execute only through validated, policy-checked adapters. Human approval sits on **high-blast-radius** branches (refunds, mass email, production config).
