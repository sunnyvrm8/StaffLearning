# Staff Learning

A **Staff / Principal engineer learning handbook**—production-oriented notes, diagrams, dual-language examples (Java + Go), system design case studies, and interview question banks. Content is organized around the curriculum in [`Plan.md`](Plan.md).

## What this repo is for

| Area | In `Plan.md` | Typical artifacts |
|------|----------------|-------------------|
| Handbook chapters | Topics 1–30 (DS&A through mock interviews) | `chapters/NN-slug/README.md`, diagrams, code, questions |
| System design case studies | Case Study 1–10 | `case-studies/NN-slug/` |
| Leadership | Technical leadership, influence, hiring, … | `leadership/slug/` |
| Interview prep | Staff/Principal strategy, behavioral, architecture | `interview-prep/slug/` |

Topics are written for **architect-level depth**: stakeholder pain first, trade-offs, failure modes, and measurable design decisions—not textbook summaries alone.

### Published handbook paths

| Path | Status |
|------|--------|
| [`chapters/01-solid-and-core-engineering-principles/`](chapters/01-solid-and-core-engineering-principles/README.md) | Full chapter + Top 20 Q&A |
| [`chapters/02-design-patterns/`](chapters/02-design-patterns/README.md) | Full chapter + 25 interview questions |
| [`chapters/03-domain-driven-design-and-bounded-contexts/`](chapters/03-domain-driven-design-and-bounded-contexts/README.md) | Full chapter + Top 10 Q&A |
| [`chapters/04-data-structures-and-complexity/`](chapters/04-data-structures-and-complexity/README.md) | Full chapter + 50 interview questions |
| [`chapters/05-java-and-golang-deep-dive/`](chapters/05-java-and-golang-deep-dive/README.md) | Full chapter + 50 Java + 50 Go Q&A |
| [`chapters/06-concurrency-and-multithreading/`](chapters/06-concurrency-and-multithreading/README.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/07-memory-management/`](chapters/07-memory-management/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/08-networking-and-http/`](chapters/08-networking-and-http/README.md) | Full chapter + Top 10 Q&A |
| [`chapters/09-api-design/`](chapters/09-api-design/README.md) | Full chapter + 50 interview questions |
| [`chapters/10-database-design-and-data-modeling/`](chapters/10-database-design-and-data-modeling/README.md) | Full chapter + 50 interview questions |
| [`chapters/17-microservices-architecture/`](chapters/17-microservices-architecture/README.md) | Full chapter + 25 interview questions |
| [`chapters/18-event-driven-architecture/`](chapters/18-event-driven-architecture/README.md) | Full chapter + 25 interview questions |
| [`chapters/25-security-architecture/`](chapters/25-security-architecture/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/26-observability/`](chapters/26-observability/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/27-performance-engineering/`](chapters/27-performance-engineering/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/28-scalability-and-capacity-planning/`](chapters/28-scalability-and-capacity-planning/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/29-platform-engineering-and-internal-developer-platforms/`](chapters/29-platform-engineering-and-internal-developer-platforms/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/30-cost-architecture-and-finops/`](chapters/30-cost-architecture-and-finops/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/31-architecture-governance/`](chapters/31-architecture-governance/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/32-ai-and-llm-systems-in-production/`](chapters/32-ai-and-llm-systems-in-production/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/33-rag-and-retrieval-architecture/`](chapters/33-rag-and-retrieval-architecture/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`chapters/34-agentic-systems-and-mlops-for-ai/`](chapters/34-agentic-systems-and-mlops-for-ai/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`leadership/`](leadership/README.md) | Leadership bundle + Top 20 Q&A with answers |
| [`interview-prep/principal-engineer-thinking/`](interview-prep/principal-engineer-thinking/README.md) | Principal thinking lens + Top 10 Q&A with answers |

## Repository layout

```text
Plan.md                          # Master curriculum (chapters, case studies, leadership)
chapters/                        # Handbook chapters (created as content is generated)
case-studies/                    # System design case studies
leadership/                      # Leadership topics
interview-prep/                  # Interview strategy tracks
.cursor/
  rules/                         # Always-on Cursor agent guidance
  skills/handbook-topic-content/ # Skill + templates for generating topic pages
```

Handbook folders are added incrementally as you (or the agent) generate topics. **`Plan.md` is the source of truth** for chapter numbers, titles, and scope.

## Live site (GitHub Pages)

- **URL:** https://sunnyvrm8.github.io/StaffLearning/
- **Local build:** The generated site is available in the `site/` folder; open [site/index.html](site/index.html) or run `mkdocs serve` to preview locally.

## Using Cursor in this repo

