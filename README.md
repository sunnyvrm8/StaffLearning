# Staff Learning

A **Staff / Principal engineer learning handbook**—production-oriented notes, diagrams, dual-language examples (Java + Go), system design case studies, and interview question banks.

This repository publishes its content from `docs/`, so the usable source paths are under `docs/chapters/`, `docs/case-studies/`, `docs/leadership/`, and `docs/interview-prep/`.

## Quick start

- Open `docs/index.md` to see the published home page.
- Use these entry points:
  - [Handbook chapters](docs/chapters/README.md)
  - [Case studies](docs/case-studies/README.md)
  - [Leadership topics](docs/leadership/README.md)
  - [Interview prep](docs/interview-prep/README.md)
  - [Curriculum and scope](docs/Plan.md)
- Preview locally from the repo root with:
  ```powershell
  mkdocs serve
  ```

## What this repo is for

| Area | Source folder | Typical artifacts |
|------|---------------|-------------------|
| Handbook chapters | `docs/chapters/` | `README.md`, diagrams, Java/Go examples, `interview-questions.md` |
| System design case studies | `docs/case-studies/` | `README.md`, diagrams, Java/Go examples |
| Leadership | `docs/leadership/` | `README.md`, interview bank |
| Interview prep | `docs/interview-prep/` | `README.md`, interview bank |

Content is authored for **architect-level depth**: stakeholder pain, practical trade-offs, failure modes, and measurable design decisions.

## Published handbook paths

