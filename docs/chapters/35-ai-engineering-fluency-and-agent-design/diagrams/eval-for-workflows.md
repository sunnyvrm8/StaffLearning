# Evaluation for Agent Workflows — Offline to Online Loop

**Supports decision:** What to measure before shipping prompt or tool changes, and how to catch regressions on multi-step flows—not only single-turn accuracy.

```mermaid
flowchart LR
  subgraph offline [Offline Eval]
    gold[Golden Traces or Tasks]
    synth[Synthetic Adversarial Cases]
    rubric[Rubric or LLM-as-Judge with Human Spot Check]
    metrics[Task Success Tool Correctness Cost Latency]
  end

  subgraph ci [CI Gate]
    diff[Compare to Baseline Branch]
    block[Block Merge on Regression]
  end

  subgraph online [Online Signals]
    shadow[Shadow or Canary Traffic]
    user[User Corrections and Thumbs]
    sli[SLO Latency Error Rate Spend per Task]
  end

  subgraph act [Action]
    adr[ADR or Change Log for Prompt and Tools]
    rollback[Feature Flag Rollback]
  end

  gold --> metrics
  synth --> metrics
  rubric --> metrics
  metrics --> diff
  diff --> block
  shadow --> sli
  user --> sli
  sli --> rollback
  metrics --> adr
```

**Caption:** **Golden workflows** anchor regression tests; **online** SLOs catch drift, abuse, and model provider changes. Tie releases to **flags** so prompt v2 can roll back without redeploying the whole service ([Chapter 34](../../34-agentic-systems-and-mlops-for-ai/README.md) MLOps context).