Open the project in [Cursor](https://cursor.com). Agent behavior is shaped by **rules** and **skills** under `.cursor/`.

### Rules (automatic)

These apply to agent chats in this workspace without you naming them:

| Rule | Purpose |
|------|---------|
| [`staff-learning-core.mdc`](.cursor/rules/staff-learning-core.mdc) | Production framing, trade-offs, default response structure |
| [`java-golang-examples.mdc`](.cursor/rules/java-golang-examples.mdc) | Matching Java + Go snippets for code-heavy topics |
| [`interview-preparation.mdc`](.cursor/rules/interview-preparation.mdc) | System design skeleton, Staff+ coding bar, behavioral STAR |
| [`system-design-case-studies.mdc`](.cursor/rules/system-design-case-studies.mdc) | Case study walkthrough expectations |

You do not need to `@`-mention these files; Cursor loads them for this project.

### Skill: `handbook-topic-content`

The skill at [`.cursor/skills/handbook-topic-content/SKILL.md`](.cursor/skills/handbook-topic-content/SKILL.md) tells the agent **how to create handbook artifacts**: folder layout, README structure, Mermaid diagrams, Java/Go files, and sized interview banks (10 / 25 / 50 / 100).

**When it applies:** Ask to create, draft, or expand handbook content—e.g. a chapter page, case study, diagrams, code examples, or interview questions for any topic listed in `Plan.md`.

**Supporting references** (used by the agent, optional reading for you):

- [`topic-templates.md`](.cursor/skills/handbook-topic-content/topic-templates.md) — README and interview file templates  
- [`interview-bank-rubric.md`](.cursor/skills/handbook-topic-content/interview-bank-rubric.md) — Question counts and category mix  
- [`diagram-patterns.md`](.cursor/skills/handbook-topic-content/diagram-patterns.md) — Diagram types and Mermaid patterns  
- [`examples.md`](.cursor/skills/handbook-topic-content/examples.md) — End-to-end invocation examples  

#### How to invoke the skill

In Cursor **Agent** chat, use natural language. The skill is picked up from the project when your request matches handbook content work. You can also explicitly point at it:

```text
@.cursor/skills/handbook-topic-content/SKILL.md
Create handbook content for chapter 10 Caching Strategies — full package.
```

**Example prompts**

| Goal | Example prompt |
|------|----------------|
| Full chapter | `Create handbook content for chapter 10 Caching Strategies — full package.` |
| Case study | `Case Study 7 Rate Limiter — content + 25 interview questions.` |
| Questions only | `Add 50 interview questions for Distributed Systems (chapter 15).` |
| Partial | `Outline only for chapter 16 CAP Theorem` or `Diagrams only for chapter 14 Kafka` |
| Expand | `Expand interview bank from 25 to 50 for chapters/10-caching-strategies/` |

**Full package** usually includes:

- `README.md` — main topic page (intuition, pain → technique, trade-offs, failures, architect takeaway)  
- `diagrams/*.md` — Mermaid (and sometimes ASCII) with captions  
- `java/` and `go/` — same scenario, ≤40 lines each when code applies  
- `interview-questions.md` — tagged questions per the rubric  

**Partial requests** are supported: outline only, diagrams only, cheatsheet (short README + 10 questions), or interview bank only.

#### Interview question banks

Banks are sized **10, 25, 50, or 100** per [`interview-bank-rubric.md`](.cursor/skills/handbook-topic-content/interview-bank-rubric.md). Each question is tagged (e.g. `[MEDIUM]`, `[STAFF+]`, `[SYSTEM]`, `[TRADEOFF]`). Full answer keys are omitted by default so you can drill; ask for hints or an answer key appendix if you want them.

#### Slugs and paths

Chapter folders use zero-padded numbers and kebab-case titles from `Plan.md`, e.g. `chapters/10-caching-strategies/`. Case studies use `case-studies/07-rate-limiter/`.

## Contributing content manually

You can author files yourself and follow the same conventions:

1. Find the topic in [`Plan.md`](Plan.md).  
2. Create or extend the folder under `chapters/`, `case-studies/`, `leadership/`, or `interview-prep/`.  
3. Use [`topic-templates.md`](.cursor/skills/handbook-topic-content/topic-templates.md) for section headings.  
4. Link related chapters and point readers to `interview-questions.md`.  

Using the agent with the handbook skill keeps structure, dual-language examples, and rubric-sized banks consistent.

## Quick start

1. Clone or open this repo in Cursor.  
2. Skim [`Plan.md`](Plan.md) and pick a topic.  
3. In Agent chat, request a full or partial package (see examples above).  
4. Review generated files under the appropriate `chapters/` or `case-studies/` path and iterate (`expand section X`, `add diagram for failure modes`, etc.).  

For interview practice, use `interview-questions.md` in a topic folder; rules in `interview-preparation.mdc` shape how the agent runs mocks and system design drills when you ask.
