# Topic Templates

## README.md Template

```markdown
# <Chapter NN>: <Title>

> **One line:** <intuition in plain language>

## Why this matters in production

<2–3 sentences: stakeholder pain — latency, cost, consistency, operability — tied to a named system>

## Core ideas

<Prose with comparisons. Use ### subheadings for major concepts.>

### <Concept A> vs <Concept B>

| | A | B |
|---|---|---|
| When | | |
| Risk | | |
| Ops signal | | |

## When to use / when to avoid

**Use when:** …

**Avoid when:** …

## How it fails

<Incident patterns, symptoms, what to check in metrics/logs/traces>

## Architect takeaway

- **Decide:** …
- **Measure:** …
- **Document in design review:** …

## Diagrams

- [Overview](./diagrams/overview.md)
- [<Other>](./diagrams/<file>.md)

## Code examples

| Scenario | Java | Go |
|----------|------|-----|
| <name> | [java/...](./java/<File>.java) | [go/...](./go/<file>.go) |

**Production note:** <when you'd ship this pattern>

## Related topics

- [Chapter XX: <Title>](../XX-slug/README.md) — <why linked>
- …

## Interview preparation

See [interview-questions.md](./interview-questions.md) (<N> questions, <size rationale>).
```

---

## interview-questions.md Template

```markdown
# Interview Questions: <Topic Title>

**Bank size:** <10 | 25 | 50 | 100>  
**Rationale:** <one line from rubric>  
**Last updated:** <YYYY-MM-DD>

---

## <Section per rubric — e.g. Foundations>

## 1. <Question text — specific, production-anchored>

**Answer:** <Full answer: mechanism, trade-off, failure mode, or numbers as appropriate.>

## 2. …

---

## <Next section>

## N. …

**Answer:** …

---

## Rapid Fire (100-bank only)

Each rapid-fire item still includes **Answer:** (can be shorter, 2–4 sentences).
```

---

## diagrams/overview.md Template

```markdown
# <Topic> — Architecture Overview

**Supports decision:** <one sentence>

```mermaid
flowchart TB
  %% ...
```
```

---

## Java File Header Template

```java
// Scenario: <e.g., per-user rate limiter for checkout API>
// Demonstrates: <mechanism>
// Trade-off: <one line vs alternative>
```

---

## Go File Header Template

```go
// Scenario: <same as Java>
// Demonstrates: <mechanism>
// Trade-off: <one line>
```

---

## Case Study README Addendum

Append to README for `case-studies/*`:

```markdown
## Problem framing

- **Users:** …
- **Peak load:** ~<N> QPS (estimate)
- **Critical path:** …

## API sketch

| Method | Path / Event | Notes |
|--------|--------------|-------|

## Data model

<entities, keys, retention>

## Reliability

<idempotency, retries, DLQ>

## If I had two more weeks

<MVP vs v2>

## Three scale triggers

1. <trigger> → <design change>
2. …
3. …
```

---

## Cheatsheet Variant (≤800 words)

Replace "Core ideas" with:

- **5 must-know bullets**
- **3 anti-patterns**
- **1 diagram**
- **10 questions** only (link file)

---

## Legacy banks

When extending an older bank that used difficulty/type tags or hints-only, rewrite touched items to Q&A format and remove tag prefixes when editing that file.
