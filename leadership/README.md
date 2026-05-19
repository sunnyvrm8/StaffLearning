# Leadership (Staff / Principal)

> **One line:** Leadership at this level is turning ambiguity into **durable decisions** other teams can execute—without owning every line of code.

## Why this matters in production

Engineering pain at Staff+ scale is rarely “we don’t know how to shard.” It is **misaligned bets** (three teams rebuild caching differently), **silent risk** (debt nobody can name in exec reviews), and **coordination tax** (incidents without a commander, RFCs nobody reads). Stakeholders feel it as missed quarters, audit findings, and attrition on strong seniors—not as “weak soft skills.”

The topics in [`Plan.md`](../Plan.md#leadership) are the operating system for **technical strategy**, **influence**, and **accountability** when you no longer fit in one squad’s backlog.

## Core ideas (by theme)

### Technical leadership and strategy

**Intuition:** Strategy is a **small set of bets** with explicit trade-offs, not a slide deck of buzzwords.

**Production anchor:** A platform team chooses “standardize on event-driven integration” vs “synchronous gRPC mesh.” The wrong abstract strategy forces every product team to fight the same operational fires (duplicate outboxes, incompatible schemas). Good strategy names **default patterns**, **escape hatches**, and **what we will not do** this year.

| | Strong strategy | Weak strategy |
|---|---|---|
| When | Multi-team surface area, repeated incidents | Single team, clear domain |
| Risk | Analysis paralysis | Tool-of-the-week |
| Ops signal | Fewer one-off integration styles per quarter | Every service invents its own queue |

### Roadmapping: outcomes vs output

**Intuition:** Roadmaps track **measurable outcomes** (checkout success rate, cost per order, mean time to restore); backlogs track **output** (tickets closed, microservices count).

**Avoid:** “Ship Kafka” as a goal without a reliability or decoupling outcome tied to revenue or risk.

### Mentorship and growing seniors

**Intuition:** Growing seniors means **expanding scope of judgment**, not delegating your TODO list.

**Failure mode:** Staff engineers who are force-multipliers on code but never run stakeholder meetings—then get stuck at “super-senior IC.”

### Stakeholder management

**Intuition:** Each stakeholder class optimizes a different utility: **product** (time to market), **exec** (risk and narrative), **legal/security** (non-negotiables), **finance** (unit economics).

**Architect takeaway:** Bring **options with costs**, not binary “we can’t.”

### Culture and psychological safety

**Intuition:** Safety is not comfort—it is **low cost to surface bad news early** (estimate was wrong, design is brittle, vendor is failing).

**Incident tie-in:** Blameless postmortems fail when managers still punish the messenger; safety erodes and outages repeat.

### Conflict and crucial conversations

**Intuition:** Most “technical disagreements” are **priority or risk tolerance** disagreements wearing architecture cosplay.

**When to escalate:** After structured debate and a time-box; not as the first move.

### Decision making: one-way vs two-way doors

**Intuition:** **Two-way doors**—cheap to reverse (feature flag, schema additive change)—decide fast with a DRI. **One-way doors**—data model cutover, public API, vendor lock-in—need broader review and rollback design.

See [decision doors](./diagrams/decision-doors.md).

### Execution and delivery accountability

**Intuition:** Accountability is **transparent commitments** (what, by when, with what risk), not heroics.

**Metric examples:** Predictability (% initiatives landed with stated scope), escaped defects, incident count—not lines of code.

### Hiring, bar raising, loops, debriefs

**Intuition:** The loop tests **judgment under ambiguity** at the level of the role; debriefs test whether the panel calibrated, not whether everyone liked the candidate.

### Cross-team alignment and working groups

**Intuition:** Working groups exist to **end**—either they produce a decision/RFC or they become permanent meeting tax.

### RFCs and written communication

**Intuition:** RFCs force **options, trade-offs, and decision** before sunk cost; async readers scale better than oral tradition.

### Incident command, postmortems, blameless learning

**Intuition:** IC structure separates **coordination** (commander) from **technical depth** (subject matter experts)—so nobody argues and fixes at the same time.

### Build vs buy and vendor evaluation

**Intuition:** Buy when **differentiation is low** and **operational burden** of building exceeds TCO + exit risk; build when the capability is core revenue or compliance-critical.

### Technical debt and risk trade-offs

**Intuition:** Debt is a **portfolio**: label principal, interest (incident/latency cost), and repayment plan—or it is invisible until an audit or outage.

## When to use / when to avoid

**Use when:**

- Work spans teams, quarters, or external commitments (SLAs, regulators).
- The cost of being wrong is measured in incidents, rework, or attrition—not a single sprint.

**Avoid when:**

- The team needs a tech lead to **pair and unblock** this week—don’t substitute strategy theater for hands-on leadership.
- The decision is reversible and local—default to the team’s two-way-door process.

## How it fails

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Repeated cross-team incidents on the same integration style | No platform strategy / RFC | Architecture decision records, integration catalog |
| Roadmap “done” but metrics flat | Output roadmap | OKRs tied to customer or SLO metrics |
| Seniors leave after “promotion” to staff | Scope of judgment didn’t grow | Staff expectations doc, role charter |
| Postmortems repeat same root cause | Blame or no action items | Action item owners, SLO error budget policy |
| Hiring bar drifts | Weak debrief, loudest voice wins | Calibration notes, level rubric |

## Architect takeaway

- **Decide:** Which decisions are one-way doors; who is DRI; what “good enough” evidence looks like before commit.
- **Measure:** Outcome metrics on roadmaps; predictability; incident recurrence; time-to-offer and loop calibration for hiring.
- **Document in design review:** Options rejected, reversibility, operational owner, and debt repayment schedule.

## Diagrams

- [One-way vs two-way doors](./diagrams/decision-doors.md)

## Related topics

- [Principal Engineer Thinking Framework](../interview-prep/principal-engineer-thinking/README.md) — lens for architecture reviews and influence without authority ([Top 10 Q&A](../interview-prep/principal-engineer-thinking/interview-questions.md))
- [Chapter 01: SOLID](../chapters/01-solid-and-core-engineering-principles/README.md) — technical debt conversations grounded in change vectors
- [Interview Preparation](../Plan.md#interview-preparation) — Staff/Principal loops and behavioral framing

## Interview preparation

See [interview-questions.md](./interview-questions.md) (**Top 20** with full answers—real-world Staff/Principal behavioral and judgment scenarios across all Leadership themes in `Plan.md`).
