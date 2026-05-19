# Principal Engineer Thinking Framework

> **One line:** A principal engineer applies the same lens everywhere—**clarify outcomes, quantify trade-offs, own the blast radius, and leave a decision others can execute.**

This track is a **lens** for case studies and governance chapters in [`Plan.md`](../../Plan.md#principal-engineer-thinking-framework), not a separate read order. Use it when reviewing designs, running mocks, or preparing for principal loops.

## Framework bullets (from Plan.md)

| Topic | What “good” looks like in production |
|-------|-------------------------------------|
| Think like a principal | Depth on existential risk; breadth on repeated failure classes; explicit time horizons |
| System design answers | Requirements + order-of-magnitude numbers before boxes; ops and failure before scale |
| Lead architecture discussions | Pre-read, criteria, options, ADR—not slide theater |
| Influence without authority | Pilots, metrics, alliances, phased mandates |
| Take ownership | End-to-end customer outcome; escalate on $/risk, not ego |
| Run architecture reviews | NFRs, reversibility, operability checklist, rejected options documented |
| Assess and communicate risk | Likelihood × impact in business terms; decision asks for execs |
| Negotiate debt vs features | Risk register with “interest”; capacity and thin paths |

## When to use / avoid

**Use when:** Cross-team contracts, one-way doors, exec-visible risk, or interview loops testing **judgment** more than syntax.

**Avoid when:** The team needs a local two-way-door decision this week—don’t run a principal-sized process on a feature flag.

## Architect takeaway

- **Decide:** What evidence is required before commit; who owns the outcome if the org chart is fuzzy.
- **Measure:** SLOs, incident recurrence, predictability, cost of delay—not activity.
- **Document:** ADR with options rejected; risk register with dates; rollout/rollback.

## Interview preparation

See [interview-questions.md](./interview-questions.md) — **Top 10** with full real-world answers covering all framework bullets.

## Related topics

- [Leadership](../leadership/README.md) — influence, debt, incidents, hiring
- [Interview Preparation (Plan.md)](../../Plan.md#interview-preparation) — Staff vs Principal loop focus
- [System design case studies](../../Plan.md#system-design-case-studies) — apply this lens while practicing tiers
