# Interview Bank Sizing Rubric

Choose **one** bank size per topic. State the rationale in the agent response.

## Size Decision Matrix

| Size | When to use | Plan.md examples |
|------|-------------|------------------|
| **10** | Narrow subtopic, leadership single theme, cheatsheet, follow-up drill | "Conflict Resolution", "Mentorship", JVM flag deep-dive |
| **25** | Medium chapter, case study supplement, leadership area bundle | Docker/CI/CD, Monitoring, Indexing, single Case Study (7–9) |
| **50** | Core handbook chapter, multi-concept | Concurrency, API Design, Caching, Kafka, Security, Networking |
| **100** | Flagship interview domains, case study + chapter combo, index topics | DS&A, Distributed Systems, Microservices, System Design (meta), Mock Interviews, Payment/Chat/Search case studies |

### Overrides

- User explicitly requests a count → use it.
- User says "staff bar" on a leadership topic → cap at **25** unless they want behavioral volume.
- User says "principal / architect loop" on distributed/system design → prefer **50–100**.

## Category Mix (by size)

Percentages are **minimum floors**; adjust ±10% for topic fit.

### 10 questions

| Dimension | Min count |
|-----------|-----------|
| `[CONCEPT]` | 3 |
| `[TRADEOFF]` | 2 |
| `[CODING]` or `[SYSTEM]` | 3 (pick dominant for topic) |
| `[DEBUG]` or `[BEHAVIORAL]` | 2 |

Difficulty: 3 EASY, 4 MEDIUM, 2 HARD, 1 STAFF+.

### 25 questions

| Dimension | Min count |
|-----------|-----------|
| `[CONCEPT]` | 6 |
| `[TRADEOFF]` | 5 |
| `[CODING]` | 5 (if algorithmic/language chapter) |
| `[SYSTEM]` | 5 (if infra/distributed/case study) |
| `[DEBUG]` | 2 |
| `[BEHAVIORAL]` | 2 (leadership chapters: 8+) |

Difficulty: 6 EASY, 10 MEDIUM, 6 HARD, 3 STAFF+.

### 50 questions

| Dimension | Min count |
|-----------|-----------|
| `[CONCEPT]` | 12 |
| `[TRADEOFF]` | 10 |
| `[CODING]` | 10 |
| `[SYSTEM]` | 10 |
| `[DEBUG]` | 4 |
| `[BEHAVIORAL]` | 4 |

Difficulty: 10 EASY, 20 MEDIUM, 12 HARD, 8 STAFF+ (include 2 PRINCIPAL for chapters 15, 24, 28–30).

### 100 questions

| Dimension | Min count |
|-----------|-----------|
| `[CONCEPT]` | 22 |
| `[TRADEOFF]` | 20 |
| `[CODING]` | 20 |
| `[SYSTEM]` | 22 |
| `[DEBUG]` | 8 |
| `[BEHAVIORAL]` | 8 |

Difficulty: 18 EASY, 38 MEDIUM, 28 HARD, 16 STAFF+ (include 6 PRINCIPAL).

## Topic-Specific Weighting

Apply **after** picking bank size; rebalance within ±10%.

| Topic family | Boost dimensions | De-emphasize |
|--------------|------------------|--------------|
| DS&A (ch. 1) | `[CODING]`, `[TRADEOFF]` | `[BEHAVIORAL]` |
| Java/Go (ch. 2) | `[CONCEPT]`, `[DEBUG]` | `[SYSTEM]` |
| SOLID/Patterns (3–4) | `[TRADEOFF]`, `[CODING]` | `[SYSTEM]` |
| Concurrency (5) | `[CODING]`, `[DEBUG]` | `[BEHAVIORAL]` |
| DB/Index/Cache (8–10) | `[SYSTEM]`, `[TRADEOFF]` | — |
| Microservices/API/Events (11–14) | `[SYSTEM]`, `[TRADEOFF]` | — |
| Distributed/CAP/Consistency (15–17) | `[SYSTEM]`, `[TRADEOFF]`, `[CONCEPT]` | `[CODING]` unless algorithms cited |
| K8s/Docker/AWS (19–21) | `[SYSTEM]`, `[DEBUG]` | `[CODING]` |
| Observability/Security (22–23) | `[DEBUG]`, `[SYSTEM]` | — |
| Case studies (24 + studies) | `[SYSTEM]` 40%+, `[TRADEOFF]` | `[BEHAVIORAL]` unless auth/payments |
| AI/RAG (25–26) | `[SYSTEM]`, `[TRADEOFF]` | — |
| Performance/Scale (27–28) | `[SYSTEM]`, `[DEBUG]` | — |
| Leadership (29) | `[BEHAVIORAL]`, `[TRADEOFF]` | `[CODING]` |
| Mock interviews (30) | `[BEHAVIORAL]`, `[SYSTEM]` meta | `[CODING]` |

## Question Quality Rules

1. **Specific** — "How does cache-aside handle stampede on hot keys?" not "What is caching?"
2. **Layered** — same theme at EASY (define), MEDIUM (apply), HARD (design under failure).
3. **No duplicates** — if two questions differ only by wording, merge.
4. **Staff+ bar** — requires trade-off, numbers, or incident reasoning.
5. **Principal** — cross-team, multi-year, build-vs-buy, org-scale.

## Strong-Answer Hints (top 15% per bank)

Format under the question:

```markdown
<details>
<summary>Hint</summary>

Strong answer names X vs Y, gives order-of-magnitude, and states failure mode Z.
</details>
```

Keep hints ≤3 sentences.

## File Section Headings (by size)

**10**: `## Core`, `## Stretch`

**25**: `## Foundations`, `## Application`, `## Design & Trade-offs`, `## Stretch`

**50**: Add `## Coding`, `## System Design`, `## Debugging & Ops`, `## Staff+`

**100**: Add `## Principal`, `## Rapid Fire` (20 one-liners), `## Scenario Drills` (5 multi-part)
