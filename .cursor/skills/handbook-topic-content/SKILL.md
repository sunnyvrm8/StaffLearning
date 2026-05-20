---
name: handbook-topic-content
description: >-
  Generates Staff Learning handbook topic pages with production-oriented prose,
  Mermaid/ASCII diagrams, Java+Go snippets, and sized interview question banks
  (10/25/50/100). Use when the user asks to create, draft, or expand handbook
  content, a chapter/topic page, learning material for Plan.md topics, topic
  diagrams, code examples, or interview questions for a subject.
---

# Handbook Topic Content

Create **durable handbook artifacts** for topics in `Plan.md` (chapters 1–30, case studies, leadership, interview prep). Align with workspace rules: `staff-learning-core.mdc`, `java-golang-examples.mdc`, `interview-preparation.mdc`, `system-design-case-studies.mdc` when applicable.

## When to Apply

| User intent | Action |
|-------------|--------|
| "Create content for chapter X / topic Y" | Full deliverable set (see below) |
| "Add interview questions only" | `interview-questions.md` + update README link |
| "Add diagram / snippet for …" | Patch existing topic folder |
| "Expand to 50 questions" | Regenerate bank using [interview-bank-rubric.md](interview-bank-rubric.md) |

**Before writing**, read `Plan.md` to resolve chapter number, title, and related cross-links.

## Deliverables Checklist

Copy and track in the response:

```
Topic: <name> (Chapter <n> or Case Study <n>)
- [ ] Folder created under chapters/ or case-studies/ or leadership/
- [ ] README.md (main topic page)
- [ ] diagrams/*.md (1–4 diagrams, as warranted)
- [ ] Code snippets (java/ + go/ when code applies)
- [ ] interview-questions.md (sized per rubric)
- [ ] Cross-links to related handbook topics
```

## Workflow

### Step 1 — Scope the topic

1. Map to **Plan.md** entry (handbook chapter, case study, leadership area, or interview track).
2. Classify **topic type** (drives diagrams and code):

   | Type | Examples | Code | Primary diagrams |
   |------|----------|------|------------------|
   | `algorithmic` | DS&A, performance | Yes, dual | flow, complexity |
   | `language-runtime` | JVM, Go scheduler | Often single-lang | memory/thread model |
   | `patterns-principles` | SOLID, patterns | Yes, dual | class/sequence |
   | `concurrency` | threads, channels | Yes, dual | sequence, state |
   | `infrastructure` | K8s, Docker, AWS | Config/snippet | deployment, network |
   | `data` | DB, indexing, cache | Yes, dual | ER, read/write path |
   | `distributed` | CAP, consistency, Kafka | Yes + pseudocode | topology, timeline |
   | `system-design` | case studies | Selective dual | C4-style, sequence |
   | `security-ops` | auth, observability | Yes | trust boundary, pipeline |
   | `ai-ml` | RAG, LLM systems | Yes | pipeline, retrieval |
   | `leadership` | influence, hiring | No / rare | none or decision tree |
   | `interview-meta` | mock strategy | No | prep flow |

3. Pick **interview bank size** using [interview-bank-rubric.md](interview-bank-rubric.md). State the choice in one line: *"Using N questions because …"*

4. If the user specified a count (10/25/50/100), honor it unless clearly wrong for a tiny subtopic—then recommend the rubric size and proceed with user preference if they insist.

### Step 2 — Create folder layout

Default paths (kebab-case slug from topic title):

```text
chapters/NN-slug/           # NN = zero-padded chapter from Plan.md
  README.md
  diagrams/
    overview.md             # embeddable Mermaid blocks
  java/...                  # when type warrants code
  go/.../
  interview-questions.md

case-studies/NN-slug/       # Case Study 1–10
leadership/slug/            # Leadership bullets in Plan.md
interview-prep/slug/        # Interview Preparation section
```

If the folder already exists, **extend** rather than duplicate; preserve existing anchors and filenames.

### Step 3 — Write README.md

Use the template in [topic-templates.md](topic-templates.md). Mandatory sections:

1. Title + one-line intuition
2. Stakeholder pain → technique mapping
3. Core content (production-anchored; comparisons over bullet dumps)
4. When to use / avoid
5. How it fails (incidents, debugging)
6. Architect takeaway (decisions, metrics, design-review bullets)
7. Diagrams (link or embed from `diagrams/`)
8. Code (link to `java/` and `go/` files)
9. Related topics (links to other chapter folders)
10. Interview prep pointer → `interview-questions.md`