| Path | Status |
|------|--------|
| [`docs/chapters/01-solid-and-core-engineering-principles/`](docs/chapters/01-solid-and-core-engineering-principles/README.md) | Full chapter + Top 20 Q&A |
| [`docs/chapters/02-design-patterns/`](docs/chapters/02-design-patterns/README.md) | Full chapter + 25 interview questions |
| [`docs/chapters/03-domain-driven-design-and-bounded-contexts/`](docs/chapters/03-domain-driven-design-and-bounded-contexts/README.md) | Full chapter + Top 10 Q&A |
| [`docs/chapters/04-data-structures-and-complexity/`](docs/chapters/04-data-structures-and-complexity/README.md) | Full chapter + 50 interview questions |
| [`docs/chapters/05-java-and-golang-deep-dive/`](docs/chapters/05-java-and-golang-deep-dive/README.md) | Full chapter + 50 Java + 50 Go Q&A |
| [`docs/chapters/06-concurrency-and-multithreading/`](docs/chapters/06-concurrency-and-multithreading/README.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/07-memory-management/`](docs/chapters/07-memory-management/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/08-networking-and-http/`](docs/chapters/08-networking-and-http/README.md) | Full chapter + Top 10 Q&A |
| [`docs/chapters/09-api-design/`](docs/chapters/09-api-design/README.md) | Full chapter + 50 interview questions |
| [`docs/chapters/10-database-design-and-data-modeling/`](docs/chapters/10-database-design-and-data-modeling/README.md) | Full chapter + 50 interview questions |
| [`docs/chapters/17-microservices-architecture/`](docs/chapters/17-microservices-architecture/README.md) | Full chapter + 25 interview questions |
| [`docs/chapters/18-event-driven-architecture/`](docs/chapters/18-event-driven-architecture/README.md) | Full chapter + 25 interview questions |
| [`docs/chapters/25-security-architecture/`](docs/chapters/25-security-architecture/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/26-observability/`](docs/chapters/26-observability/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/27-performance-engineering/`](docs/chapters/27-performance-engineering/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/28-scalability-and-capacity-planning/`](docs/chapters/28-scalability-and-capacity-planning/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/29-platform-engineering-and-internal-developer-platforms/`](docs/chapters/29-platform-engineering-and-internal-developer-platforms/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/30-cost-architecture-and-finops/`](docs/chapters/30-cost-architecture-and-finops/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/31-architecture-governance/`](docs/chapters/31-architecture-governance/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/32-ai-and-llm-systems-in-production/`](docs/chapters/32-ai-and-llm-systems-in-production/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/33-rag-and-retrieval-architecture/`](docs/chapters/33-rag-and-retrieval-architecture/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/chapters/34-agentic-systems-and-mlops-for-ai/`](docs/chapters/34-agentic-systems-and-mlops-for-ai/interview-questions.md) | Top 10 Q&A (full chapter pending) |
| [`docs/leadership/`](docs/leadership/README.md) | Leadership bundle + Top 20 Q&A with answers |
| [`docs/interview-prep/principal-engineer-thinking/`](docs/interview-prep/principal-engineer-thinking/README.md) | Principal thinking lens + Top 10 Q&A with answers |

## Repository layout

```text
docs/                            # Published handbook sources
  index.md                       # Site home
  Plan.md                        # Curriculum and source of truth
  chapters/                      # Handbook chapters
  case-studies/                  # System design case studies
  leadership/                    # Leadership topics
  interview-prep/                # Interview prep tracks
site/                            # Generated MkDocs site output
stylesheets/                     # MkDocs custom CSS
mkdocs.yml                       # Site configuration
.cursor/                         # Agent rules and handbook skill templates
```

## Live site (GitHub Pages)

- **URL:** https://sunnyvrm8.github.io/StaffLearning/
- **Local build:** open `site/index.html` or run `mkdocs serve` from the repo root.

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

**When it applies:** ask to create, draft, or expand handbook content—e.g. a chapter page, case study, diagrams, code examples, or interview questions for any topic listed in `docs/Plan.md`.

**Supporting references** (used by the agent, optional reading for you):

- [`topic-templates.md`](docs/.cursor/skills/handbook-topic-content/topic-templates.md) — README and interview file templates  
- [`interview-bank-rubric.md`](docs/.cursor/skills/handbook-topic-content/interview-bank-rubric.md) — Question counts and category mix  
- [`diagram-patterns.md`](docs/.cursor/skills/handbook-topic-content/diagram-patterns.md) — Diagram types and Mermaid patterns  
- [`examples.md`](docs/.cursor/skills/handbook-topic-content/examples.md) — End-to-end invocation examples  

#### How to invoke the skill

In Cursor **Agent** chat, use natural language. The skill is picked up from the project when your request matches handbook content work. You can also explicitly point at it:

```text
@.cursor/skills/handbook-topic-content/SKILL.md
Create handbook content for chapter 10 Caching Strategies — full package.
```

#### Interview question banks

Banks are sized **10, 25, 50, or 100** per [`docs/.cursor/skills/handbook-topic-content/interview-bank-rubric.md`](docs/.cursor/skills/handbook-topic-content/interview-bank-rubric.md). Each question is tagged (e.g. `[MEDIUM]`, `[STAFF+]`, `[SYSTEM]`, `[TRADEOFF]`). Full answer keys are omitted by default so you can drill; ask for hints or an answer key appendix if you want them.

#### Slugs and paths

Chapter folders use zero-padded numbers and kebab-case titles from `docs/Plan.md`, e.g. `chapters/10-caching-strategies/`. Case studies use `case-studies/07-rate-limiter/`.

## Contributing content manually

You can author files yourself and follow the same conventions:

1. Find the topic in [`docs/Plan.md`](docs/Plan.md).
2. Create or extend the folder under `docs/chapters/`, `docs/case-studies/`, `docs/leadership/`, or `docs/interview-prep/`.
3. Use [`docs/.cursor/skills/handbook-topic-content/topic-templates.md`](docs/.cursor/skills/handbook-topic-content/topic-templates.md) for section headings.
4. Link related chapters and point readers to `interview-questions.md`.

Using the agent with the handbook skill keeps structure, dual-language examples, and rubric-sized banks consistent.

## Next step

- Start from `docs/index.md`, or
- Jump to a published topic in `docs/chapters/` or `docs/case-studies/`.

