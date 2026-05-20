# Interview Bank Sizing Rubric

Choose **one** bank size per topic. State the rationale in the agent response.

**Output format:** Every item is question + **Answer:**. Do **not** label questions with difficulty or type tags in the file—use the mix tables below only when **planning** variety.

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

Percentages are **minimum floors** for planning; adjust ±10% for topic fit. Cover the mix through question wording and answer depth—never with visible tags.

### 10 questions

| Kind | Min count |
|------|-----------|
| Conceptual / definitions | 3 |
| Trade-offs / alternatives | 2 |
| Coding or system design | 3 (pick dominant for topic) |
| Debugging or behavioral | 2 |

Depth spread: include easier definitional items, applied scenarios, and at least one staff-level design or incident question.

### 25 questions

| Kind | Min count |
|------|-----------|
| Conceptual | 6 |
| Trade-offs | 5 |
| Coding | 5 (if algorithmic/language chapter) |
| System design | 5 (if infra/distributed/case study) |
| Debugging | 2 |
| Behavioral | 2 (leadership chapters: 8+) |

### 50 questions

| Kind | Min count |
|------|-----------|
| Conceptual | 12 |
| Trade-offs | 10 |
| Coding | 10 |
| System design | 10 |
| Debugging | 4 |
| Behavioral | 4 |

Include principal-scope items (cross-team, build-vs-buy, multi-year) where the topic warrants it—without labeling them.

### 100 questions

| Kind | Min count |
|------|-----------|
| Conceptual | 22 |
| Trade-offs | 20 |
| Coding | 20 |
| System design | 22 |
| Debugging | 8 |
| Behavioral | 8 |

Include several principal-scope and rapid-fire items in dedicated sections.

## Topic-Specific Weighting

Apply **after** picking bank size; rebalance within ±10%.

| Topic family | Emphasize in the bank | De-emphasize |
|--------------|----------------------|--------------|
| DS&A (ch. 1) | coding, complexity trade-offs | behavioral |
| Java/Go (ch. 2) | concepts, debugging | system design |
| SOLID/Patterns (3–4) | trade-offs, coding | system design |
| Concurrency (5) | coding, debugging | behavioral |
| DB/Index/Cache (8–10) | system design, trade-offs | — |
| Microservices/API/Events (11–14) | system design, trade-offs | — |
| Distributed/CAP/Consistency (15–17) | system design, trade-offs, concepts | coding unless algorithms cited |
| K8s/Docker/AWS (19–21) | system design, debugging | coding |
| Observability/Security (22–23) | debugging, system design | — |
| Case studies (24 + studies) | system design 40%+, trade-offs | behavioral unless auth/payments |
| AI/RAG (25–26) | system design, trade-offs | — |
| Performance/Scale (27–28) | system design, debugging | — |
| Leadership (29) | behavioral, trade-offs | coding |
| Mock interviews (30) | behavioral, system-design meta | coding |

## Question Quality Rules

1. **Specific** — "How does cache-aside handle stampede on hot keys?" not "What is caching?"
2. **Layered** — same theme from definition → apply → design under failure, without difficulty labels.
3. **No duplicates** — if two questions differ only by wording, merge.
4. **Staff+ bar** — answers name trade-offs, order-of-magnitude, or incident reasoning where relevant.
5. **Principal** — answers may cover cross-team, multi-year, build-vs-buy, org-scale when the question asks for it.

## Answer quality

- **Answer** body is complete enough to study or mock from—not a one-line hint.
- Prefer 1 short paragraph; 2–3 for system design or behavioral items.
- Name X vs Y, give order-of-magnitude when relevant, state a failure mode or metric when the question is design- or ops-oriented.

## File Section Headings (by size)

**10**: `## Core`, `## Stretch`

**25**: `## Foundations`, `## Application`, `## Design & Trade-offs`, `## Stretch`

**50**: Add `## Coding`, `## System Design`, `## Debugging & Ops`, `## Staff+`

**100**: Add `## Principal`, `## Rapid Fire` (20 Q&A pairs), `## Scenario Drills` (5 multi-part, each part with **Answer:**)