**Length guidance**: handbook chapter 1,500–3,500 words unless user asks for "cheat sheet" (≤800) or "deep dive" (4,000+).

### Step 4 — Diagrams

Follow [diagram-patterns.md](diagram-patterns.md).

Rules:

- **Minimum 1** diagram for technical topics; **0–1** for pure leadership/interview-meta unless user asks.
- Prefer **Mermaid** in `diagrams/*.md`; use ASCII only for quick inline in README or terminal-friendly views.
- Every diagram gets a **caption**: what decision it supports.
- Name files by purpose: `read-path.md`, `topology.md`, `failure-modes.md`, not `diagram1.md`.

### Step 5 — Code snippets

When `topic type` includes code (see Step 1):

1. Create **matching scenario** in Java and Go per `java-golang-examples.mdc`.
2. Keep each file **≤40 lines**; extract to second file only if user requests "full example."
3. Use a **real system scenario** (idempotency, rate limit, cache-aside, worker pool)—not `FooBar` toys.
4. Add a short header comment: scenario + what to notice.
5. Paths:
   - `chapters/NN-slug/java/<Scenario>.java`
   - `chapters/NN-slug/go/<scenario>.go` (Go file naming: snake_case)

Skip dual snippets for: CAP-only theory, leadership, interview strategy, pure napkin-math case study overviews (unless implementing a mechanism).

### Step 6 — Interview question bank

Generate `interview-questions.md` using [topic-templates.md](topic-templates.md#interview-questions-template). Follow `interview-preparation.mdc` (**Handbook interview question banks**).

Requirements:

- Exact count: 10, 25, 50, or 100 per rubric/user.
- Each item: numbered question + full **Answer:** (production-anchored; trade-offs and order-of-magnitude where relevant).
- **No** difficulty or type tags (`[EASY]`, `[CONCEPT]`, etc.) and **no** Legend for tags.
- Group by **subsections** (see rubric for mix and section headings).
- For system-design case studies, include enough design-depth Q&A with capacity and failure follow-ups in the answers (no `[SYSTEM]` labels).

### Step 7 — Quality gate

Before finishing, verify:

| Check | |
|-------|---|
| Production anchor | Named real system (payments, chat, search, …) in intro |
| Trade-offs | At least 2 alternatives compared |
| Numbers | Order-of-magnitude where relevant (QPS, latency, storage) |
| Cross-links | ≥2 related Plan.md topics linked |
| Diagram sanity | Mermaid parses (balanced fences, no spaces in node IDs) |
| Code compiles | Imports/packages plausible; no pseudo-syntax |
| Interview count | Matches stated N exactly |
| No filler | No generic "explain OOP" unless topic is fundamentals |

## Invocation Examples

**Full chapter**

> Create handbook content for chapter 10 Caching Strategies — full package.

→ `chapters/10-caching-strategies/` with README, 2–3 diagrams, cache-aside + stampede snippets, 50 questions per rubric.

**Case study**

> Case Study 7 Rate Limiter — content + 25 interview questions.

→ `case-studies/07-rate-limiter/`, system-design walkthrough template, dual snippets, 25 Q&A pairs (~40% system-design depth per rubric).

**Questions only**

> Add 100 interview questions for Distributed Systems chapter.

→ Extend or create `interview-questions.md` only; link from README.

## Partial / Incremental Requests

| Request | Deliver |
|---------|---------|
| "Outline only" | README headings + diagram list + question categories (no full prose) |
| "Diagrams only" | `diagrams/` files + links |
| "Cheatsheet" | Condensed README ≤800 words + 10 questions |
| "Interview expansion 25→50" | Add 25 new Q&A pairs; dedupe; re-balance section mix per rubric |

## Anti-Patterns (reject or rewrite)

- Textbook definitions without production pain
- Kafka/K8s as default answer without requirements
- 100 questions for a narrow subtopic (e.g., "what is a binary tree")
- Duplicate questions rephrased
- Diagrams with no caption or decision tie-in
- Java-only when dual-language rule applies

## Reference Files

- [topic-templates.md](topic-templates.md) — README and interview file templates
- [interview-bank-rubric.md](interview-bank-rubric.md) — 10/25/50/100 sizing and category quotas
- [diagram-patterns.md](diagram-patterns.md) — diagram types and Mermaid starters

## After Delivery

Offer (once, briefly): mock drill on 3 questions from the bank, or a canvas-style architecture view for case studies—only if useful for the topic type.
